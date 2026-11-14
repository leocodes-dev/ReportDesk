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
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe in-memory report index. All GUI/command reads hit this map, never the disk.
 * Persistence is handled by the async storage layer.
 *
 * <p>Two secondary indexes keep the per-target lookups cheap instead of O(N) full scans:
 *
 * <ul>
 *   <li>{@code openByTarget} — the ids of the reports currently open, grouped by the normalised
 *       (lower-cased) target name, so {@link #countOpenForTarget} and {@link
 *       #findOpenDuplicate} only touch one target's handful of open reports;
 *   <li>{@code totalByTarget} — the total number of reports ever recorded per target, so {@link
 *       #countForTarget} is a map lookup.
 * </ul>
 *
 * <p>Both are maintained on every {@code put} / {@code remove} / {@code seed}; the target name of a
 * report never changes for a given report id (all {@code with*} copies in {@link Report} preserve
 * it), so the indexes cannot drift from the primary map.
 *
 * <p>Filtering additionally reuses a small per-id cache of lower-cased query fields (see {@code
 * queryFields}), so a text query over an unchanged repository costs no lower-casing at all; the
 * cache invalidates on every state change and is evicted on removal.
 */
public final class ReportRepository {

  private final ConcurrentHashMap<Long, Report> reports = new ConcurrentHashMap<>();

  private final AtomicLong nextId = new AtomicLong(1L);

  private volatile boolean ready;

  /** Open report ids grouped by normalised target name; the inner map acts as a set. */
  private final ConcurrentHashMap<String, ConcurrentHashMap<Long, Boolean>> openByTarget =
      new ConcurrentHashMap<>();

  /** Total report count per normalised target name (open and terminal alike). */
  private final ConcurrentHashMap<String, AtomicInteger> totalByTarget =
      new ConcurrentHashMap<>();

  /**
   * Memoised lower-cased query fields per report id, keyed by the {@code updatedAt} they were
   * built for. Entries are rebuilt lazily whenever a report is replaced with a newer state, so a
   * filter pass over an unchanged repository costs zero lower-casing. Evicted on removal.
   */
  private final ConcurrentHashMap<Long, SearchFieldEntry> searchFields =
      new ConcurrentHashMap<>();

  private static final class SearchFieldEntry {

    final long updatedAt;

    final String[] fields;

    SearchFieldEntry(long updatedAt, String[] fields) {

      this.updatedAt = updatedAt;

      this.fields = fields;
    }
  }

  /**
   * Merges reports loaded from disk. Any report already added before the load
   *
   * <p>finished (for example by an auto-detection task) is kept.
   */
  public void seed(Collection<Report> loaded) {

    long maxId = 0L;

    if (loaded != null) {

      for (Report report : loaded) {

        if (report == null) {

          continue;
        }

        if (reports.putIfAbsent(report.id(), report) == null) {

          indexAdded(report);
        }

        maxId = Math.max(maxId, report.id());
      }
    }

    long max = maxId;

    nextId.updateAndGet(current -> Math.max(current, max + 1));

    ready = true;
  }

  public boolean isReady() {

    return ready;
  }

  public long nextId() {

    return nextId.getAndUpdate(current -> current + 1);
  }

  public void put(Report report) {

    if (report == null) {

      return;
    }

    Report old = reports.put(report.id(), report);

    if (old == null) {

      indexAdded(report);
    } else if (old.isOpen() != report.isOpen()) {

      // Same report id, same target: only the open state changed.
      //
      if (report.isOpen()) {

        indexOpenAdd(report.id(), report.targetName());
      } else {

        indexOpenRemove(report.id(), report.targetName());
      }
    }
  }

  public void add(Report report) {

    put(report);
  }

  public Optional<Report> get(long id) {

    return Optional.ofNullable(reports.get(id));
  }

  public boolean remove(long id) {

    Report old = reports.remove(id);

    if (old != null) {

      indexRemoved(old);

      searchFields.remove(id);
    }

    return old != null;
  }

  public boolean contains(long id) {

    return reports.containsKey(id);
  }

  public int size() {

    return reports.size();
  }

  public List<Report> all() {

    List<Report> snapshot = new ArrayList<>(reports.values());

    snapshot.sort(ReportFilter.all().comparator());

    return snapshot;
  }

  /**
   * Unsorted copy of every current report. Use this from loops that only need to *visit* the
   * reports (maintenance passes, statistics, player summaries) instead of {@link #all()}, which
   * copies <em>and</em> sorts the whole collection.
   */
  public List<Report> snapshot() {

    return new ArrayList<>(reports.values());
  }

  /** Number of reports that are currently open; a lookup, not a scan. */
  public int openCount() {

    int total = 0;

    for (ConcurrentHashMap<Long, Boolean> set : openByTarget.values()) {

      total += set.size();
    }

    return total;
  }

  public List<Report> filter(ReportFilter filter) {

    ReportFilter effective = filter == null ? ReportFilter.all() : filter;

    List<Report> matches = new ArrayList<>();

    for (Report report : reports.values()) {

      if (effective.matches(report, queryFields(report))) {

        matches.add(report);
      }
    }

    matches.sort(effective.comparator());

    return matches;
  }

  /**
   * The memoised lower-cased query fields for one report. A cached entry is reused while the
   * report's {@code updatedAt} is unchanged, which (by the invariant documented on {@link
   * Report#matchesQuery(String, String[])}) means the searched fields are unchanged too.
   */
  private String[] queryFields(Report report) {

    SearchFieldEntry entry = searchFields.get(report.id());

    if (entry != null && entry.updatedAt == report.updatedAt()) {

      return entry.fields;
    }

    String[] fields = Report.searchFields(report);

    searchFields.put(report.id(), new SearchFieldEntry(report.updatedAt(), fields));

    return fields;
  }

  public Page<Report> page(ReportFilter filter, int page, int pageSize) {

    return Page.of(filter(filter), page, pageSize);
  }

  public List<Report> byReporter(UUID reporterId) {

    List<Report> matches = new ArrayList<>();

    if (reporterId != null) {

      for (Report report : reports.values()) {

        if (reporterId.equals(report.reporterId())) {

          matches.add(report);
        }
      }
    }

    matches.sort(Comparator.comparingLong(Report::createdAt).reversed());

    return matches;
  }

  public Optional<Report> findOpenDuplicate(UUID reporterId, String targetName, long since) {

    if (reporterId == null || targetName == null) {

      return Optional.empty();
    }

    // Only this target's open reports are considered; the weakly-consistent
    // key-set iteration stays safe while other threads add/remove entries.
    ConcurrentHashMap<Long, Boolean> openIds = openByTarget.get(normalizeTarget(targetName));

    if (openIds == null) {

      return Optional.empty();
    }

    for (Long id : openIds.keySet()) {

      Report report = reports.get(id);

      if (report != null
          && report.isOpen()
          && reporterId.equals(report.reporterId())
          && report.createdAt() >= since) {

        return Optional.of(report);
      }
    }

    return Optional.empty();
  }

  public int countOpenForTarget(String targetName) {

    if (targetName == null) {

      return 0;
    }

    ConcurrentHashMap<Long, Boolean> openIds = openByTarget.get(normalizeTarget(targetName));

    return openIds == null ? 0 : openIds.size();
  }

  /**
   * Total number of reports (open and closed) recorded against a target.
   *
   * <p>Used by the detail view to show repeat-offender history.
   */
  public int countForTarget(String targetName) {

    if (targetName == null) {

      return 0;
    }

    AtomicInteger count = totalByTarget.get(normalizeTarget(targetName));

    return count == null ? 0 : count.get();
  }

  public List<Report> openReports() {

    List<Report> matches = new ArrayList<>();

    for (Report report : reports.values()) {

      if (report.isOpen()) {

        matches.add(report);
      }
    }

    return matches;
  }

  // ---------------------------------------------------------------------

  // Secondary index maintenance

  // ---------------------------------------------------------------------

  private void indexAdded(Report report) {

    indexTotalAdd(report.targetName());

    if (report.isOpen()) {

      indexOpenAdd(report.id(), report.targetName());
    }
  }

  private void indexRemoved(Report report) {

    indexTotalRemove(report.targetName());

    if (report.isOpen()) {

      indexOpenRemove(report.id(), report.targetName());
    }
  }

  private void indexOpenAdd(long reportId, String targetName) {

    synchronized (openByTarget) {

      openByTarget
          .computeIfAbsent(normalizeTarget(targetName), ignored -> new ConcurrentHashMap<>())
          .put(reportId, Boolean.TRUE);
    }
  }

  private void indexOpenRemove(long reportId, String targetName) {

    synchronized (openByTarget) {

      ConcurrentHashMap<Long, Boolean> set = openByTarget.get(normalizeTarget(targetName));

      if (set != null) {

        set.remove(reportId);
      }
    }
  }

  private void indexTotalAdd(String targetName) {

    totalByTarget
        .computeIfAbsent(normalizeTarget(targetName), ignored -> new AtomicInteger(0))
        .incrementAndGet();
  }

  private void indexTotalRemove(String targetName) {

    AtomicInteger count = totalByTarget.get(normalizeTarget(targetName));

    if (count != null) {

      count.decrementAndGet();
    }
  }

  /** Normalises a target name the same way the reports store it (blank-safe, case-folded). */
  private static String normalizeTarget(String targetName) {

    if (targetName == null || targetName.isBlank()) {

      return "";
    }

    return targetName.trim().toLowerCase(Locale.ROOT);
  }
}
