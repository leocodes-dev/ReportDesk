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
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class RateLimiterTest {

  private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

  private MutableClock clock() {

    return new MutableClock(START);
  }

  @Test
  void cooldownBlocksThenAllowsAfterExpiry() {

    MutableClock clock = clock();

    RateLimiter limiter =
        new RateLimiter(new RateLimitPolicy(60, 0, 0, 0, 0), clock, ZoneOffset.UTC);

    ReportRepository repository = new ReportRepository();

    repository.seed(List.of());

    limiter.record(TestFixtures.ALICE, "Bob");

    RateLimiter.Decision blocked = limiter.check(repository, TestFixtures.ALICE, "Charlie");

    assertEquals(RateLimiter.Reason.COOLDOWN, blocked.reason());

    assertEquals(60, blocked.seconds());

    assertFalse(blocked.isAllowed());

    clock.advanceSeconds(30);

    assertEquals(30, limiter.check(repository, TestFixtures.ALICE, "Charlie").seconds());

    clock.advanceSeconds(30);

    assertTrue(limiter.check(repository, TestFixtures.ALICE, "Charlie").isAllowed());
  }

  @Test
  void dailyLimitIsPerReporterAndResetsNextDay() {

    MutableClock clock = clock();

    RateLimiter limiter =
        new RateLimiter(new RateLimitPolicy(0, 2, 0, 0, 0), clock, ZoneOffset.UTC);

    ReportRepository repository = new ReportRepository();

    repository.seed(List.of());

    limiter.record(TestFixtures.ALICE, "Bob");

    limiter.record(TestFixtures.ALICE, "Charlie");

    RateLimiter.Decision blocked = limiter.check(repository, TestFixtures.ALICE, "Dave");

    assertEquals(RateLimiter.Reason.DAILY_LIMIT, blocked.reason());

    assertEquals(2, blocked.limit());

    assertTrue(limiter.check(repository, TestFixtures.BOB, "Dave").isAllowed());

    clock.advanceSeconds(86_400);

    assertTrue(limiter.check(repository, TestFixtures.ALICE, "Dave").isAllowed());
  }

  @Test
  void sameTargetComparisonIgnoresCaseAndWhitespace() {

    MutableClock clock = clock();

    RateLimiter limiter =
        new RateLimiter(new RateLimitPolicy(0, 0, 300, 0, 0), clock, ZoneOffset.UTC);

    ReportRepository repository = new ReportRepository();

    repository.seed(List.of());

    limiter.record(TestFixtures.ALICE, " Bob ");

    RateLimiter.Decision blocked = limiter.check(repository, TestFixtures.ALICE, "bob");

    assertEquals(RateLimiter.Reason.SAME_TARGET, blocked.reason());

    assertTrue(limiter.check(repository, TestFixtures.ALICE, "Charlie").isAllowed());
  }

  @Test
  void duplicateWindowUsesOpenReportsFromRepository() {

    MutableClock clock = clock();

    ReportRepository repository = new ReportRepository();

    repository.seed(
        List.of(
            TestFixtures.open(
                7L,
                TestFixtures.ALICE,
                "Alice",
                "Bob",
                "cheating",
                START.toEpochMilli() - 1_000L)));

    RateLimiter limiter =
        new RateLimiter(new RateLimitPolicy(0, 0, 0, 600, 0), clock, ZoneOffset.UTC);

    RateLimiter.Decision decision = limiter.check(repository, TestFixtures.ALICE, "Bob");

    assertEquals(RateLimiter.Reason.DUPLICATE, decision.reason());

    assertEquals(7L, decision.duplicateId());
  }

  @Test
  void closedReportsAreNotDuplicates() {

    MutableClock clock = clock();

    ReportRepository repository = new ReportRepository();

    repository.seed(
        List.of(
            TestFixtures.report(
                7L,
                TestFixtures.ALICE,
                "Alice",
                "Bob",
                "cheating",
                ReportStatus.RESOLVED,
                START.toEpochMilli() - 1_000L,
                START.toEpochMilli() - 500L,
                START.toEpochMilli() - 500L)));

    RateLimiter limiter =
        new RateLimiter(new RateLimitPolicy(0, 0, 0, 600, 0), clock, ZoneOffset.UTC);

    assertTrue(limiter.check(repository, TestFixtures.ALICE, "Bob").isAllowed());
  }

  @Test
  void maxOpenPerTargetBlocksFurtherReports() {

    MutableClock clock = clock();

    ReportRepository repository = new ReportRepository();

    repository.seed(
        List.of(
            TestFixtures.open(
                1L, TestFixtures.ALICE, "Alice", "Bob", "cheating", START.toEpochMilli()),
            TestFixtures.open(2L, TestFixtures.BOB, "Bob", "bob", "chat", START.toEpochMilli())));

    RateLimiter limiter =
        new RateLimiter(new RateLimitPolicy(0, 0, 0, 0, 2), clock, ZoneOffset.UTC);

    RateLimiter.Decision decision = limiter.check(repository, UUIDHelper.THIRD, "BOB");

    assertEquals(RateLimiter.Reason.MAX_OPEN, decision.reason());

    assertEquals(2, decision.limit());
  }

  @Test
  void seededReportsRestoreCooldownAndDailyCounts() {

    MutableClock clock = clock();

    RateLimiter limiter =
        new RateLimiter(new RateLimitPolicy(60, 1, 0, 0, 0), clock, ZoneOffset.UTC);

    ReportRepository repository = new ReportRepository();

    repository.seed(List.of());

    limiter.seed(
        List.of(
            TestFixtures.open(
                1L, TestFixtures.ALICE, "Alice", "Bob", "cheating", START.toEpochMilli())));

    RateLimiter.Decision decision = limiter.check(repository, TestFixtures.ALICE, "Bob");

    assertEquals(RateLimiter.Reason.COOLDOWN, decision.reason());

    clock.advanceSeconds(61);

    assertEquals(
        RateLimiter.Reason.DAILY_LIMIT,
        limiter.check(repository, TestFixtures.ALICE, "Bob").reason());
  }

  @Test
  void resetClearsAllState() {

    MutableClock clock = clock();

    RateLimiter limiter =
        new RateLimiter(new RateLimitPolicy(60, 1, 300, 0, 0), clock, ZoneOffset.UTC);

    ReportRepository repository = new ReportRepository();

    repository.seed(List.of());

    limiter.record(TestFixtures.ALICE, "Bob");

    limiter.reset();

    assertTrue(limiter.check(repository, TestFixtures.ALICE, "Bob").isAllowed());
  }

  @Test
  void zeroPolicyAllowsEverything() {

    RateLimiter limiter =
        new RateLimiter(new RateLimitPolicy(0, 0, 0, 0, 0), clock(), ZoneOffset.UTC);

    assertTrue(limiter.check(new ReportRepository(), TestFixtures.ALICE, "Bob").isAllowed());
  }

  private static final class UUIDHelper {

    private static final java.util.UUID THIRD =
        java.util.UUID.fromString("44444444-4444-4444-4444-444444444444");
  }
}
