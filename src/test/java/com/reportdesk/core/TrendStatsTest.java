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
import org.junit.jupiter.api.Test;

class TrendStatsTest {

  /** An epoch instant pinned to exactly day 100 (UTC day boundary). */
  private static final long NOW = 100L * TrendStats.MILLISECONDS_PER_DAY;

  private static Report openAt(long id, long createdAt) {

    return TestFixtures.open(id, TestFixtures.ALICE, "Alice", "Bob", "chat", createdAt);
  }

  private static Report closedAt(long id, long createdAt, long closedAt) {

    return openAt(id, createdAt).withClosed(Verdict.RESOLVED, TestFixtures.STAFF, "Staff", "", closedAt);
  }

  private static long day(long index) {

    return index * TrendStats.MILLISECONDS_PER_DAY;
  }

  @Test
  void bucketsCreatedAndClosedIntoDailyPoints() {

    List<Report> reports =
        List.of(
            openAt(1L, day(99)),
            openAt(2L, day(100)),
            closedAt(3L, day(99), day(100)),
            closedAt(4L, day(98), day(99)));

    TrendStats.Trend trend = TrendStats.compute(reports, 7, NOW);

    assertEquals(7, trend.days());

    // Offset: window start = day 94; today (day 100) is the last point.

    // day 99 (index 5): reports 1 and 3 created, report 4 closed
    assertEquals(2, trend.dayPoints().get(5).created());
    assertEquals(1, trend.dayPoints().get(5).closed());

    // day 100 (index 6): report 2 created, report 3 closed
    assertEquals(1, trend.dayPoints().get(6).created());
    assertEquals(1, trend.dayPoints().get(6).closed());

    // day 98 (index 4): report 4 created
    assertEquals(1, trend.dayPoints().get(4).created());

    // day 94 (index 0): nothing
    assertEquals(0, trend.dayPoints().get(0).created());

    assertEquals(4, trend.totalCreated());

    assertEquals(2, trend.totalClosed());
  }

  @Test
  void windowExcludesOlderActivity() {

    List<Report> reports = List.of(openAt(1L, day(90)));

    TrendStats.Trend trend = TrendStats.compute(reports, 7, NOW);

    // day 90 is outside the 7-day window [94..100].

    assertFalse(trend.hasActivity());

    assertEquals(0, trend.totalCreated());
  }

  @Test
  void categoryShareIsSortedByCountAndBarScaledToTen() {

    // Two "chat" reports and one "grief" report create a 2/1 split.

    List<Report> mixed =
        List.of(
            openAt(1L, day(100)),
            openAt(2L, day(100)),
            TestFixtures.open(3L, TestFixtures.ALICE, "Alice", "Bob", "grief", day(100)));

    TrendStats.Trend trend = TrendStats.compute(mixed, 7, NOW);

    assertEquals(2, trend.categoryShares().size());

    assertEquals("chat", trend.categoryShares().get(0).category());

    assertEquals(2, trend.categoryShares().get(0).count());

    assertEquals(0.6666, trend.categoryShares().get(0).share(), 0.001);

    assertEquals(7, trend.categoryShares().get(0).bar());

    assertEquals("grief", trend.categoryShares().get(1).category());

    assertEquals(1, trend.categoryShares().get(1).count());

    assertEquals(3, trend.categoryShares().get(1).bar());
  }

  @Test
  void emptyAndNullInputsAreSafe() {

    TrendStats.Trend trend = TrendStats.compute(List.of(), 7, NOW);

    assertFalse(trend.hasActivity());

    assertEquals(0, trend.maxDayCount());

    assertTrue(trend.categoryShares().isEmpty());

    // Null reports still yields a full window of zero day-points (no crash).

    TrendStats.Trend nullTrend = TrendStats.compute(null, 7, NOW);

    assertEquals(7, nullTrend.dayPoints().size());

    assertFalse(nullTrend.hasActivity());

    assertEquals(0, nullTrend.totalCreated());
  }

  @Test
  void subOneWindowIsClampedToOneDay() {

    List<Report> reports = List.of(openAt(1L, day(100)), openAt(2L, day(99)));

    TrendStats.Trend trend = TrendStats.compute(reports, 0, NOW);

    assertEquals(1, trend.days());

    assertEquals(1, trend.totalCreated());
  }

  @Test
  void openReportsOnlyCountAsCreatedNotClosed() {

    List<Report> reports = List.of(openAt(1L, day(100)));

    TrendStats.Trend trend = TrendStats.compute(reports, 7, NOW);

    assertEquals(1, trend.totalCreated());

    assertEquals(0, trend.totalClosed());
  }
}
