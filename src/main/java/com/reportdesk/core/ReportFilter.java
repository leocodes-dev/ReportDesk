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
 * Filter/sort model used by the admin GUI, the player history GUI and the text lists. It is
 * deliberately Bukkit-free so pagination and query matching can be unit tested.
 *
 * <p>{@code reporter} restricts a filter to one reporter's reports (the player history feature);
 * it is {@code null} for the global admin views.
 */
public record ReportFilter(
    java.util.Set<ReportStatus> statuses,
    String category,
    String query,
    java.util.UUID claimedBy,
    SortOrder sort,
    java.util.Set<ReportPriority> priorities,
    boolean overdueOnly,
    java.util.UUID reporter) {

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

    this(statuses, category, query, claimedBy, sort, java.util.Set.of(), false, null);
  }

  /** Backwards-compatible constructor without the reporter restriction. */
  public ReportFilter(
      java.util.Set<ReportStatus> statuses,
      String category,
      String query,
      java.util.UUID claimedBy,
      SortOrder sort,
      java.util.Set<ReportPriority> priorities,
      boolean overdueOnly) {

    this(statuses, category, query, claimedBy, sort, priorities, overdueOnly, null);
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
          java.util.Set.of(), category, query, claimedBy, sort, priorities, overdueOnly, reporter);
    }

    return new ReportFilter(
        java.util.Set.of(status),
        category,
        query,
        claimedBy,
        sort,
        priorities,
        overdueOnly,
        reporter);
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

    return new ReportFilter(
        statuses, newCategory, query, claimedBy, sort, priorities, overdueOnly, reporter);
  }

  public ReportFilter withQuery(String newQuery) {

    return new ReportFilter(
        statuses, category, newQuery, claimedBy, sort, priorities, overdueOnly, reporter);
  }

  public ReportFilter withClaimedBy(java.util.UUID staffId) {

    return new ReportFilter(
        statuses, category, query, staffId, sort, priorities, overdueOnly, reporter);
  }

  public ReportFilter withSort(SortOrder newSort) {

    return new ReportFilter(
        statuses, category, query, claimedBy, newSort, priorities, overdueOnly, reporter);
  }

  public ReportFilter withPriority(ReportPriority priority) {

    return new ReportFilter(
        statuses,
        category,
        query,
        claimedBy,
        sort,
        priority == null ? java.util.Set.of() : java.util.Set.of(priority),
        overdueOnly,
        reporter);
  }

  public ReportFilter withOverdueOnly(boolean overdue) {

    return new ReportFilter(statuses, category, query, claimedBy, sort, priorities, overdue, reporter);
  }

  /** Restricts the filter to the reports filed by one reporter (the player history view). */
  public ReportFilter withReporter(java.util.UUID reporterId) {

    return new ReportFilter(
        statuses, category, query, claimedBy, sort, priorities, overdueOnly, reporterId);
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

    return matches(report, null);
  }

  /**
   * Same as {@link #matches(Report)}, but the caller may supply the memoised lower-cased query
   * fields for the report (as {@link com.reportdesk.core.ReportRepository} does on each filter
   * pass) so repeated renders do not re-pay the {@code toLowerCase} work.
   *
   * @param cachedFields memoised lower-cased search fields, or {@code null} to build them.
   */
  public boolean matches(Report report, String[] cachedFields) {

    if (reporter != null && !reporter.equals(report.reporterId())) {

      return false;
    }

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

    // The per-field lower-casing is memoised by the caller's repository
    // state, so paging through a long list no longer re-pays it on every
    // render.
    return report.matchesQuery(query, cachedFields);
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
