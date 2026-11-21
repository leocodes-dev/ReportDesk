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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.reportdesk.test.TestFixtures;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReportAssignmentServiceTest {

  private static final Instant NOW = Instant.parse("2026-06-01T10:00:00Z");

  private static final UUID ZERO = new UUID(0L, 0L);

  private static final UUID OTHER = UUID.fromString("55555555-5555-5555-5555-555555555555");

  private ReportRepository repository;

  private ReportAssignmentService service;

  private void setup() {

    repository = new ReportRepository();

    repository.seed(List.of());

    service =
        new ReportAssignmentService(
            repository, Clock.fixed(NOW.minusSeconds(60), ZoneOffset.UTC));
  }

  private Report openReport() {

    Report report = TestFixtures.open(1L, TestFixtures.ALICE, "Alice", "Bob", "chat", 0L);

    repository.put(report);

    return report;
  }

  private Report claimedReport() {

    Report claimed = TestFixtures.open(1L, TestFixtures.ALICE, "Alice", "Bob", "chat", 0L)
        .withClaim(TestFixtures.STAFF, "Staff", NOW.minusSeconds(3600).toEpochMilli());

    repository.put(claimed);

    return claimed;
  }

  @Test
  void assignMovesAnOpenReportToTheTargetAdmin() {

    setup();

    Report report = openReport();

    var result = service.assign(report, OTHER, "Other");

    assertEquals(ReportAssignmentService.Outcome.ASSIGNED, result.outcome());

    assertTrue(result.isChange());

    assertSame(result.report(), repository.get(1L).orElseThrow());

    assertEquals(OTHER, result.report().claimedBy());

    assertEquals("Other", result.report().claimedByName());

    assertEquals(ReportStatus.CLAIMED, result.report().status());

    assertEquals(NOW.minusSeconds(60).toEpochMilli(), result.report().claimedAt());

    assertEquals("Other", result.placeholders().get("staff"));

    assertEquals("1", result.placeholders().get("id"));
  }

  @Test
  void assignToTheCurrentClaimerIsIdempotent() {

    setup();

    Report claimed = claimedReport();

    var result = service.assign(claimed, TestFixtures.STAFF, "Staff");

    assertEquals(ReportAssignmentService.Outcome.ALREADY_ASSIGNED, result.outcome());

    assertFalse(result.isChange());

    // No write happened: the repository still holds the original instance.

    assertSame(claimed, repository.get(1L).orElseThrow());
  }

  @Test
  void assignWhileClaimedBySomeoneElseFailsWithoutTransfer() {

    setup();

    Report claimed = claimedReport();

    var result = service.assign(claimed, OTHER, "Other");

    assertEquals(ReportAssignmentService.Outcome.CLAIMED_BY_OTHER, result.outcome());

    assertEquals("Staff", result.placeholders().get("staff"));

    assertSame(claimed, repository.get(1L).orElseThrow());
  }

  @Test
  void assignOfATerminalReportIsRejected() {

    setup();

    Report closed =
        TestFixtures.report(
            2L,
            TestFixtures.ALICE,
            "Alice",
            "Bob",
            "chat",
            ReportStatus.RESOLVED,
            0L,
            0L,
            100L);

    repository.put(closed);

    var result = service.assign(closed, OTHER, "Other");

    assertEquals(ReportAssignmentService.Outcome.TERMINAL, result.outcome());
  }

  @Test
  void transferMovesTheClaimAndKeepsTheOriginalClaimTime() {

    setup();

    Report claimed = claimedReport();

    long originalClaimAt = claimed.claimedAt();

    var result =
        service.transfer(claimed, TestFixtures.STAFF, false, OTHER, "Other");

    assertEquals(ReportAssignmentService.Outcome.TRANSFERRED, result.outcome());

    assertEquals(OTHER, result.report().claimedBy());

    assertEquals("Other", result.report().claimedByName());

    assertEquals(originalClaimAt, result.report().claimedAt());

    assertTrue(result.report().updatedAt() >= originalClaimAt);

    assertSame(result.report(), repository.get(1L).orElseThrow());
  }

  @Test
  void transferToTheCurrentClaimerIsIdempotent() {

    setup();

    Report claimed = claimedReport();

    var result = service.transfer(claimed, TestFixtures.STAFF, false, TestFixtures.STAFF, "Staff");

    assertEquals(ReportAssignmentService.Outcome.ALREADY_ASSIGNED, result.outcome());

    assertSame(claimed, repository.get(1L).orElseThrow());
  }

  @Test
  void transferOfAnUnclaimedReportFails() {

    setup();

    Report report = openReport();

    var result = service.transfer(report, TestFixtures.STAFF, false, OTHER, "Other");

    assertEquals(ReportAssignmentService.Outcome.NOT_CLAIMED, result.outcome());
  }

  @Test
  void transferByAPlayerWhoDoesNotHoldTheClaimNeedsOverride() {

    setup();

    Report claimed = claimedReport();

    var denied = service.transfer(claimed, OTHER, false, OTHER, "Other");

    assertEquals(ReportAssignmentService.Outcome.NOT_OWNER, denied.outcome());

    var allowed = service.transfer(claimed, OTHER, true, OTHER, "Other");

    assertEquals(ReportAssignmentService.Outcome.TRANSFERRED, allowed.outcome());
  }

  @Test
  void consoleActorsAlwaysNeedTheOverrideFlag() {

    setup();

    Report claimed = claimedReport();

    var denied = service.transfer(claimed, ZERO, false, OTHER, "Other");

    assertEquals(ReportAssignmentService.Outcome.NOT_OWNER, denied.outcome());

    var allowed = service.transfer(claimed, ZERO, true, OTHER, "Other");

    assertEquals(ReportAssignmentService.Outcome.TRANSFERRED, allowed.outcome());

    var nullActor = service.transfer(claimed, null, true, OTHER, "Other");

    assertEquals(ReportAssignmentService.Outcome.TRANSFERRED, nullActor.outcome());
  }

  @Test
  void transferOfATerminalReportIsRejected() {

    setup();

    Report closed =
        TestFixtures.report(
            3L,
            TestFixtures.ALICE,
            "Alice",
            "Bob",
            "chat",
            ReportStatus.AUTO_CLOSED,
            0L,
            0L,
            100L);

    repository.put(closed);

    var result = service.transfer(closed, TestFixtures.STAFF, true, OTHER, "Other");

    assertEquals(ReportAssignmentService.Outcome.TERMINAL, result.outcome());
  }
}
