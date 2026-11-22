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
import java.util.List;
import java.util.UUID;

/**
 * Pure planning step for bulk closes (roadmap P2#12). Given a batch of report ids and a verdict,
 * it works out - without touching the repository - which reports will actually be closed, which
 * are already terminal (skipped), which ids do not exist, and whether the batch exceeded the
 * configured cap. The Bukkit service layer then applies the returned updates, fires events and
 * writes the single consolidated audit entry.
 */
public final class BulkClosePlan {

  /** What happens to one id of the batch. */
  public enum Disposition {
    /** The report was closed by this plan. */
    CLOSED,

    /** The report is already terminal; nothing changed. */
    SKIPPED_TERMINAL,

    /** The id does not resolve to a report. */
    MISSING,

    /** The id was not a positive number and was rejected. */
    INVALID
  }

  /** One planned batch, carrying the closed reports in input order. */
  public record Plan(
      List<Report> toClose,
      List<Long> skippedIds,
      List<Long> missingIds,
      boolean capped,
      int closed,
      int skipped,
      int missing) {

    public boolean isNoop() {

      return toClose.isEmpty() && skippedIds.isEmpty() && missingIds.isEmpty();
    }
  }

  private BulkClosePlan() {}

  /**
   * Plans a bulk close.
   *
   * @param repository the in-memory repository to look ids up
   * @param ids the batch, in the order given; truncated to {@code cap} (a leading slice) when
   *     longer
   * @param cap the maximum batch size ({@code reports.bulk-max}); values below 1 are treated as 1
   * @param verdict the verdict applied to every report that is not already terminal
   * @param staffId the acting admin's id, or the zero UUID for the console
   * @param staffName the acting admin's name
   * @param note an optional close note applied to every closed report
   * @param now the timestamp stamped onto the closures
   * @return the plan; the caller is responsible for applying {@link Plan#toClose()}
   */
  public static Plan plan(
      ReportRepository repository,
      List<Long> ids,
      int cap,
      Verdict verdict,
      UUID staffId,
      String staffName,
      String note,
      long now) {

    if (repository == null || ids == null || ids.isEmpty() || verdict == null) {

      return new Plan(List.of(), List.of(), List.of(), false, 0, 0, 0);
    }

    int effectiveCap = Math.max(1, cap);

    boolean capped = ids.size() > effectiveCap;

    List<Long> effective = capped ? ids.subList(0, effectiveCap) : ids;

    List<Report> toClose = new ArrayList<>();

    List<Long> skippedIds = new ArrayList<>();

    List<Long> missingIds = new ArrayList<>();

    for (Long id : effective) {

      if (id == null || id <= 0) {

        missingIds.add(id);

        continue;
      }

      var found = repository.get(id);

      if (found.isEmpty()) {

        missingIds.add(id);

        continue;
      }

      Report report = found.get();

      if (report.isTerminal()) {

        skippedIds.add(id);

        continue;
      }

      toClose.add(
          report.withClosed(
              verdict,
              staffId,
              staffName == null ? "" : staffName,
              note == null ? "" : note,
              now));
    }

    return new Plan(
        List.copyOf(toClose),
        List.copyOf(skippedIds),
        List.copyOf(missingIds),
        capped,
        toClose.size(),
        skippedIds.size(),
        missingIds.size());
  }
}
