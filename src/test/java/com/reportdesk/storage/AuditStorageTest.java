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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.reportdesk.core.AuditEntry;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AuditStorageTest {

  @TempDir Path temp;

  private final List<String> warnings = new ArrayList<>();

  @AfterEach
  void cleanUp() {

    // Each test shuts its own storage instance down.

  }

  @Test
  void entriesAreAppendedAndReadNewestFirst() {

    AuditStorage storage = new AuditStorage(temp, true, 5, warnings::add);

    try {

      long now = System.currentTimeMillis();

      AuditEntry first = new AuditEntry(now - 2_000L, UUID.randomUUID(), "Alice", "CLAIM", 1L, "");

      AuditEntry second =
          new AuditEntry(now - 1_000L, UUID.randomUUID(), "Bob", "CLOSE", 1L, "RESOLVED");

      storage.append(first).join();

      storage.append(second).join();

      List<String> lines = storage.readPage(1, 10).join();

      assertEquals(2, lines.size());

      assertTrue(lines.get(0).contains("CLOSE"));

      assertTrue(lines.get(1).contains("CLAIM"));

      assertEquals(second, AuditEntry.fromLine(lines.get(0)));

      assertTrue(warnings.isEmpty(), () -> "unexpected warnings: " + warnings);

    } finally {

      storage.shutdown();
    }
  }

  @Test
  void pagingClampsAndReturnsRequestedSlice() {

    AuditStorage storage = new AuditStorage(temp, true, 5, warnings::add);

    try {

      long now = System.currentTimeMillis();

      for (int index = 0; index < 5; index++) {

        storage
            .append(
                new AuditEntry(
                    now + index, UUID.randomUUID(), "Staff", "ACTION_" + index, index, ""))
            .join();
      }

      List<String> pageOne = storage.readPage(1, 2).join();

      List<String> pageThree = storage.readPage(3, 2).join();

      List<String> beyond = storage.readPage(99, 2).join();

      assertEquals(2, pageOne.size());

      assertTrue(pageOne.get(0).contains("ACTION_4"));

      assertEquals(1, pageThree.size());

      assertTrue(pageThree.get(0).contains("ACTION_0"));

      assertEquals(1, beyond.size());

    } finally {

      storage.shutdown();
    }
  }

  @Test
  void disabledStorageDoesNotWriteFiles() {

    AuditStorage storage = new AuditStorage(temp, false, 5, warnings::add);

    try {

      storage.append(new AuditEntry(1L, UUID.randomUUID(), "Staff", "ACTION", 1L, "")).join();

      assertFalse(Files.isDirectory(temp.resolve("audit")));

      assertTrue(storage.readPage(1, 10).join().isEmpty());

    } finally {

      storage.shutdown();
    }
  }

  @Test
  void purgeRemovesOnlyOldLogFiles() throws IOException {

    AuditStorage storage = new AuditStorage(temp, true, 5, warnings::add);

    try {

      long now = System.currentTimeMillis();

      storage.append(new AuditEntry(now, UUID.randomUUID(), "Staff", "ACTION", 1L, "")).join();

      Path auditDir = temp.resolve("audit");

      Path old = auditDir.resolve("audit-2020-01-01.log");

      Files.writeString(old, "old line\n");

      Files.setLastModifiedTime(old, FileTime.from(Instant.parse("2020-01-01T00:00:00Z")));

      int deleted =
          storage.purgeBefore(Instant.parse("2021-01-01T00:00:00Z").toEpochMilli()).join();

      assertEquals(1, deleted);

      assertFalse(Files.exists(old));

      assertEquals(1, storage.readPage(1, 10).join().size());

    } finally {

      storage.shutdown();
    }
  }

  @Test
  void missingDirectoryReadsAsEmpty() {

    AuditStorage storage = new AuditStorage(temp, true, 5, warnings::add);

    try {

      assertTrue(storage.readPage(1, 10).join().isEmpty());

      assertEquals(0, storage.purgeBefore(System.currentTimeMillis()).join());

    } finally {

      storage.shutdown();
    }
  }

  @Test
  void searchReturnsCaseInsensitiveSubstringMatchesNewestFirst() {

    AuditStorage storage = new AuditStorage(temp, true, 5, warnings::add);

    try {

      long now = System.currentTimeMillis();

      storage.append(new AuditEntry(now - 3_000L, UUID.randomUUID(), "Alice", "CLAIM", 1L, "first")).join();

      storage.append(new AuditEntry(now - 2_000L, UUID.randomUUID(), "Bob", "CLOSE", 1L, "resolved")).join();

      storage.append(new AuditEntry(now - 1_000L, UUID.randomUUID(), "Carol", "CLAIM", 2L, "claimed")).join();

      List<String> claims = storage.search("claim", 10).join();

      assertEquals(2, claims.size());

      // Newest match first.
      assertTrue(claims.get(0).contains("CLAIM") && claims.get(0).contains("Carol"));

      assertTrue(claims.get(1).contains("CLAIM") && claims.get(1).contains("Alice"));

      // Blank keyword returns the newest limit lines regardless of content.
      assertEquals(3, storage.search("", 10).join().size());

      assertTrue(storage.search("nonsense-that-matches-nothing", 10).join().isEmpty());

    } finally {

      storage.shutdown();
    }
  }

  @Test
  void exportCsvWritesBomAndOneRowPerEntry() throws IOException {

    AuditStorage storage = new AuditStorage(temp, true, 5, warnings::add);

    try {

      long now = System.currentTimeMillis();

      storage.append(new AuditEntry(now - 1_000L, UUID.randomUUID(), "Alice", "CLAIM", 7L, "note")).join();

      storage.append(new AuditEntry(now, UUID.randomUUID(), "Bob", "EXPORT", 0L, "")).join();

      Path out = temp.resolve("export");

      Path written = storage.exportCsv(out).join();

      assertEquals(out, written.getParent());

      String content = Files.readString(written, java.nio.charset.StandardCharsets.UTF_8);

      assertTrue(content.startsWith("\ufeff"));

      assertTrue(content.startsWith("\ufefftimestamp,actor,action,report_id,detail\n"));

      // Two entries: one with report id 7, one rendered as '-' (reportId <= 0).
      assertTrue(content.contains("CLAIM"));

      assertTrue(content.contains("EXPORT"));

      assertTrue(content.contains(",7,"));

      assertTrue(content.contains("EXPORT,-,"), "report id 0 renders as '-'");

    } finally {

      storage.shutdown();
    }
  }

  @Test
  void exportCsvFailsWhenNoAuditDirectoryExists() {

    AuditStorage storage = new AuditStorage(temp, true, 5, warnings::add);

    try {

      java.util.concurrent.CompletableFuture<Path> future =
          storage.exportCsv(temp.resolve("export"));

      try {

        future.join();

        org.junit.jupiter.api.Assertions.fail("expected export to fail with no audit data");

      } catch (java.util.concurrent.CompletionException expected) {

        // No audit files yet; exportCsv must fail rather than write an empty export.
      }

    } finally {

      storage.shutdown();
    }
  }

  @Test
  void purgeOverSizeDeletesOldestFilesFirst() throws IOException {

    AuditStorage storage = new AuditStorage(temp, true, 5, warnings::add);

    try {

      Path auditDir = temp.resolve("audit");

      Files.createDirectories(auditDir);

      // Three files, each 1 KiB; cap at 2 KiB => oldest is deleted.
      Files.writeString(auditDir.resolve("audit-2020-01-01.log"), "x".repeat(1024));

      Files.writeString(auditDir.resolve("audit-2020-01-02.log"), "x".repeat(1024));

      Files.writeString(auditDir.resolve("audit-2020-01-03.log"), "x".repeat(1024));

      int deleted = storage.purgeOverSize(2 * 1024).join();

      assertEquals(1, deleted);

      // The oldest file is the one removed.
      assertFalse(Files.exists(auditDir.resolve("audit-2020-01-01.log")));

      assertTrue(Files.exists(auditDir.resolve("audit-2020-01-02.log")));

      assertTrue(Files.exists(auditDir.resolve("audit-2020-01-03.log")));

    } finally {

      storage.shutdown();
    }
  }

  @Test
  void purgeOverSizeIsDisabledForNonPositiveLimit() {

    AuditStorage storage = new AuditStorage(temp, true, 5, warnings::add);

    try {

      assertEquals(0, storage.purgeOverSize(0).join());

      assertEquals(0, storage.purgeOverSize(-1).join());

    } finally {

      storage.shutdown();
    }
  }
}
