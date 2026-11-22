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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Daily trend aggregation for the statistics dashboard (ROADMAP P2#9): how many reports were
 * created and closed on each of the last N days, plus the per-category share of all reports.
 *
 * <p>Pure and deterministic: day buckets are epoch-day indices (no time zones, no IO); the
 * caller renders the human-readable labels.
 */
public final class TrendStats {

  /** Milliseconds per UTC day; the bucketing unit. */
  public static final long MILLISECONDS_PER_DAY = 86_400_000L;

  /** One day's activity. */
  public record DayPoint(long dayIndex, int created, int closed) {}

  /** Per-category share of the whole (all statuses). */
  public record CategoryShare(String category, int count, double share, int bar) {

    public CategoryShare {

      category = category == null ? "" : category;
    }
  }

  /** The full trend snapshot. */
  public record Trend(
      int days,
      List<DayPoint> dayPoints,
      int totalCreated,
      int totalClosed,
      List<CategoryShare> categoryShares) {

    public boolean hasActivity() {

      return totalCreated > 0 || totalClosed > 0;
    }

    /** The highest single-day count; the scaling factor for the text bars. */
    public int maxDayCount() {

      int max = 0;

      for (DayPoint point : dayPoints) {

        max = Math.max(max, Math.max(point.created(), point.closed()));
      }

      return max;
    }
  }

  private TrendStats() {}

  /**
   * Computes the trend over the last {@code days} days (inclusive of today).
   *
   * @param reports the in-memory snapshot
   * @param days the window in days; values below 1 are clamped to 1
   * @param nowMillis the "current" instant in epoch milliseconds
   * @return the trend report
   */
  public static Trend compute(Collection<Report> reports, int days, long nowMillis) {

    int window = Math.max(1, days);

    long todayIndex = nowMillis / MILLISECONDS_PER_DAY;

    long windowStart = todayIndex - window + 1;

    // Index -> created / closed counters for the window.
    int[] created = new int[window];

    int[] closed = new int[window];

    // Category share over the whole history (independent of the trend window).
    LinkedHashMap<String, Integer> categoryCounts = new LinkedHashMap<>();

    int total = 0;

    if (reports != null) {

      for (Report report : reports) {

        if (report == null) {

          continue;
        }

        total++;

        String category =
            report.category().isEmpty()
                ? "other"
                : report.category().toLowerCase(Locale.ROOT);

        categoryCounts.merge(category, 1, Integer::sum);

        long createdDay = report.createdAt() / MILLISECONDS_PER_DAY;

        if (createdDay >= windowStart && createdDay <= todayIndex) {

          created[(int) (createdDay - windowStart)]++;
        }

        if (report.status().isTerminal()
            && report.closedAt() > 0
            && report.closedAt() >= report.createdAt()) {

          long closedDay = report.closedAt() / MILLISECONDS_PER_DAY;

          if (closedDay >= windowStart && closedDay <= todayIndex) {

            closed[(int) (closedDay - windowStart)]++;
          }
        }
      }
    }

    List<DayPoint> dayPoints = new ArrayList<>(window);

    int totalCreated = 0;

    int totalClosed = 0;

    for (int offset = 0; offset < window; offset++) {

      dayPoints.add(new DayPoint(windowStart + offset, created[offset], closed[offset]));

      totalCreated += created[offset];

      totalClosed += closed[offset];
    }

    List<CategoryShare> shares = new ArrayList<>(categoryCounts.size());

    for (Map.Entry<String, Integer> entry : categoryCounts.entrySet()) {

      double share = total == 0 ? 0.0 : entry.getValue() / (double) total;

      int bar = total == 0 ? 0 : (int) Math.round(share * 100.0 / 10.0);

      shares.add(new CategoryShare(entry.getKey(), entry.getValue(), share, bar));
    }

    shares.sort(
        (a, b) -> {
          int cmp = Integer.compare(b.count(), a.count());

          return cmp != 0 ? cmp : a.category().compareTo(b.category());
        });

    return new Trend(
        window, List.copyOf(dayPoints), totalCreated, totalClosed, List.copyOf(shares));
  }
}
