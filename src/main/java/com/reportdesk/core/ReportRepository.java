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
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe in-memory report index. All GUI/command reads hit this map, never the disk.
 * Persistence is handled by the async storage layer.
 */
public final class ReportRepository {

  private final ConcurrentHashMap<Long, Report> reports = new ConcurrentHashMap<>();

  private final AtomicLong nextId = new AtomicLong(1L);

  private volatile boolean ready;

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

        reports.putIfAbsent(report.id(), report);

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

    if (report != null) {

      reports.put(report.id(), report);
    }
  }

  public void add(Report report) {

    put(report);
  }

  public Optional<Report> get(long id) {

    return Optional.ofNullable(reports.get(id));
  }

  public boolean remove(long id) {

    return reports.remove(id) != null;
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

  public List<Report> filter(ReportFilter filter) {

    ReportFilter effective = filter == null ? ReportFilter.all() : filter;

    List<Report> matches = new ArrayList<>();

    for (Report report : reports.values()) {

      if (effective.matches(report)) {

        matches.add(report);
      }
    }

    matches.sort(effective.comparator());

    return matches;
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

    for (Report report : reports.values()) {

      if (report.isOpen()
          && reporterId.equals(report.reporterId())
          && report.targetName().equalsIgnoreCase(targetName)
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

    int count = 0;

    for (Report report : reports.values()) {

      if (report.isOpen() && report.targetName().equalsIgnoreCase(targetName)) {

        count++;
      }
    }

    return count;
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

    int count = 0;

    for (Report report : reports.values()) {

      if (report.targetName().equalsIgnoreCase(targetName)) {

        count++;
      }
    }

    return count;
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
}
