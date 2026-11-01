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
import com.reportdesk.test.TestFixtures; import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals; import static org.junit.jupiter.api.Assertions.assertFalse; import static org.junit.jupiter.api.Assertions.assertTrue;
class PlayerSummaryTest {


@Test

 void aggregatesReportsAsTargetAndReporter() {



 List<Report> reports = List.of(





 TestFixtures.open(1L, TestFixtures.ALICE, "Alice", "Bob", "cheating", 100L),





 TestFixtures.open(2L, TestFixtures.BOB, "Bob", "Bob", "chat", 200L),





 TestFixtures.report(3L, TestFixtures.ALICE, "Alice", "Bob", "cheating",







 ReportStatus.RESOLVED, 300L, 400L, 400L)



 );




List<PlayerSummary> summaries = PlayerSummary.build(reports, List.of("Carol"));



 assertEquals(3, summaries.size());




PlayerSummary bob = find(summaries, "Bob");



 assertEquals(3, bob.reportsAsTarget());



 assertEquals(1, bob.reportsAsReporter());



 assertEquals(2, bob.openAsTarget());



 assertEquals(1, bob.resolvedAsTarget());



 assertEquals(4, bob.totalReports());



 assertEquals(300L, bob.lastActivityAt());




PlayerSummary alice = find(summaries, "Alice");



 assertEquals(0, alice.reportsAsTarget());



 assertEquals(2, alice.reportsAsReporter());




PlayerSummary carol = find(summaries, "Carol");



 assertEquals(0, carol.totalReports());



 assertTrue(carol.online());

 }


@Test

 void sortsOpenTargetsFirstThenName() {



 List<Report> reports = List.of(





 TestFixtures.report(1L, TestFixtures.ALICE, "Alice", "Zed", "cheating",







 ReportStatus.RESOLVED, 100L, 100L, 100L),





 TestFixtures.open(2L, TestFixtures.ALICE, "Alice", "Yan", "cheating", 200L),





 TestFixtures.open(3L, TestFixtures.ALICE, "Alice", "Yan", "chat", 300L),





 TestFixtures.open(4L, TestFixtures.ALICE, "Alice", "Xia", "chat", 400L)



 );




List<PlayerSummary> summaries = PlayerSummary.build(reports, List.of());




assertEquals("Yan", summaries.get(0).name());



 assertEquals(2, summaries.get(0).openAsTarget());



 assertEquals("Xia", summaries.get(1).name());



 assertEquals("Zed", summaries.get(2).name());



 assertEquals("Alice", summaries.get(summaries.size() - 1).name());

 }


@Test

 void queryIsCaseInsensitiveAndMatchesPartialNames() {



 List<Report> reports = List.of(





 TestFixtures.open(1L, TestFixtures.ALICE, "Alice", "Bob", "cheating", 100L),





 TestFixtures.open(2L, TestFixtures.ALICE, "Alice", "Bobby", "cheating", 200L),





 TestFixtures.open(3L, TestFixtures.ALICE, "Alice", "Carol", "cheating", 300L)



 );




List<PlayerSummary> matches = PlayerSummary.build(reports, List.of(), " bOb ");



 assertEquals(2, matches.size());



 assertTrue(matches.stream().anyMatch(summary -> summary.name().equals("Bob")));



 assertTrue(matches.stream().anyMatch(summary -> summary.name().equals("Bobby")));




assertEquals(4, PlayerSummary.build(reports, List.of(), "").size());



 assertTrue(PlayerSummary.build(reports, List.of(), "nobody").isEmpty());

 }


@Test

 void namesAreMergedCaseInsensitively() {



 List<Report> reports = List.of(





 TestFixtures.open(1L, TestFixtures.ALICE, "Alice", "Bob", "cheating", 100L),





 TestFixtures.open(2L, TestFixtures.ALICE, "Alice", "bob", "chat", 200L)



 );




List<PlayerSummary> summaries = PlayerSummary.build(reports, List.of());



 PlayerSummary bob = find(summaries, "bob");



 assertEquals(2, bob.reportsAsTarget());



 assertEquals("Bob", bob.name());



 assertFalse(bob.online());

 }


private static PlayerSummary find(List<PlayerSummary> summaries, String name) {



 return summaries.stream()





 .filter(summary -> summary.name().equalsIgnoreCase(name))





 .findFirst()





 .orElseThrow(() -> new AssertionError("missing summary " + name));

 } }
