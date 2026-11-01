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

import java.util.Locale;

/**
 * Filter/sort model used by the admin GUI and the text list. It is deliberately Bukkit-free so
 * pagination and query matching can be unit tested.
 */
public record ReportFilter(
    java.util.Set<ReportStatus> statuses,
    String category,
    String query,
    java.util.UUID claimedBy,
    SortOrder sort,
    java.util.Set<ReportPriority> priorities,
    boolean overdueOnly) {

  public enum SortOrder {
    NEWEST,

    OLDEST,

    PRIORITY
  }

  public ReportFilter {

    statuses = statuses == null ? java.util.Set.of() : java.util.Set.copyOf(statuses);

    category = category == null ? "" : category.trim().toLowerCase(Locale.ROOT);

    query = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);

    sort = sort == null ? SortOrder.NEWEST : sort;

    priorities = priorities == null ? java.util.Set.of() : java.util.Set.copyOf(priorities);
  }

  /** Backwards-compatible constructor for callers that only use the old fields. */
  public ReportFilter(
      java.util.Set<ReportStatus> statuses,
      String category,
      String query,
      java.util.UUID claimedBy,
      SortOrder sort) {

    this(statuses, category, query, claimedBy, sort, java.util.Set.of(), false);
  }

  public static ReportFilter all() {

    return new ReportFilter(java.util.Set.of(), "", "", null, SortOrder.NEWEST);
  }

  public static ReportFilter ofStatus(ReportStatus status) {

    return all().withStatus(status);
  }

  public ReportFilter withStatus(ReportStatus status) {

    if (status == null) {

      return new ReportFilter(
          java.util.Set.of(), category, query, claimedBy, sort, priorities, overdueOnly);
    }

    return new ReportFilter(
        java.util.Set.of(status), category, query, claimedBy, sort, priorities, overdueOnly);
  }

  public ReportFilter cycleStatus() {

    ReportStatus[] values = ReportStatus.values();

    if (statuses.isEmpty()) {

      return withStatus(values[0]);
    }

    ReportStatus current = statuses.iterator().next();

    int next = current.ordinal() + 1;

    if (next >= values.length) {

      return withStatus(null);
    }

    return withStatus(values[next]);
  }

  public ReportFilter withCategory(String newCategory) {

    return new ReportFilter(statuses, newCategory, query, claimedBy, sort, priorities, overdueOnly);
  }

  public ReportFilter withQuery(String newQuery) {

    return new ReportFilter(statuses, category, newQuery, claimedBy, sort, priorities, overdueOnly);
  }

  public ReportFilter withClaimedBy(java.util.UUID staffId) {

    return new ReportFilter(statuses, category, query, staffId, sort, priorities, overdueOnly);
  }

  public ReportFilter withSort(SortOrder newSort) {

    return new ReportFilter(statuses, category, query, claimedBy, newSort, priorities, overdueOnly);
  }

  public ReportFilter withPriority(ReportPriority priority) {

    return new ReportFilter(
        statuses,
        category,
        query,
        claimedBy,
        sort,
        priority == null ? java.util.Set.of() : java.util.Set.of(priority),
        overdueOnly);
  }

  public ReportFilter withOverdueOnly(boolean overdue) {

    return new ReportFilter(statuses, category, query, claimedBy, sort, priorities, overdue);
  }

  public ReportFilter cyclePriority() {

    ReportPriority[] values = ReportPriority.values();

    if (priorities.isEmpty()) {

      return withPriority(values[0]);
    }

    ReportPriority current = priorities.iterator().next();

    int next = current.ordinal() + 1;

    if (next >= values.length) {

      return withPriority(null);
    }

    return withPriority(values[next]);
  }

  public boolean matches(Report report) {

    if (!statuses.isEmpty() && !statuses.contains(report.status())) {

      return false;
    }

    if (!category.isEmpty() && !category.equalsIgnoreCase(report.category())) {

      return false;
    }

    if (claimedBy != null && !claimedBy.equals(report.claimedBy())) {

      return false;
    }

    if (!priorities.isEmpty() && !priorities.contains(report.priority())) {

      return false;
    }

    if (overdueOnly && !report.isOverdue(System.currentTimeMillis())) {

      return false;
    }

    if (query.isEmpty()) {

      return true;
    }

    return contains(report.targetName())
        || contains(report.reporterName())
        || contains(report.category())
        || contains(report.detail())
        || contains(report.claimedByName())
        || contains(report.closedByName())
        || contains(report.verdict())
        || contains(report.priority().id())
        || contains(String.valueOf(report.id()))
        || contains("#" + report.id());
  }

  private boolean contains(String value) {

    return value != null && value.toLowerCase(Locale.ROOT).contains(query);
  }

  public java.util.Comparator<Report> comparator() {

    java.util.Comparator<Report> byCreated = java.util.Comparator.comparingLong(Report::createdAt);

    java.util.Comparator<Report> byId = java.util.Comparator.comparingLong(Report::id);

    java.util.Comparator<Report> byTime = byCreated.thenComparing(byId);

    return switch (sort) {
      case OLDEST -> byTime;

      case PRIORITY ->
          java.util.Comparator.comparingInt((Report report) -> report.priority().weight())
              .reversed()
              .thenComparing(byTime.reversed());

      default -> byTime.reversed();
    };
  }
}
