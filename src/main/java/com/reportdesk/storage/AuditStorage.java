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

import com.reportdesk.core.AuditEntry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Asynchronous append-only audit log. A dedicated executor keeps audit writes off the main thread
 * and ensures lines are appended in order.
 */
public final class AuditStorage {

  private static final int MAX_READ_LINES = 10_000;

  private final Path directory;

  private final Consumer<String> warning;

  private final ExecutorService executor;

  private final int shutdownTimeoutSeconds;

  private final boolean enabled;

  public AuditStorage(
      Path pluginDataFolder,
      boolean enabled,
      int shutdownTimeoutSeconds,
      Consumer<String> warning) {

    this.directory = pluginDataFolder.resolve("audit");

    this.enabled = enabled;

    this.shutdownTimeoutSeconds = Math.max(1, shutdownTimeoutSeconds);

    this.warning = warning == null ? ignored -> {} : warning;

    ThreadFactory factory =
        runnable -> {
          Thread thread = new Thread(runnable, "ReportDesk-Audit");

          thread.setDaemon(true);

          return thread;
        };

    this.executor = Executors.newSingleThreadExecutor(factory);
  }

  public boolean enabled() {

    return enabled;
  }

  public CompletableFuture<Void> append(AuditEntry entry) {

    if (!enabled || entry == null) {

      return CompletableFuture.completedFuture(null);
    }

    return CompletableFuture.runAsync(
        () -> {
          try {

            Files.createDirectories(directory);

            Path file = fileFor(entry.timestamp());

            Files.writeString(
                file,
                entry.toLine() + System.lineSeparator(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND,
                StandardOpenOption.WRITE);

          } catch (IOException exception) {

            warn("Failed to append audit entry: " + exception.getMessage());

            throw new CompletionException(exception);
          }
        },
        executor);
  }

  public record AuditPage(List<String> lines, int page, int totalPages) {}

  /** Reads newest entries first and returns the requested page. */
  public CompletableFuture<List<String>> readPage(int page, int pageSize) {

    return readPageInfo(page, pageSize).thenApply(AuditPage::lines);
  }

  /**
   * Same as {@link #readPage(int, int)} but also reports the page count so the
   *
   * <p>GUI can render correct previous/next buttons.
   */
  public CompletableFuture<AuditPage> readPageInfo(int page, int pageSize) {

    return CompletableFuture.supplyAsync(
        () -> {
          if (!Files.isDirectory(directory)) {

            return new AuditPage(List.of(), 1, 1);
          }

          List<Path> files;

          try (var stream = Files.list(directory)) {

            files =
                stream
                    .filter(path -> path.getFileName().toString().endsWith(".log"))
                    .sorted(
                        Comparator.comparing(
                            path -> path.getFileName().toString(), Comparator.reverseOrder()))
                    .toList();

          } catch (IOException exception) {

            warn("Failed to list audit logs: " + exception.getMessage());

            return new AuditPage(List.of(), 1, 1);
          }

          List<String> lines = new ArrayList<>();

          for (Path file : files) {

            try {

              lines.addAll(Files.readAllLines(file, StandardCharsets.UTF_8));

            } catch (IOException exception) {

              warn(
                  "Failed to read audit log " + file.getFileName() + ": " + exception.getMessage());
            }

            if (lines.size() >= MAX_READ_LINES) {

              break;
            }
          }

          java.util.Collections.reverse(lines);

          int size = Math.max(1, pageSize);

          int totalPages = Math.max(1, (lines.size() + size - 1) / size);

          int safePage = Math.max(1, Math.min(page, totalPages));

          int from = Math.min(lines.size(), (safePage - 1) * size);

          int to = Math.min(lines.size(), from + size);

          return new AuditPage(List.copyOf(lines.subList(from, to)), safePage, totalPages);
        },
        executor);
  }

  public CompletableFuture<Integer> purgeBefore(long cutoffMillis) {

    return CompletableFuture.supplyAsync(
        () -> {
          if (!Files.isDirectory(directory)) {

            return 0;
          }

          int deleted = 0;

          try (var stream = Files.list(directory)) {

            for (Path file :
                stream.filter(path -> path.getFileName().toString().endsWith(".log")).toList()) {

              try {

                if (Files.getLastModifiedTime(file).toMillis() < cutoffMillis
                    && Files.deleteIfExists(file)) {

                  deleted++;
                }

              } catch (IOException exception) {

                warn(
                    "Failed to purge audit file "
                        + file.getFileName()
                        + ": "
                        + exception.getMessage());
              }
            }

          } catch (IOException exception) {

            warn("Failed to scan audit directory: " + exception.getMessage());
          }

          return deleted;
        },
        executor);
  }

  /**
   * Case-insensitive substring search over audit lines, newest first. Bounded by {@code limit}
   * result lines; the scan stops as soon as enough matches are collected. Runs off the main
   * thread.
   *
   * @param keyword the search text; an empty/blank keyword returns the newest {@code limit} lines
   * @param limit maximum number of matching lines to return
   */
  public CompletableFuture<List<String>> search(String keyword, int limit) {

    String needle = keyword == null ? "" : keyword.trim().toLowerCase(java.util.Locale.ROOT);

    int cap = Math.max(1, limit);

    return CompletableFuture.supplyAsync(
        () -> {
          if (!Files.isDirectory(directory)) {

            return List.<String>of();
          }

          List<Path> files;

          try (var stream = Files.list(directory)) {

            files =
                stream
                    .filter(path -> path.getFileName().toString().endsWith(".log"))
                    .sorted(
                        Comparator.comparing(
                            path -> path.getFileName().toString(), Comparator.reverseOrder()))
                    .toList();

          } catch (IOException exception) {

            warn("Failed to list audit logs for search: " + exception.getMessage());

            return List.of();
          }

          List<String> matches = new ArrayList<>();

          for (Path file : files) {

            List<String> lines;

            try {

              lines = Files.readAllLines(file, StandardCharsets.UTF_8);

            } catch (IOException exception) {

              warn("Failed to read audit log " + file.getFileName() + ": " + exception.getMessage());

              continue;
            }

            // Newest entries live at the bottom of the per-day file, so scan backwards.
            for (int index = lines.size() - 1; index >= 0; index--) {

              String line = lines.get(index);

              if (!line.isBlank()
                  && (needle.isEmpty() || line.toLowerCase(java.util.Locale.ROOT).contains(needle))) {

                matches.add(line);

                if (matches.size() >= cap) {

                  return matches;
                }
              }
            }
          }

          return matches;
        },
        executor);
  }

  /**
   * Writes every audit entry (all days, newest first) to a UTF-8 CSV file with a BOM, in the same
   * style as the report export. Runs off the main thread; the caller supplies the destination
   * folder.
   *
   * @param outputDirectory folder in which {@code audit-export-*.csv} is written
   * @return the written file, or a failed future when the directory cannot be created
   */
  public CompletableFuture<Path> exportCsv(Path outputDirectory) {

    return CompletableFuture.supplyAsync(
        () -> {
          if (!Files.isDirectory(directory)) {

            throw new CompletionException(new IOException("no audit data to export"));
          }

          List<String> all = new ArrayList<>();

          try (var stream = Files.list(directory)) {

            List<Path> files =
                stream
                    .filter(path -> path.getFileName().toString().endsWith(".log"))
                    .sorted(
                        Comparator.comparing(
                            path -> path.getFileName().toString(), Comparator.reverseOrder()))
                    .toList();

            for (Path file : files) {

              try {

                all.addAll(Files.readAllLines(file, StandardCharsets.UTF_8));

              } catch (IOException exception) {

                warn(
                    "Failed to read audit log "
                        + file.getFileName()
                        + ": "
                        + exception.getMessage());
              }
            }

          } catch (IOException exception) {

            warn("Failed to scan audit directory: " + exception.getMessage());
          }

          java.util.Collections.reverse(all);

          try {

            Files.createDirectories(outputDirectory);

            String stamp = java.time.Instant.now().toString().replace(":", "").replace("-", "");

            Path target =
                outputDirectory.resolve("audit-export-" + stamp + ".csv");

            StringBuilder builder = new StringBuilder();

            builder.append('\ufeff').append("timestamp,actor,action,report_id,detail\n");

            for (String line : all) {

              com.reportdesk.core.AuditEntry entry =
                  com.reportdesk.core.AuditEntry.fromLine(line);

              if (entry == null) {

                continue;
              }

              builder
                  .append(csv(entry.timestamp()))
                  .append(',')
                  .append(csv(entry.actorName()))
                  .append(',')
                  .append(csv(entry.action()))
                  .append(',')
                  .append(entry.reportId() <= 0 ? "-" : entry.reportId())
                  .append(',')
                  .append(csv(entry.detail()))
                  .append('\n');
            }

            Files.writeString(
                target,
                builder.toString(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE);

            return target;

          } catch (IOException exception) {

            throw new CompletionException(exception);
          }
        },
        executor);
  }

  /**
   * Deletes the oldest audit files until the total on-disk size falls at or under {@code
   * maxBytes}, complementing the time-based {@link #purgeBefore(long)}. A non-positive limit
   * disables the size cap and deletes nothing.
   *
   * @return the number of files deleted
   */
  public CompletableFuture<Integer> purgeOverSize(long maxBytes) {

    if (maxBytes <= 0L) {

      return CompletableFuture.completedFuture(0);
    }

    return CompletableFuture.supplyAsync(
        () -> {
          if (!Files.isDirectory(directory)) {

            return 0;
          }

          List<java.util.AbstractMap.SimpleEntry<Path, Long>> files = new ArrayList<>();

          long total = 0L;

          try (var stream = Files.list(directory)) {

            for (Path file :
                stream.filter(path -> path.getFileName().toString().endsWith(".log")).toList()) {

              long size;

              try {

                size = Files.size(file);

              } catch (IOException exception) {

                warn("Failed to size audit file " + file.getFileName() + ": " + exception);

                continue;
              }

              files.add(new java.util.AbstractMap.SimpleEntry<>(file, size));

              total += size;
            }

          } catch (IOException exception) {

            warn("Failed to scan audit directory: " + exception.getMessage());

            return 0;
          }

          if (total <= maxBytes) {

            return 0;
          }

          // Delete from the oldest file first (ascending by name == ascending by date).
          files.sort(
              java.util.Comparator.comparing(
                  java.util.AbstractMap.SimpleEntry<Path, Long>::getKey,
                  Comparator.comparing(path -> path.getFileName().toString())));

          int deleted = 0;

          for (java.util.AbstractMap.SimpleEntry<Path, Long> entry : files) {

            if (total <= maxBytes) {

              break;
            }

            try {

              if (Files.deleteIfExists(entry.getKey())) {

                total -= entry.getValue();

                deleted++;
              }

            } catch (IOException exception) {

              warn(
                  "Failed to delete oversized audit file "
                      + entry.getKey().getFileName()
                      + ": "
                      + exception.getMessage());
            }
          }

          return deleted;
        },
        executor);
  }

  private static String csv(Object value) {

    String text = value == null ? "" : String.valueOf(value);

    text = text.replace('\r', ' ').replace('\n', ' ').replace('\t', ' ');

    boolean quote = text.indexOf(',') >= 0 || text.indexOf('"') >= 0;

    if (quote) {

      return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    return text;
  }

  public void shutdown() {

    executor.shutdown();

    try {

      if (!executor.awaitTermination(shutdownTimeoutSeconds, TimeUnit.SECONDS)) {

        executor.shutdownNow();
      }

    } catch (InterruptedException exception) {

      Thread.currentThread().interrupt();

      executor.shutdownNow();
    }
  }

  private Path fileFor(long timestamp) {

    LocalDate date = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate();

    return directory.resolve("audit-" + date + ".log");
  }

  private void warn(String message) {

    warning.accept("[audit] " + message);
  }
}
