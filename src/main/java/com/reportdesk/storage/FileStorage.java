/*
 * ReportDesk - Enterprise-grade player reporting and evidence system.
 * Copyright (C) 2026 William
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.reportdesk.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.reportdesk.config.PluginConfig;
import com.reportdesk.core.Evidence;
import com.reportdesk.core.PendingNotice;
import com.reportdesk.core.Report;
import com.reportdesk.core.WatchEntry;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Asynchronous JSON storage. Every public operation runs on a dedicated single-thread executor; the
 * main thread only reads the in-memory repository.
 *
 * <p>Writes are atomic (temp file + move) and reports.json is backed up at a configurable interval
 * before a save.
 */
public final class FileStorage {

  private static final int SCHEMA_VERSION = 2;
  private static final int SCHEMA_VERSION_V1 = 1;
  private static final String REPORTS_FILE = "reports.json";
  private static final String REPORTS_LOG_FILE = "reports.log";
  private static final String PENDING_FILE = "pending-notices.json";
  private static final String WATCHLIST_FILE = "watchlist.json";
  private static final String CONFIG_FILE = "config.yml";

  private final Path configFile;
  private final Path root;
  private final Path dataDir;
  private final Path evidenceDir;
  private final Path backupDir;
  private final Path exportDir;
  private final Consumer<String> warning;
  private final Gson gson;

  /** Compact single-line serializer for write-ahead log entries. */
  private final Gson compactGson;

  private final ExecutorService executor;
  private final long backupIntervalMillis;
  private final int maxBackups;
  private final int shutdownTimeoutSeconds;
  private final boolean backupOnSave;
  private final AtomicLong lastBackupAt = new AtomicLong(0L);
  private final int compactAfterEntries;
  private final long compactIntervalMillis;

  /** Accessed only on the storage executor thread; never from the main thread. */
  private long lastCompactAt;

  /** Accessed only on the storage executor thread; never from the main thread. */
  private int walEntriesSinceCompact;

  public FileStorage(
      Path pluginDataFolder, PluginConfig.Storage settings, Consumer<String> warning) {
    PluginConfig.Storage effective =
        settings == null ? new PluginConfig.Storage("data", true, 5, 60, 5, 50, 360) : settings;
    this.compactAfterEntries = Math.max(1, effective.compactAfterEntries());
    this.compactIntervalMillis = Math.max(0L, effective.compactIntervalMinutes()) * 60_000L;
    this.configFile = pluginDataFolder.resolve(CONFIG_FILE);
    this.root = pluginDataFolder.resolve(effective.directory()).normalize();
    this.dataDir = root;
    this.evidenceDir = root.resolve("evidence");
    this.backupDir = root.resolve("backups");
    this.exportDir = root.resolve("export");
    this.warning = warning == null ? ignored -> {} : warning;
    this.gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
    this.compactGson = new GsonBuilder().serializeNulls().create();
    this.backupOnSave = effective.backupOnSave();
    this.maxBackups = effective.maxBackups();
    this.backupIntervalMillis = Math.max(0L, effective.backupIntervalMinutes()) * 60_000L;
    this.shutdownTimeoutSeconds = effective.shutdownTimeoutSeconds();
    ThreadFactory factory =
        runnable -> {
          Thread thread = new Thread(runnable, "ReportDesk-Storage");
          thread.setDaemon(true);
          return thread;
        };
    this.executor = Executors.newSingleThreadExecutor(factory);
  }

  public Path root() {
    return root;
  }

  public Path evidenceDirectory() {
    return evidenceDir;
  }

  public Path exportDirectory() {
    return exportDir;
  }

  /**
   * Loads the reports snapshot plus any pending write-ahead log entries that have not been
   * compacted yet, so a restart after a crash (or between two compactions) never loses
   * already-confirmed saves.
   */
  public CompletableFuture<List<Report>> loadReports() {
    return supply(
        () -> {
          Map<Long, Report> merged = replayInto(readReportSnapshot(), readWalEntries());
          List<Report> reports = new ArrayList<>(merged.values());
          reports.sort(Comparator.comparingLong(Report::id));
          return List.copyOf(reports);
        });
  }

  /**
   * Full replacement of the reports snapshot. Merges the supplied reports with any pending
   * write-ahead log entries so in-flight saves are not lost, then folds everything into
   * reports.json and clears the log. Prefer {@link #saveReport(Report)} / {@link
   * #deleteReport(long)} for single reports: those only append one line.
   */
  public CompletableFuture<Void> saveReports(Collection<Report> reports) {
    Map<Long, Report> base = new LinkedHashMap<>();
    if (reports != null) {
      for (Report report : reports) {
        if (report != null && report.id() > 0) {
          base.put(report.id(), report);
        }
      }
    }
    return run(
        () -> {
          Map<Long, Report> merged = replayInto(base, readWalEntries());
          writeReportSnapshot(merged);
          clearWal();
        });
  }

  /**
   * Loads notifications queued while reporters were offline. Runs on the storage thread; invalid
   * entries are skipped instead of failing the load.
   */
  public CompletableFuture<List<PendingNotice>> loadPendingNotices() {
    return supply(
        () ->
            readList(
                dataDir.resolve(PENDING_FILE),
                "notices",
                PendingNotice.class,
                PendingNotice::valid,
                "pending notices"));
  }

  public CompletableFuture<Void> savePendingNotices(Collection<PendingNotice> notices) {
    List<PendingNotice> snapshot = notices == null ? List.of() : List.copyOf(notices);
    return run(() -> writeList(dataDir.resolve(PENDING_FILE), "notices", snapshot));
  }

  /**
   * Loads the per-staff watchlist. Target names are matched case-insensitively by the watch
   * service, so the file keeps the canonical display name.
   */
  public CompletableFuture<List<WatchEntry>> loadWatchlist() {
    return supply(
        () ->
            readList(
                dataDir.resolve(WATCHLIST_FILE),
                "watches",
                WatchEntry.class,
                WatchEntry::valid,
                "watchlist entries"));
  }

  public CompletableFuture<Void> saveWatchlist(Collection<WatchEntry> entries) {
    List<WatchEntry> snapshot = entries == null ? List.of() : List.copyOf(entries);
    return run(() -> writeList(dataDir.resolve(WATCHLIST_FILE), "watches", snapshot));
  }

  /**
   * Cheaply persists one report modification: the report is appended to the write-ahead log
   * (reports.log, one compact JSON object per line) and no full-file rewrite happens. A periodic /
   * threshold-based compaction (triggered automatically after {@code compact-after-entries} appends
   * or {@code compact-interval-minutes} of activity, and always on shutdown) folds the log into
   * reports.json.
   */
  public CompletableFuture<Void> saveReport(Report report) {
    if (report == null || report.id() <= 0) {
      return CompletableFuture.completedFuture(null);
    }
    return appendWal(compactGson.toJson(report), report.id());
  }

  /**
   * Removes a report from persistence state. Like {@link #saveReport(Report)} this only appends to
   * the write-ahead log; compaction drops the entry from the snapshot.
   */
  public CompletableFuture<Void> deleteReport(long reportId) {
    if (reportId <= 0) {
      return CompletableFuture.completedFuture(null);
    }
    JsonObject marker = new JsonObject();
    marker.addProperty("op", "delete");
    marker.addProperty("id", reportId);
    return appendWal(marker.toString(), reportId);
  }

  /**
   * Folds the write-ahead log into reports.json: reads the current snapshot, replays pending log
   * entries, atomically writes the snapshot, and truncates the log. Backs up the snapshot first
   * when backup-on-save is enabled. No-op when the log is empty.
   */
  public CompletableFuture<Void> compactReports() {
    return run(() -> compactInPlace());
  }

  /**
   * Folds the write-ahead log into reports.json in place. Must only be called from inside an
   * executor task (which owns the WAL bookkeeping); no-op when the log is empty.
   */
  private void compactInPlace() throws IOException {
    List<String> pending = readWalEntries();
    if (pending.isEmpty()) {
      return;
    }
    Map<Long, Report> merged = replayInto(readReportSnapshot(), pending);
    writeReportSnapshot(merged);
    clearWal();
  }

  /**
   * Runs one compaction round when either the entry-count threshold or the time-based interval has
   * been exceeded since the last compaction. No-op otherwise; called at the end of every WAL append
   * so the log stays small.
   */
  private void maybeCompactAfterAppend() throws IOException {
    long now = System.currentTimeMillis();
    boolean entryThreshold = walEntriesSinceCompact >= compactAfterEntries;
    boolean timeThreshold =
        compactIntervalMillis > 0L
            && lastCompactAt > 0L
            && now - lastCompactAt >= compactIntervalMillis;
    if (!entryThreshold && !timeThreshold) {
      return;
    }
    compactInPlace();
  }

  /** Appends one line to the write-ahead log. Runs on the executor. */
  private CompletableFuture<Void> appendWal(String line, long reportId) {
    return run(
        () -> {
          Path logFile = dataDir.resolve(REPORTS_LOG_FILE);
          Files.createDirectories(logFile.getParent());
          if (lastCompactAt == 0L) {
            // Seed the time baseline on the first append since start (or
            // since the last compaction cleared the log), so the interval
            // threshold can fire even when a pre-existing log was left
            // behind by a crash.
            lastCompactAt = System.currentTimeMillis();
          }
          walEntriesSinceCompact++;
          appendLine(logFile, line);
          maybeCompactAfterAppend();
        });
  }

  /**
   * Deletes the write-ahead log; called right after a successful compaction so the snapshot on disk
   * is the complete state.
   */
  private void clearWal() throws IOException {
    Files.deleteIfExists(dataDir.resolve(REPORTS_LOG_FILE));
    walEntriesSinceCompact = 0;
    lastCompactAt = System.currentTimeMillis();
  }

  /** Writes the snapshot file atomically after optionally backing up the current one. */
  private void writeReportSnapshot(Map<Long, Report> snapshot) throws IOException {
    Files.createDirectories(dataDir);
    Path target = dataDir.resolve(REPORTS_FILE);
    if (backupOnSave) {
      backupIfNeeded(target);
    }
    JsonObject rootObject = new JsonObject();
    rootObject.addProperty("schema", SCHEMA_VERSION);
    rootObject.addProperty("savedAt", Instant.now().toString());
    JsonArray array = new JsonArray();
    List<Long> ids = new ArrayList<>(snapshot.keySet());
    ids.sort(Long::compare);
    for (Long id : ids) {
      array.add(gson.toJsonTree(snapshot.get(id)));
    }
    rootObject.add("reports", array);
    writeAtomically(target, writer -> gson.toJson(rootObject, writer));
  }

  /**
   * Merges log entries into the report snapshot: {@code op} markers delete entries, plain JSON
   * objects are upserts by id.
   */
  private Map<Long, Report> replayInto(Map<Long, Report> snapshot, List<String> walLines) {
    Map<Long, Report> merged = new LinkedHashMap<>(snapshot);
    for (String line : walLines) {
      if (line.isBlank()) {
        continue;
      }
      try {
        JsonElement element = JsonParser.parseString(line);
        if (!element.isJsonObject()) {
          continue;
        }
        JsonObject object = element.getAsJsonObject();
        // A WAL line is either a delete marker ({"op":"delete","id":N})
        // or a plain serialized Report object. Delete markers carry no
        // "report" field; reports always do (and never have "op").
        if (object.has("op")) {
          merged.remove(object.get("id").getAsLong());
        } else {
          Report report = gson.fromJson(object, Report.class);
          if (report != null && report.id() > 0 && !report.targetName().isBlank()) {
            merged.put(report.id(), report);
          } else {
            warn("Skipped an invalid write-ahead log entry");
          }
        }
      } catch (RuntimeException exception) {
        warn("Skipped a corrupt write-ahead log line: " + exception.getMessage());
      }
    }
    return merged;
  }

  /** Reads the reports.json snapshot (v1 or v2 format) into a plain map. */
  private Map<Long, Report> readReportSnapshot() {
    Path file = dataDir.resolve(REPORTS_FILE);
    if (!Files.isRegularFile(file)) {
      return Map.of();
    }
    Map<Long, Report> snapshot = new LinkedHashMap<>();
    try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
      JsonObject rootObject = JsonParser.parseReader(reader).getAsJsonObject();
      int version =
          rootObject.has("schema") ? rootObject.get("schema").getAsInt() : SCHEMA_VERSION_V1;
      JsonArray array =
          rootObject.has("reports") && rootObject.get("reports").isJsonArray()
              ? rootObject.getAsJsonArray("reports")
              : new JsonArray();
      for (JsonElement element : array) {
        try {
          Report report = gson.fromJson(element, Report.class);
          if (report != null && report.id() > 0 && !report.targetName().isBlank()) {
            snapshot.put(report.id(), report);
          } else {
            warn("Skipped an invalid report entry in reports.json");
          }
        } catch (RuntimeException exception) {
          warn("Skipped a corrupt report entry: " + exception.getMessage());
        }
      }
      if (version != SCHEMA_VERSION && version != SCHEMA_VERSION_V1) {
        warn("reports.json has unknown schema " + version + "; treating it as a v1 snapshot");
      }
    } catch (IOException | RuntimeException exception) {
      warn("Failed to read reports.json: " + exception.getMessage());
    }
    return snapshot;
  }

  /**
   * Reads pending write-ahead log lines; a truncated last line is dropped so an interrupted write
   * cannot corrupt the replay.
   */
  private List<String> readWalEntries() {
    Path file = dataDir.resolve(REPORTS_LOG_FILE);
    if (!Files.isRegularFile(file)) {
      return List.of();
    }
    List<String> lines = new ArrayList<>();
    try {
      lines.addAll(Files.readAllLines(file, StandardCharsets.UTF_8));
    } catch (IOException exception) {
      warn("Failed to read " + REPORTS_LOG_FILE + ": " + exception.getMessage());
      return List.of();
    }
    while (!lines.isEmpty() && lines.get(lines.size() - 1).isBlank()) {
      lines.remove(lines.size() - 1);
    }
    return lines;
  }

  /**
   * Appends a single line plus a line separator to the WAL file. One write call on a regular file
   * is atomic enough for our purposes; a torn final line is dropped by {@link #readWalEntries()}.
   */
  private void appendLine(Path target, String line) throws IOException {
    try (Writer output =
        Files.newBufferedWriter(
            target,
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND,
            StandardOpenOption.WRITE)) {
      output.write(line);
      output.write(System.lineSeparator());
    }
  }

  /**
   * Reads config.yml for the in-game editor. Runs on the storage thread; the returned text is the
   * source of truth for comment-preserving edits.
   */
  public CompletableFuture<String> readConfigText() {
    return supply(
        () -> {
          try {
            return Files.isRegularFile(configFile)
                ? Files.readString(configFile, StandardCharsets.UTF_8)
                : "";
          } catch (IOException exception) {
            throw new java.io.UncheckedIOException(exception);
          }
        });
  }

  /**
   * Writes config.yml atomically after optionally creating a timestamped backup. Never called from
   * the main thread.
   */
  public CompletableFuture<Void> writeConfigText(String text, boolean backup, int keepBackups) {
    String content = text == null ? "" : text;
    return run(
        () -> {
          if (backup && keepBackups > 0 && Files.isRegularFile(configFile)) {
            backupConfig(keepBackups);
          }
          writeAtomically(configFile, writer -> writer.write(content));
        });
  }

  public CompletableFuture<Void> saveEvidence(Evidence evidence) {
    return run(
        () -> {
          if (evidence == null || evidence.reportId() <= 0) {
            return;
          }
          Files.createDirectories(evidenceDir);
          JsonObject rootObject = new JsonObject();
          rootObject.addProperty("schema", SCHEMA_VERSION);
          rootObject.add("evidence", gson.toJsonTree(evidence));
          writeAtomically(
              evidenceFile(evidence.reportId()), writer -> gson.toJson(rootObject, writer));
        });
  }

  public CompletableFuture<Optional<Evidence>> loadEvidence(long reportId) {
    return supply(
        () -> {
          Path file = evidenceFile(reportId);
          if (!Files.isRegularFile(file)) {
            return Optional.empty();
          }
          try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject rootObject = JsonParser.parseReader(reader).getAsJsonObject();
            JsonElement element = rootObject.get("evidence");
            if (element == null || element.isJsonNull()) {
              return Optional.empty();
            }
            return Optional.ofNullable(gson.fromJson(element, Evidence.class));
          } catch (IOException | RuntimeException exception) {
            warn("Failed to read evidence #" + reportId + ": " + exception.getMessage());
            return Optional.empty();
          }
        });
  }

  public CompletableFuture<Boolean> deleteEvidence(long reportId) {
    return supply(
        () -> {
          try {
            return Files.deleteIfExists(evidenceFile(reportId));
          } catch (IOException exception) {
            warn("Failed to delete evidence #" + reportId + ": " + exception.getMessage());
            return false;
          }
        });
  }

  public CompletableFuture<Integer> purgeEvidenceBefore(long cutoffMillis) {
    return supply(
        () -> {
          if (!Files.isDirectory(evidenceDir)) {
            return 0;
          }
          int deleted = 0;
          try (var stream = Files.list(evidenceDir)) {
            List<Path> files = stream.filter(Files::isRegularFile).toList();
            for (Path file : files) {
              try {
                if (Files.getLastModifiedTime(file).toMillis() < cutoffMillis) {
                  if (Files.deleteIfExists(file)) {
                    deleted++;
                  }
                }
              } catch (IOException exception) {
                warn(
                    "Failed to purge evidence "
                        + file.getFileName()
                        + ": "
                        + exception.getMessage());
              }
            }
          } catch (IOException exception) {
            warn("Failed to scan evidence directory: " + exception.getMessage());
          }
          return deleted;
        });
  }

  public CompletableFuture<Path> exportCsv(Collection<Report> reports) {
    List<Report> snapshot = reports == null ? List.of() : List.copyOf(reports);
    return supply(
        () -> {
          try {
            Files.createDirectories(exportDir);
            String timestamp =
                DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.now());
            Path target = exportDir.resolve("reports-" + timestamp + ".csv");
            writeAtomically(
                target,
                writer -> {
                  writer.write('\ufeff');
                  writer.write(com.reportdesk.storage.CsvExporter.toCsv(snapshot));
                });
            return target;
          } catch (IOException exception) {
            throw new CompletionException(exception);
          }
        });
  }

  public CompletableFuture<Integer> purgeBackups() {
    return supply(
        () -> {
          if (!Files.isDirectory(backupDir)) {
            return 0;
          }
          int deleted = 0;
          try (var stream = Files.list(backupDir)) {
            for (Path file :
                stream.filter(path -> path.getFileName().toString().endsWith(".json")).toList()) {
              if (Files.deleteIfExists(file)) {
                deleted++;
              }
            }
          } catch (IOException exception) {
            warn("Failed to purge backups: " + exception.getMessage());
          }
          return deleted;
        });
  }

  /**
   * Stops the executor. Tasks queued before this call (including a final compaction scheduled by
   * the caller) drain first; any write-ahead log entries left uncompacted are still replayed by
   * {@link #loadReports()} on the next startup.
   */
  public void shutdown() {
    try {
      // Queue a final compaction before draining; if the timeout expires
      // the log survives on disk and the next startup replays it.
      compactReports();
    } catch (java.util.concurrent.RejectedExecutionException ignored) {
      // The executor already stopped; the WAL remains replayable.
    }
    executor.shutdown();
    try {
      if (!executor.awaitTermination(Math.max(1, shutdownTimeoutSeconds), TimeUnit.SECONDS)) {
        warn(
            "Storage did not finish within "
                + shutdownTimeoutSeconds
                + "s; pending writes may be lost");
        executor.shutdownNow();
      }
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      executor.shutdownNow();
    }
  }

  /** Test/shutdown helper: waits for queued work to finish. */
  public boolean awaitIdle(long timeout, TimeUnit unit) {
    try {
      return executor.awaitTermination(timeout, unit);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  private Path evidenceFile(long reportId) {
    return evidenceDir.resolve("evidence-" + reportId + ".json");
  }

  private <T> List<T> readList(
      Path file, String field, Class<T> type, Predicate<T> valid, String label) {
    if (!Files.isRegularFile(file)) {
      return List.of();
    }
    try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
      JsonObject rootObject = JsonParser.parseReader(reader).getAsJsonObject();
      JsonArray array =
          rootObject.has(field) && rootObject.get(field).isJsonArray()
              ? rootObject.getAsJsonArray(field)
              : new JsonArray();
      List<T> values = new ArrayList<>();
      for (JsonElement element : array) {
        try {
          T value = gson.fromJson(element, type);
          if (value != null && valid.test(value)) {
            values.add(value);
          } else {
            warn("Skipped an invalid entry in " + label);
          }
        } catch (RuntimeException exception) {
          warn("Skipped a corrupt entry in " + label + ": " + exception.getMessage());
        }
      }
      return List.copyOf(values);
    } catch (IOException | RuntimeException exception) {
      warn("Failed to read " + label + ": " + exception.getMessage());
      return List.of();
    }
  }

  private <T> void writeList(Path target, String field, Collection<T> values) throws IOException {
    JsonObject rootObject = new JsonObject();
    rootObject.addProperty("schema", SCHEMA_VERSION);
    rootObject.addProperty("savedAt", Instant.now().toString());
    JsonArray array = new JsonArray();
    for (T value : values) {
      array.add(gson.toJsonTree(value));
    }
    rootObject.add(field, array);
    writeAtomically(target, writer -> gson.toJson(rootObject, writer));
  }

  private void backupIfNeeded(Path reportsFile) {
    if (!Files.isRegularFile(reportsFile) || maxBackups <= 0) {
      return;
    }
    long now = System.currentTimeMillis();
    long last = lastBackupAt.get();
    if (last != 0L && now - last < backupIntervalMillis) {
      return;
    }
    try {
      Files.createDirectories(backupDir);
      String timestamp =
          DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
              .withZone(ZoneId.systemDefault())
              .format(Instant.ofEpochMilli(now));
      Path backup = backupDir.resolve("reports-" + timestamp + ".json");
      Files.copy(reportsFile, backup, StandardCopyOption.REPLACE_EXISTING);
      lastBackupAt.set(now);
      rotateBackups();
    } catch (IOException exception) {
      warn("Failed to create reports backup: " + exception.getMessage());
    }
  }

  private void rotateBackups() {
    try (var stream = Files.list(backupDir)) {
      List<Path> backups =
          stream
              .filter(path -> path.getFileName().toString().startsWith("reports-"))
              .filter(path -> path.getFileName().toString().endsWith(".json"))
              .sorted(
                  Comparator.comparing(
                      path -> path.getFileName().toString(), Comparator.reverseOrder()))
              .toList();
      for (int index = maxBackups; index < backups.size(); index++) {
        Files.deleteIfExists(backups.get(index));
      }
    } catch (IOException exception) {
      warn("Failed to rotate backups: " + exception.getMessage());
    }
  }

  private void backupConfig(int keep) throws IOException {
    Files.createDirectories(backupDir);
    String timestamp =
        DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")
            .withZone(ZoneId.systemDefault())
            .format(Instant.now());
    Path backup = backupDir.resolve("config-" + timestamp + ".yml");
    Files.copy(configFile, backup, StandardCopyOption.REPLACE_EXISTING);
    try (var stream = Files.list(backupDir)) {
      List<Path> backups =
          stream
              .filter(path -> path.getFileName().toString().startsWith("config-"))
              .filter(path -> path.getFileName().toString().endsWith(".yml"))
              .sorted(
                  Comparator.comparing(
                      path -> path.getFileName().toString(), Comparator.reverseOrder()))
              .toList();
      for (int index = keep; index < backups.size(); index++) {
        Files.deleteIfExists(backups.get(index));
      }
    }
  }

  private void writeAtomically(Path target, ContentWriter writer) throws IOException {
    Files.createDirectories(target.getParent());
    Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
    try (Writer output =
        Files.newBufferedWriter(
            temporary,
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE)) {
      writer.write(output);
    }
    try {
      Files.move(
          temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    } catch (AtomicMoveNotSupportedException exception) {
      Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
    } finally {
      Files.deleteIfExists(temporary);
    }
  }

  private CompletableFuture<Void> run(IoRunnable task) {
    return CompletableFuture.runAsync(
        () -> {
          try {
            task.run();
          } catch (Throwable throwable) {
            warn("Storage write failed: " + throwable.getMessage());
            throw new CompletionException(throwable);
          }
        },
        executor);
  }

  private <T> CompletableFuture<T> supply(Supplier<T> task) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return task.get();
          } catch (Throwable throwable) {
            warn("Storage read failed: " + throwable.getMessage());
            throw new CompletionException(throwable);
          }
        },
        executor);
  }

  private void warn(String message) {
    warning.accept("[storage] " + message);
  }

  @FunctionalInterface
  private interface IoRunnable {
    void run() throws IOException;
  }

  @FunctionalInterface
  private interface ContentWriter {
    void write(Writer writer) throws IOException;
  }
}
