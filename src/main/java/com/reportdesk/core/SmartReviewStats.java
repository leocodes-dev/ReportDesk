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

import java.util.Collection;

/**
 * Smart-priority retrospective (ROADMAP P1#6): cross-tabs the recorded smart scores of closed
 * reports against their final verdicts, so an admin can see whether the garbage band is
 * calibrated right and which way to move the thresholds.
 *
 * <p>The "ground truth" is the staff verdict: a report closed as {@link ReportStatus#RESOLVED}
 * was confirmed; everything else ({@code REJECTED}, {@code DUPLICATE}, {@code INSUFFICIENT},
 * {@code AUTO_CLOSED}) is treated as "actually garbage". A report counts as "flagged" when its
 * recorded score is at or below the configured garbage threshold.
 *
 * <p>Reports that carry no score ({@code smartScore == 0}) are excluded: the smart layer was off
 * when they were submitted, so there is no prediction to review.
 *
 * <p>Pure single pass over an in-memory snapshot; no IO.
 */
public final class SmartReviewStats {

  /** Ten 10-point bands: index 0 is 0-9, index 9 is 90-100. */
  public static final int BUCKETS = 10;

  /** One retrospective over a report window. */
  public record Review(
      int scored,
      int flagged,
      int garbage,
      int flaggedGarbage,
      int[] scoreBuckets,
      double precision,
      double recall,
      double flagRate) {

    public boolean isEmpty() {

      return scored == 0;
    }

    /** The share of flagged reports that turned out garbage; 0 when nothing was flagged. */
    public double precision() {

      return precision;
    }

    /** The share of garbage reports that were flagged; 0 when there was no garbage. */
    public double recall() {

      return recall;
    }

    /** The share of scored reports that were flagged (the "garbage flag rate"). */
    public double flagRate() {

      return flagRate;
    }
  }

  private SmartReviewStats() {}

  /**
   * Computes the retrospective over a snapshot.
   *
   * @param reports the in-memory report snapshot
   * @param garbageScore the configured garbage threshold ({@code score <= threshold} = flagged)
   * @param windowStartMillis only reports created at/after this instant are reviewed; {@code 0}
   *     reviews the full history
   * @return the review, empty when no scored closed report falls inside the window
   */
  public static Review compute(
      Collection<Report> reports, int garbageScore, long windowStartMillis) {

    int scored = 0;

    int flagged = 0;

    int garbage = 0;

    int flaggedGarbage = 0;

    int[] buckets = new int[BUCKETS];

    if (reports != null) {

      for (Report report : reports) {

        if (report == null || !report.status().isTerminal()) {

          continue;
        }

        if (report.smartScore() <= 0) {

          continue;
        }

        if (windowStartMillis > 0L && report.createdAt() < windowStartMillis) {

          continue;
        }

        scored++;

        buckets[Math.min(BUCKETS - 1, report.smartScore() / 10)]++;

        boolean isFlagged = report.smartScore() <= garbageScore;

        boolean isActuallyGarbage = report.status() != ReportStatus.RESOLVED;

        if (isFlagged) {

          flagged++;
        }

        if (isActuallyGarbage) {

          garbage++;
        }

        if (isFlagged && isActuallyGarbage) {

          flaggedGarbage++;
        }
      }
    }

    double precision = flagged == 0 ? 0.0 : flaggedGarbage / (double) flagged;

    double recall = garbage == 0 ? 0.0 : flaggedGarbage / (double) garbage;

    double flagRate = scored == 0 ? 0.0 : flagged / (double) scored;

    return new Review(
        scored, flagged, garbage, flaggedGarbage, buckets, precision, recall, flagRate);
  }
}
