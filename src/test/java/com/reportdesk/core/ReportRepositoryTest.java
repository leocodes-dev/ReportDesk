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
import com.reportdesk.test.TestFixtures; import org.junit.jupiter.api.BeforeEach; import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals; import static org.junit.jupiter.api.Assertions.assertFalse; import static org.junit.jupiter.api.Assertions.assertTrue;
class ReportRepositoryTest {


private ReportRepository repository;


@BeforeEach

 void setUp() {



 repository = new ReportRepository();



 repository.seed(List.of(





 TestFixtures.open(5L, TestFixtures.ALICE, "Alice", "Bob", "cheating", 1_000L),





 TestFixtures.open(2L, TestFixtures.BOB, "Bob", "Charlie", "chat", 2_000L),





 TestFixtures.report(9L, TestFixtures.ALICE, "Alice", "Bob", "cheating",







 ReportStatus.RESOLVED, 3_000L, 4_000L, 4_000L),





 TestFixtures.open(7L, TestFixtures.STAFF, "Staff", "Dave", "grief", 4_000L)



 ));

 }


@Test

 void seedKeepsHighestIdAndInitializesIdGenerator() {



 assertTrue(repository.isReady());



 assertEquals(4, repository.size());



 assertEquals(10L, repository.nextId());



 assertEquals(11L, repository.nextId());

 }


@Test

 void seedIsIdempotentAndKeepsEarlierEntries() {



 repository.put(TestFixtures.open(99L, TestFixtures.ALICE, "Alice", "Eve", "other", 5_000L));



 repository.seed(List.of(TestFixtures.open(5L, TestFixtures.ALICE, "Alice", "Bob",





 "cheating", 1_000L)));




assertEquals(5, repository.size());



 assertEquals(10L, repository.nextId());

 }


@Test

 void allSortsNewestFirst() {



 List<Report> all = repository.all();



 assertEquals(List.of(7L, 9L, 2L, 5L), all.stream().map(Report::id).toList());

 }


@Test

 void filterMatchesStatusCategoryQueryAndClaimedBy() {



 assertEquals(3, repository.filter(ReportFilter.ofStatus(ReportStatus.OPEN)).size());



 assertEquals(2, repository.filter(ReportFilter.all().withCategory("CHEATING")).size());



 assertEquals(1, repository.filter(ReportFilter.all().withQuery("charlie")).size());



 assertEquals(1, repository.filter(ReportFilter.all().withQuery("#9")).size());



 assertEquals(0, repository.filter(ReportFilter.all()





 .withClaimedBy(TestFixtures.STAFF)).size());

 }


@Test

 void filterSortsOldestWhenRequested() {



 List<Report> result = repository.filter(ReportFilter.all().withSort(ReportFilter.SortOrder.OLDEST));



 assertEquals(List.of(5L, 2L, 9L, 7L), result.stream().map(Report::id).toList());

 }


@Test

 void pageBoundsAreClamped() {



 Page<Report> first = repository.page(ReportFilter.all(), 1, 2);



 assertEquals(2, first.items().size());



 assertEquals(1, first.page());



 assertEquals(2, first.totalPages());



 assertTrue(first.hasNext());



 assertFalse(first.hasPrevious());




Page<Report> beyond = repository.page(ReportFilter.all(), 99, 2);



 assertEquals(2, beyond.page());



 assertFalse(beyond.hasNext());



 assertTrue(beyond.hasPrevious());

 }


@Test

 void pageWithNoItemsStillHasOnePage() {



 Page<Report> page = new ReportRepository().page(ReportFilter.all(), 5, 10);



 assertEquals(0, page.totalItems());



 assertEquals(1, page.page());



 assertEquals(1, page.totalPages());



 assertTrue(page.items().isEmpty());

 }


@Test

 void byReporterIsNewestFirst() {



 List<Report> reports = repository.byReporter(TestFixtures.ALICE);



 assertEquals(List.of(9L, 5L), reports.stream().map(Report::id).toList());



 assertTrue(repository.byReporter(null).isEmpty());

 }


@Test

 void findOpenDuplicateHonoursCutoffAndOpenState() {



 assertTrue(repository.findOpenDuplicate(TestFixtures.ALICE, "bob", 0L).isPresent());



 assertTrue(repository.findOpenDuplicate(TestFixtures.ALICE, "bob", 1_500L).isEmpty());



 assertTrue(repository.findOpenDuplicate(TestFixtures.BOB, "bob", 0L).isEmpty());



 assertTrue(repository.findOpenDuplicate(null, "bob", 0L).isEmpty());

 }


@Test

 void countOpenForTargetIgnoresTerminalReportsAndCase() {



 assertEquals(1, repository.countOpenForTarget("BOB"));



 repository.put(TestFixtures.open(20L, TestFixtures.BOB, "Bob", "Bob", "chat", 6_000L));



 assertEquals(2, repository.countOpenForTarget("bob"));



 assertEquals(0, repository.countOpenForTarget(null));

 }


@Test

 void countForTargetIncludesOpenAndClosedReports() {



 assertEquals(2, repository.countForTarget("BOB"));



 assertEquals(2, repository.countForTarget("bob"));



 assertEquals(1, repository.countForTarget("Charlie"));



 assertEquals(0, repository.countForTarget(null));



 assertEquals(0, repository.countForTarget("Nobody"));

 }


@Test

 void openReportsExcludeClosedOnes() {



 List<Report> open = repository.openReports();



 assertEquals(3, open.size());



 assertTrue(open.stream().allMatch(Report::isOpen));

 }


@Test

 void removeAndContainsWork() {



 assertTrue(repository.contains(5L));



 assertTrue(repository.remove(5L));



 assertFalse(repository.remove(5L));



 assertFalse(repository.contains(5L));



 assertTrue(repository.get(5L).isEmpty());

 } }
