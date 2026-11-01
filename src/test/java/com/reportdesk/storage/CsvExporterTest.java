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
import com.reportdesk.core.Report; import com.reportdesk.core.ReportNote; import com.reportdesk.core.ReportStatus; import com.reportdesk.test.TestFixtures; import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals; import static org.junit.jupiter.api.Assertions.assertTrue;
class CsvExporterTest {


@Test

 void headerIsAlwaysPresent() {



 String csv = CsvExporter.toCsv(null);



 assertEquals("id,created_at,status,category,target,reporter,claimed_by,closed_by,verdict,detail,notes\n",





 csv);

 }


@Test

 void rowsContainReportFieldsAndNoteCount() {



 Report report = TestFixtures.report(3L, TestFixtures.ALICE, "Alice", "Bob", "cheating",





 ReportStatus.RESOLVED, 1_700_000_000_000L, 1_700_000_100_000L, 1_700_000_100_000L)





 .withNote(new ReportNote(TestFixtures.STAFF, "Staff", 1_700_000_050_000L, "note"), 1_700_000_050_000L);




String csv = CsvExporter.toCsv(List.of(report));



 String[] lines = csv.split("\n");




assertEquals(2, lines.length);



 assertTrue(lines[1].startsWith("3,"));



 assertTrue(lines[1].contains(",RESOLVED,cheating,Bob,Alice,"));



 assertTrue(lines[1].endsWith(",1"));

 }


@Test

 void commasQuotesAndNewlinesAreEscaped() {



 Report report = TestFixtures.open(1L, TestFixtures.ALICE, "Alice, \"The Great\"",





 "Bob", "chat", 1_700_000_000_000L);



 Report withDetail = new Report(report.id(), report.reporterId(), report.reporterName(),





 report.targetId(), report.targetName(), report.category(), "line1\nline2, \"quoted\"",





 report.status(), report.createdAt(), report.updatedAt(), report.claimedBy(),





 report.claimedByName(), report.claimedAt(), report.closedBy(), report.closedByName(),





 report.closedAt(), report.verdict(), report.closeNote(), report.notes(),





 report.evidence(), report.systemGenerated(), report.server());




String csv = CsvExporter.toCsv(List.of(withDetail));




assertTrue(csv.contains("\"Alice, \"\"The Great\"\"\""));



 assertTrue(csv.contains("line1 line2, \"\"quoted\"\""));



 assertTrue(csv.contains("\"line1 line2, \"\"quoted\"\"\""));

 }


@Test

 void semicolonsAreQuotedForSpreadsheetCompatibility() {



 Report report = TestFixtures.open(1L, TestFixtures.ALICE, "Alice", "Bob;Charlie", "chat", 1L);



 assertTrue(CsvExporter.toCsv(List.of(report)).contains("\"Bob;Charlie\""));

 } }
