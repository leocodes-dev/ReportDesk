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

class SmartReviewStatsTest {

  private static final int GARBAGE = 30;

  private static Report closed(long id, long created, int score, Verdict verdict, long closedAt) {

    Report open = TestFixtures.open(id, TestFixtures.ALICE, "Alice", "Bob", "chat", created);

    return open.withClosed(verdict, TestFixtures.STAFF, "Staff", "", closedAt).withSmartScore(score);
  }

  @Test
  void computesPrecisionRecallAndFlagRate() {

    // 4 flagged (<=30), of which 2 are actually garbage (REJECTED/DUPLICATE);
    // 3 garbage total, so 1 garbage was NOT flagged (score 50).
    List<Report> reports =
        List.of(
            closed(1L, 0L, 20, Verdict.REJECTED, 100L), // flagged + garbage
            closed(2L, 0L, 10, Verdict.DUPLICATE, 100L), // flagged + garbage
            closed(3L, 0L, 25, Verdict.RESOLVED, 100L), // flagged but confirmed
            closed(4L, 0L, 50, Verdict.REJECTED, 100L), // garbage, not flagged
            closed(5L, 0L, 80, Verdict.RESOLVED, 100L)); // high, confirmed

    SmartReviewStats.Review review = SmartReviewStats.compute(reports, GARBAGE, 0L);

    assertEquals(5, review.scored());

    assertEquals(3, review.flagged());

    assertEquals(3, review.garbage());

    assertEquals(2, review.flaggedGarbage());

    assertEquals(2.0 / 3.0, review.precision(), 1e-9);

    assertEquals(2.0 / 3.0, review.recall(), 1e-9);

    assertEquals(3.0 / 5.0, review.flagRate(), 1e-9);

    int[] buckets = review.scoreBuckets();

    assertEquals(1, buckets[1]); // 10-19: score 10

    assertEquals(2, buckets[2]); // 20-29: scores 20, 25
  }

  @Test
  void bucketBandsGroupScoresIntoTenPointRings() {

    List<Report> reports =
        List.of(
            closed(1L, 0L, 0, Verdict.RESOLVED, 100L),
            closed(2L, 0L, 9, Verdict.RESOLVED, 100L),
            closed(3L, 0L, 10, Verdict.RESOLVED, 100L),
            closed(4L, 0L, 19, Verdict.RESOLVED, 100L),
            closed(5L, 0L, 90, Verdict.RESOLVED, 100L),
            closed(6L, 0L, 100, Verdict.RESOLVED, 100L));

    SmartReviewStats.Review review = SmartReviewStats.compute(reports, GARBAGE, 0L);

    assertEquals(1, review.scoreBuckets()[0]); // 0-9: only the 9 (0 means "unscored" and is excluded)

    assertEquals(2, review.scoreBuckets()[1]); // 10-19: scores 10, 19

    assertEquals(2, review.scoreBuckets()[9]); // 90-100: scores 90, 100
  }

  @Test
  void unscoredAndOpenReportsAreExcluded() {

    List<Report> reports =
        List.of(
            TestFixtures.open(1L, TestFixtures.ALICE, "Alice", "Bob", "chat", 0L), // open, no score
            closed(2L, 0L, 0, Verdict.RESOLVED, 100L)); // terminal but smartScore == 0

    SmartReviewStats.Review review = SmartReviewStats.compute(reports, GARBAGE, 0L);

    assertTrue(review.isEmpty());

    assertEquals(0, review.scored());
  }

  @Test
  void windowStartRestrictsToRecentReports() {

    List<Report> reports =
        List.of(
            closed(1L, 1_000L, 20, Verdict.REJECTED, 2_000L), // old
            closed(2L, 9_000L, 20, Verdict.REJECTED, 10_000L)); // recent

    SmartReviewStats.Review review = SmartReviewStats.compute(reports, GARBAGE, 5_000L);

    assertEquals(1, review.scored());

    // Only the recent report (created 9_000 >= 5_000) is inside the window, so the single
    // flagged-and-garbage entry drives both ratios to 1.0.

    assertEquals(1.0, review.flagRate(), 1e-9);
  }

  @Test
  void emptyWindowYieldsZerosNotDivisionErrors() {

    SmartReviewStats.Review review = SmartReviewStats.compute(List.of(), GARBAGE, 0L);

    assertTrue(review.isEmpty());

    assertEquals(0.0, review.precision(), 1e-9);

    assertEquals(0.0, review.recall(), 1e-9);

    assertEquals(0.0, review.flagRate(), 1e-9);

    assertEquals(0, SmartReviewStats.compute(null, GARBAGE, 0L).scored());
  }
}
