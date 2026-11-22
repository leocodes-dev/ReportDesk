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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.reportdesk.test.TestFixtures;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BulkClosePlanTest {

  private static final UUID STAFF = TestFixtures.STAFF;

  private static final long NOW = 1_000L;

  private static ReportRepository seed(List<Report> reports) {

    ReportRepository repository = new ReportRepository();

    repository.seed(reports);

    return repository;
  }

  private static Report open(long id) {

    return TestFixtures.open(id, TestFixtures.ALICE, "Alice", "Bob", "chat", 0L);
  }

  @Test
  void closesEveryNonTerminalReportWithTheVerdict() {

    ReportRepository repository = seed(List.of(open(1L), open(2L)));

    BulkClosePlan.Plan plan =
        BulkClosePlan.plan(repository, List.of(1L, 2L), 50, Verdict.RESOLVED, STAFF, "Staff", "note",
            NOW);

    assertEquals(2, plan.closed());

    assertEquals(0, plan.skipped());

    assertEquals(0, plan.missing());

    assertFalse(plan.capped());

    for (Report updated : plan.toClose()) {

      assertEquals(ReportStatus.RESOLVED, updated.status());

      assertEquals(STAFF, updated.closedBy());

      assertEquals("note", updated.closeNote());

      assertEquals(NOW, updated.closedAt());
    }
  }

  @Test
  void skipsTerminalReportsAndCountsMissingIds() {

    ReportRepository repository =
        seed(
            List.of(
                open(1L),
                open(2L).withClosed(Verdict.REJECTED, STAFF, "Staff", "", NOW)));

    BulkClosePlan.Plan plan =
        BulkClosePlan.plan(repository, List.of(1L, 2L, 99L), 50, Verdict.RESOLVED, STAFF, "Staff",
            "", NOW);

    assertEquals(1, plan.closed());

    assertEquals(List.of(2L), plan.skippedIds());

    assertEquals(List.of(99L), plan.missingIds());
  }

  @Test
  void truncatesTheBatchToTheCap() {

    ReportRepository repository = seed(List.of(open(1L), open(2L), open(3L)));

    BulkClosePlan.Plan plan =
        BulkClosePlan.plan(repository, List.of(1L, 2L, 3L), 2, Verdict.RESOLVED, STAFF, "Staff",
            "", NOW);

    assertTrue(plan.capped());

    assertEquals(2, plan.closed());

    // Only the leading slice of the batch is considered.

    assertEquals(List.of(1L, 2L), plan.toClose().stream().map(Report::id).toList());
  }

  @Test
  void invalidAndZeroCapIsClampedToOne() {

    ReportRepository repository = seed(List.of(open(1L), open(2L)));

    BulkClosePlan.Plan plan =
        BulkClosePlan.plan(repository, List.of(1L, 2L), 0, Verdict.RESOLVED, STAFF, "Staff", "",
            NOW);

    assertEquals(1, plan.closed());

    assertTrue(plan.capped());
  }

  @Test
  void invalidIdsAreCountedAsMissing() {

    ReportRepository repository = seed(List.of(open(1L)));

    BulkClosePlan.Plan plan =
        BulkClosePlan.plan(repository, List.of(1L, 0L, -5L), 50, Verdict.RESOLVED, STAFF, "Staff",
            "", NOW);

    assertEquals(1, plan.closed());

    assertEquals(2, plan.missing());
  }

  @Test
  void emptyOrNullInputsYieldNoopPlans() {

    ReportRepository repository = seed(List.of(open(1L)));

    assertTrue(BulkClosePlan.plan(repository, List.of(), 50, Verdict.RESOLVED, STAFF, "Staff", "",
        NOW).isNoop());

    assertTrue(BulkClosePlan.plan(repository, null, 50, Verdict.RESOLVED, STAFF, "Staff", "", NOW)
        .isNoop());

    assertTrue(
        BulkClosePlan.plan(repository, List.of(1L), 50, null, STAFF, "Staff", "", NOW).isNoop());
  }

  @Test
  void planDoesNotMutateTheRepository() {

    Report before = open(1L);

    ReportRepository repository = seed(List.of(before));

    BulkClosePlan.plan(repository, List.of(1L), 50, Verdict.RESOLVED, STAFF, "Staff", "", NOW);

    // The stored report is still in its original open state; applying the plan is the caller's
    // job.

    assertEquals(before, repository.get(1L).orElseThrow());
  }
}
