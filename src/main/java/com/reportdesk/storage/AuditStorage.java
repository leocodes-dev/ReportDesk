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
