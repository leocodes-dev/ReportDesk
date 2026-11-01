/*
 * ReportDesk - Enterprise-grade player reporting and evidence system.
 * Copyright (C) 2026 William
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.reportdesk.core;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditEntryTest {

    @Test
    void fromLineRoundTrips() {
        AuditEntry entry = new AuditEntry(
            1_735_000_000_000L,
            UUID.fromString("00000000-0000-0000-0000-000000000001"),
            "Alice",
            "report.claim",
            42L,
            "staff claimed the report");
        AuditEntry parsed = AuditEntry.fromLine(entry.toLine());
        assertEquals(entry, parsed);
        assertEquals("report.claim", parsed.action());
        assertEquals(42L, parsed.reportId());
    }

    @Test
    void missingReportIdIsZero() {
        AuditEntry entry = new AuditEntry(1L, null, null, "boot", -1L, null);
        AuditEntry parsed = AuditEntry.fromLine(entry.toLine());
        assertEquals(0L, parsed.reportId());
        assertEquals(entry.actorId(), parsed.actorId());
        assertEquals("", parsed.actorName());
        assertEquals("boot", parsed.action());
        assertEquals("", parsed.detail());
    }

    @Test
    void controlCharactersAreSanitized() {
        AuditEntry entry = new AuditEntry(1L, UUID.randomUUID(), "St\taff",
            "NOTE\n", 1L, "line1\r\nline2\tend");

        assertEquals("St aff", entry.actorName());
        assertEquals("NOTE ", entry.action());
        assertEquals("line1  line2 end", entry.detail());
        assertEquals(6, entry.toLine().split("\t", -1).length);
    }

    @Test
    void invalidLinesReturnNull() {
        assertNull(AuditEntry.fromLine(null));
        assertNull(AuditEntry.fromLine("   "));
        assertNull(AuditEntry.fromLine("not-a-timestamp\trest"));
        assertNull(AuditEntry.fromLine("1970-01-01T00:00:00Z\tbadauthor\taction"));
    }

    @Test
    void lineEndsWithTabSeparatorOnlyWhenDetailEmpty() {
        AuditEntry entry = new AuditEntry(2L, UUID.randomUUID(), "Bob", "close", 7L, "");
        String line = entry.toLine();
        assertTrue(line.endsWith("\t"));
        assertEquals(6, line.split("\t", -1).length);
        assertEquals("", AuditEntry.fromLine(line).detail());
    }
}
