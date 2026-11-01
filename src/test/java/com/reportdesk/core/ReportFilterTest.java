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
import static org.junit.jupiter.api.Assertions.assertEquals; import static org.junit.jupiter.api.Assertions.assertFalse; import static org.junit.jupiter.api.Assertions.assertTrue;
class ReportFilterTest {


@Test

 void changingStatusPreservesCategoryQueryClaimAndSort() {



 ReportFilter filter = ReportFilter.all()





 .withStatus(ReportStatus.OPEN)





 .withCategory("chat")





 .withQuery("bob")





 .withClaimedBy(TestFixtures.STAFF)





 .withSort(ReportFilter.SortOrder.OLDEST);




ReportFilter cleared = filter.withStatus(null);




assertTrue(cleared.statuses().isEmpty());



 assertEquals("chat", cleared.category());



 assertEquals("bob", cleared.query());



 assertEquals(TestFixtures.STAFF, cleared.claimedBy());



 assertEquals(ReportFilter.SortOrder.OLDEST, cleared.sort());

 }


@Test

 void completedFilterMatchesOnlyReportsSatisfyingEveryField() {



 ReportFilter filter = ReportFilter.all()





 .withStatus(ReportStatus.OPEN)





 .withCategory("cheating")





 .withQuery("bob");




assertTrue(filter.matches(TestFixtures.open(1L, TestFixtures.ALICE, "Alice", "Bob",





 "cheating", 1_000L)));



 assertFalse(filter.matches(TestFixtures.open(2L, TestFixtures.ALICE, "Alice", "Bob",





 "chat", 1_000L)));



 assertFalse(filter.matches(TestFixtures.open(3L, TestFixtures.ALICE, "Alice", "Charlie",





 "cheating", 1_000L)));

 }


@Test

 void sortComparatorOrdersByCreationTimeThenId() {



 Report early = TestFixtures.open(1L, TestFixtures.ALICE, "Alice", "Bob", "chat", 1_000L);



 Report late = TestFixtures.open(2L, TestFixtures.ALICE, "Alice", "Bob", "chat", 2_000L);



 Report sameTime = TestFixtures.open(3L, TestFixtures.ALICE, "Alice", "Bob", "chat", 2_000L);




List<Report> newest = new java.util.ArrayList<>(List.of(early, late, sameTime));



 newest.sort(ReportFilter.all().comparator());



 assertEquals(List.of(3L, 2L, 1L), newest.stream().map(Report::id).toList());




List<Report> oldest = new java.util.ArrayList<>(List.of(late, sameTime, early));



 oldest.sort(ReportFilter.all().withSort(ReportFilter.SortOrder.OLDEST).comparator());



 assertEquals(List.of(1L, 2L, 3L), oldest.stream().map(Report::id).toList());

 }


@Test

 void priorityAndOverdueFiltersMatchReports() {



 Report low = TestFixtures.open(1L, TestFixtures.ALICE, "Alice", "Bob", "chat", 1_000L);



 Report urgent = TestFixtures.open(2L, TestFixtures.ALICE, "Alice", "Bob", "chat", 2_000L)





 .withPriority(ReportPriority.URGENT, System.currentTimeMillis() - 60_000L, 2_000L);




ReportFilter priorityFilter = ReportFilter.all().withPriority(ReportPriority.URGENT);



 assertFalse(priorityFilter.matches(low));



 assertTrue(priorityFilter.matches(urgent));




ReportFilter overdueFilter = ReportFilter.all().withOverdueOnly(true);



 assertFalse(overdueFilter.matches(low));



 assertTrue(overdueFilter.matches(urgent));



 assertEquals(ReportPriority.LOW, ReportFilter.all().cyclePriority().priorities().iterator().next());



 assertEquals(ReportPriority.URGENT, ReportFilter.all().withPriority(ReportPriority.HIGH)





 .cyclePriority().priorities().iterator().next());



 assertTrue(ReportFilter.all().withPriority(ReportPriority.URGENT).cyclePriority()





 .priorities().isEmpty());

 }


@Test

 void prioritySortKeepsHigherPriorityFirstThenNewest() {



 Report low = TestFixtures.open(1L, TestFixtures.ALICE, "Alice", "Bob", "chat", 3_000L);



 Report highOld = TestFixtures.open(2L, TestFixtures.ALICE, "Alice", "Bob", "chat", 1_000L)





 .withPriority(ReportPriority.HIGH, 0L, 1_000L);



 Report highNew = TestFixtures.open(3L, TestFixtures.ALICE, "Alice", "Bob", "chat", 2_000L)





 .withPriority(ReportPriority.HIGH, 0L, 2_000L);




List<Report> reports = new java.util.ArrayList<>(List.of(low, highOld, highNew));



 reports.sort(ReportFilter.all().withSort(ReportFilter.SortOrder.PRIORITY).comparator());




assertEquals(List.of(3L, 2L, 1L), reports.stream().map(Report::id).toList());

 }


@Test

 void priorityQueryMatchesPriorityId() {



 Report urgent = TestFixtures.open(1L, TestFixtures.ALICE, "Alice", "Bob", "chat", 1_000L)





 .withPriority(ReportPriority.URGENT, 0L, 1_000L);




assertTrue(ReportFilter.all().withQuery("urgent").matches(urgent));



 assertFalse(ReportFilter.all().withQuery("low").matches(urgent));

 } }
