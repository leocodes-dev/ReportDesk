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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.reportdesk.test.TestFixtures;
import java.util.List;
import org.junit.jupiter.api.Test;

class SlaStatsTest {

  private static final long NOW = 1_000_000L;

  private static Report open(long id, ReportPriority priority, long dueAt, String category) {

    return TestFixtures.open(id, TestFixtures.ALICE, "Alice", "Bob", category, NOW)
        .withPriority(priority, dueAt, NOW);
  }

  @Test
  void emptySnapshotIsAnEmptyReport() {

    SlaStats.SlaReport report = SlaStats.compute(List.of(), NOW);

    assertTrue(report.isEmpty());

    assertEquals(0, report.totalOverdue());

    assertEquals(0, report.averageOverdueMillis());

    assertTrue(report.byPriority().isEmpty());
  }

  @Test
  void nullCollectionIsAnEmptyReport() {

    assertTrue(SlaStats.compute(null, NOW).isEmpty());
  }

  @Test
  void onlyOverdueNonTerminalReportsAreCounted() {

    // Terminal reports are excluded even when past SLA.
    Report resolved = TestFixtures.report(
        1L,
        TestFixtures.ALICE,
        "Alice",
        "Bob",
        "chat",
        ReportStatus.RESOLVED,
        NOW,
        NOW,
        NOW);

    // No SLA deadline at all.
    Report noSla = TestFixtures.open(2L, TestFixtures.ALICE, "Alice", "Bob", "chat", NOW);

    // Future deadline.
    Report future = open(3L, ReportPriority.NORMAL, NOW + 5_000L, "chat");

    // Past deadline, still open.
    Report overdue = open(4L, ReportPriority.NORMAL, NOW - 5_000L, "chat");

    SlaStats.SlaReport report =
        SlaStats.compute(List.of(resolved, noSla, future, overdue), NOW);

    assertEquals(1, report.totalOverdue());

    assertEquals(5_000L, report.averageOverdueMillis());
  }

  @Test
  void aggregatesByPriority() {

    Report urgent = open(1L, ReportPriority.URGENT, NOW - 10_000L, "chat");

    Report high = open(2L, ReportPriority.HIGH, NOW - 2_000L, "chat");

    SlaStats.SlaReport report = SlaStats.compute(List.of(urgent, high), NOW);

    assertEquals(2, report.totalOverdue());

    // Sorted by overdue count desc, then label; both have one each, so alphabetical.
    assertEquals(2, report.byPriority().size());

    long highTotal =
        report.byPriority().stream()
            .filter(d -> d.label().equals("high"))
            .mapToLong(SlaStats.DimensionStats::averageOverdueMillis)
            .sum();

    assertEquals(2_000L, highTotal);
  }

  @Test
  void aggregatesByClaimedAdminAndLabelsUnclaimed() {

    Report claimed =
        TestFixtures.open(1L, TestFixtures.ALICE, "Alice", "Bob", "chat", NOW)
            .withPriority(ReportPriority.NORMAL, NOW - 1_000L, NOW)
            .withClaim(TestFixtures.STAFF, "Dave", NOW);

    Report unclaimed = open(2L, ReportPriority.NORMAL, NOW - 3_000L, "chat");

    SlaStats.SlaReport report = SlaStats.compute(List.of(claimed, unclaimed), NOW);

    assertEquals(2, report.totalOverdue());

    assertEquals(2, report.byAdmin().size());

    SlaStats.DimensionStats unclaimedDim =
        report.byAdmin().stream().filter(d -> d.label().equals("unclaimed")).findFirst().orElseThrow();

    SlaStats.DimensionStats daveDim =
        report.byAdmin().stream().filter(d -> d.label().equals("Dave")).findFirst().orElseThrow();

    assertEquals(3_000L, unclaimedDim.averageOverdueMillis());

    assertEquals(1_000L, daveDim.averageOverdueMillis());
  }

  @Test
  void blankCategoryBucketsUnderOther() {

    Report blankCat = open(1L, ReportPriority.NORMAL, NOW - 1_000L, "   ");

    SlaStats.SlaReport report = SlaStats.compute(List.of(blankCat), NOW);

    assertEquals(1, report.byCategory().size());

    assertEquals("other", report.byCategory().get(0).label());
  }

  @Test
  void breakdownSummaryListsOverdueDimensions() {

    Report a = open(1L, ReportPriority.URGENT, NOW - 1_000L, "cheating");

    Report b = open(2L, ReportPriority.URGENT, NOW - 2_000L, "cheating");

    SlaStats.SlaReport report = SlaStats.compute(List.of(a, b), NOW);

    String breakdown = report.breakdownSummary();

    // Two urgent, two cheating.
    assertTrue(breakdown.contains("urgent=2"), "priority breakdown: " + breakdown);

    assertTrue(breakdown.contains("cheating=2"), "category breakdown: " + breakdown);
  }

  @Test
  void emptyReportHasNoBreakdown() {

    SlaStats.SlaReport empty = SlaStats.compute(List.of(), NOW);

    assertTrue(empty.isEmpty());

    assertTrue(empty.breakdownSummary().isEmpty());
  }
}
