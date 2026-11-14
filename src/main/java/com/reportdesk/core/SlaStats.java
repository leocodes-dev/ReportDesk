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
import java.util.Map;

/**
 * SLA aggregation computed from an in-memory snapshot of reports.
 *
 * <p>Produces per-dimension (priority, category, admin) overdue counts and
 * average overrun. Pure and thread-safe; no IO.
 */
public final class SlaStats {

  public record DimensionStats(String label, int overdueCount, long averageOverdueMillis) {

    public boolean hasOverdue() {

      return overdueCount > 0;
    }
  }

  public record SlaReport(
      int totalOverdue,
      long averageOverdueMillis,
      List<DimensionStats> byPriority,
      List<DimensionStats> byCategory,
      List<DimensionStats> byAdmin) {

    public boolean isEmpty() {

      return totalOverdue == 0;
    }

    public String summaryLine() {

      return totalOverdue + " overdue";
    }

    public String breakdownSummary() {

      StringBuilder sb = new StringBuilder();

      appendBreakdown(sb, byPriority);

      if (sb.length() > 0) {

        sb.append(" | ");
      }

      appendBreakdown(sb, byCategory);

      return sb.toString();
    }

    private static void appendBreakdown(StringBuilder sb, List<DimensionStats> dims) {

      for (DimensionStats dim : dims) {

        if (!dim.hasOverdue()) {

          continue;
        }

        if (sb.length() > 0) {

          sb.append(", ");
        }

        sb.append(dim.label()).append('=').append(dim.overdueCount());
      }
    }
  }

  private SlaStats() {}

  /**
   * Computes SLA statistics from a snapshot. Only reports with a configured
   * {@code slaDueAt} (> 0) that are not in a terminal state and whose deadline
   * has passed contribute.
   *
   * @param reports unsorted report snapshot
   * @param now current time in epoch milliseconds
   * @return the computed report, or an empty one when no reports are overdue
   */
  public static SlaReport compute(Collection<Report> reports, long now) {

    if (reports == null || reports.isEmpty()) {

      return emptyReport();
    }

    Map<String, List<Long>> priorityOverrun = new LinkedHashMap<>();

    Map<String, List<Long>> categoryOverrun = new LinkedHashMap<>();

    Map<String, List<Long>> adminOverrun = new LinkedHashMap<>();

    int totalOverdue = 0;

    long totalOverrun = 0L;

    for (Report report : reports) {

      if (report == null || report.isTerminal()) {

        continue;
      }

      if (report.slaDueAt() <= 0 || now <= report.slaDueAt()) {

        continue;
      }

      long overrun = now - report.slaDueAt();

      totalOverdue++;

      totalOverrun += overrun;

      priorityOverrun
          .computeIfAbsent(report.priority().id(), k -> new ArrayList<>())
          .add(overrun);

      categoryOverrun
          .computeIfAbsent(
              report.category().isBlank() ? "other" : report.category(), k -> new ArrayList<>())
          .add(overrun);

      String admin =
          report.claimedByName().isEmpty() ? "unclaimed" : report.claimedByName();

      adminOverrun.computeIfAbsent(admin, k -> new ArrayList<>()).add(overrun);
    }

    long avg = totalOverdue == 0 ? 0L : totalOverrun / totalOverdue;

    return new SlaReport(
        totalOverdue,
        avg,
        toDimensionStats(priorityOverrun),
        toDimensionStats(categoryOverrun),
        toDimensionStats(adminOverrun));
  }

  private static SlaReport emptyReport() {

    return new SlaReport(0, 0L, List.of(), List.of(), List.of());
  }

  private static List<DimensionStats> toDimensionStats(Map<String, List<Long>> groups) {

    List<DimensionStats> result = new ArrayList<>();

    for (Map.Entry<String, List<Long>> entry : groups.entrySet()) {

      long sum = 0L;

      for (long v : entry.getValue()) {

        sum += v;
      }

      result.add(
          new DimensionStats(entry.getKey(), entry.getValue().size(), sum / entry.getValue().size()));
    }

    result.sort(
        (a, b) -> {
          int cmp = Integer.compare(b.overdueCount(), a.overdueCount());

          return cmp != 0 ? cmp : a.label().compareTo(b.label());
        });

    return List.copyOf(result);
  }
}
