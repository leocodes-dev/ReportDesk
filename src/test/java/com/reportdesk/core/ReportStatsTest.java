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

package com.reportdesk.core;
import com.reportdesk.test.TestFixtures; import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals; import static org.junit.jupiter.api.Assertions.assertTrue;
class ReportStatsTest {


@Test

 void countsStatusesCategoriesTargetsAndAverageHandlingTime() {



 List<Report> reports = List.of(





 TestFixtures.open(1L, TestFixtures.ALICE, "Alice", "Bob", "cheating", 1_000L),





 TestFixtures.report(2L, TestFixtures.ALICE, "Alice", "Bob", "cheating",







 ReportStatus.CLAIMED, 2_000L, 2_500L, 0L),





 TestFixtures.report(3L, TestFixtures.BOB, "Bob", "Charlie", "chat",







 ReportStatus.RESOLVED, 3_000L, 9_000L, 9_000L),





 TestFixtures.report(4L, TestFixtures.BOB, "Bob", "Charlie", "chat",







 ReportStatus.REJECTED, 4_000L, 6_000L, 6_000L),





 TestFixtures.report(5L, TestFixtures.STAFF, "Staff", "Dave", "grief",







 ReportStatus.DUPLICATE, 5_000L, 5_000L, 5_000L),





 TestFixtures.report(6L, TestFixtures.STAFF, "Staff", "Dave", "grief",







 ReportStatus.AUTO_CLOSED, 6_000L, 8_000L, 8_000L)



 );




ReportStats stats = ReportStats.compute(reports);




assertEquals(6, stats.total());



 assertEquals(1, stats.open());



 assertEquals(1, stats.claimed());



 assertEquals(1, stats.resolved());



 assertEquals(1, stats.rejected());



 assertEquals(1, stats.duplicate());



 assertEquals(1, stats.autoClosed());



 assertEquals(4, stats.closed());



 assertEquals((6_000L + 2_000L + 0L + 2_000L) / 4L, stats.averageHandlingMillis());



 assertEquals(Integer.valueOf(2), stats.byCategory().get("cheating"));



 assertEquals(Integer.valueOf(2), stats.byCategory().get("chat"));



 assertEquals(Integer.valueOf(2), stats.byTarget().get("Bob"));



 assertEquals(1, stats.topTargets(1).size());

 }


@Test

 void emptyStatsAreSafe() {



 ReportStats stats = ReportStats.compute(null);



 assertEquals(0, stats.total());



 assertEquals(0L, stats.averageHandlingMillis());



 assertTrue(stats.byCategory().isEmpty());



 assertEquals(0, ReportStats.empty().total());

 }


@Test

 void reportsWithoutClosureTimeDoNotSkewAverage() {



 ReportStats stats = ReportStats.compute(List.of(





 TestFixtures.report(1L, TestFixtures.ALICE, "Alice", "Bob", "chat",







 ReportStatus.RESOLVED, 1_000L, 1_000L, 0L),





 TestFixtures.report(2L, TestFixtures.ALICE, "Alice", "Bob", "chat",







 ReportStatus.RESOLVED, 1_000L, 3_000L, 3_000L)



 ));




assertEquals(2_000L, stats.averageHandlingMillis());

 }


@Test

 void staffStatisticsCountTerminalReportsPerCloser() {



 Report resolvedByStaff = TestFixtures.open(1L, TestFixtures.ALICE, "Alice", "Bob", "chat", 1_000L)





 .withClosed(Verdict.RESOLVED, TestFixtures.STAFF, "Staff", "", 5_000L);



 Report rejectedByStaff = TestFixtures.open(2L, TestFixtures.ALICE, "Alice", "Bob", "chat", 2_000L)





 .withClosed(Verdict.REJECTED, TestFixtures.STAFF, "Staff", "", 6_000L);



 Report resolvedByOther = TestFixtures.open(3L, TestFixtures.ALICE, "Alice", "Bob", "chat", 3_000L)





 .withClosed(Verdict.RESOLVED, TestFixtures.BOB, "Bob", "", 7_000L);



 Report stillOpen = TestFixtures.open(4L, TestFixtures.ALICE, "Alice", "Bob", "chat", 4_000L);




ReportStats stats = ReportStats.compute(List.of(





 resolvedByStaff, rejectedByStaff, resolvedByOther, stillOpen));




assertEquals(Integer.valueOf(2), stats.byStaff().get("Staff"));



 assertEquals(Integer.valueOf(1), stats.byStaff().get("Bob"));



 assertEquals("Staff", stats.topStaff(1).get(0).getKey());



 assertEquals(1, stats.topStaff(1).size());



 assertEquals(0, stats.topStaff(0).size());

 } }
