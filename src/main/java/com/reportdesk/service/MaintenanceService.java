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

package com.reportdesk.service;

import com.reportdesk.ReportDeskPlugin;
import com.reportdesk.config.PluginConfig;
import com.reportdesk.core.Report;
import com.reportdesk.core.Verdict;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Auto-close and retention maintenance. Runs asynchronously; repository access is thread-safe and
 * all file work is delegated to the storage executors.
 */
public final class MaintenanceService {

  private static final UUID SYSTEM_ID = new UUID(0L, 0L);

  public record MaintenanceResult(
      int autoClosed, int purgedReports, int purgedEvidence, int purgedAudit) {

    public static MaintenanceResult empty() {

      return new MaintenanceResult(0, 0, 0, 0);
    }

    public boolean changed() {

      return autoClosed + purgedReports + purgedEvidence + purgedAudit > 0;
    }
  }

  private final ReportDeskPlugin plugin;

  public MaintenanceService(ReportDeskPlugin plugin) {

    this.plugin = plugin;
  }

  public CompletableFuture<MaintenanceResult> runOnce() {

    return CompletableFuture.supplyAsync(
        () -> {
          if (!plugin.repository().isReady()) {

            return MaintenanceResult.empty();
          }

          PluginConfig config = plugin.config();

          long now = System.currentTimeMillis();

          int autoClosed = 0;

          int purgedReports = 0;

          // Unsorted snapshot: the auto-close pass only visits reports,
          // the display order is irrelevant here.
          List<Report> snapshot = plugin.repository().snapshot();

          if (config.reports().autoCloseEnabled() && config.reports().autoCloseDays() > 0) {

            long cutoff = now - config.reports().autoCloseDays() * 86_400_000L;

            for (Report report : snapshot) {

              if (report.isOpen() && report.createdAt() <= cutoff) {

                Report closed =
                    report.withClosed(
                        Verdict.AUTO_CLOSED,
                        SYSTEM_ID,
                        ReportService.SYSTEM_NAME,
                        "auto-closed",
                        now);

                plugin.repository().put(closed);

                plugin.storage().saveReport(closed);

                autoClosed++;
              }
            }
          }

          if (config.reports().retentionDays() > 0) {

            long cutoff = now - config.reports().retentionDays() * 86_400_000L;

            for (Report report : new ArrayList<>(plugin.repository().snapshot())) {

              if (report.isTerminal() && report.closedAt() > 0 && report.closedAt() <= cutoff) {

                plugin.repository().remove(report.id());

                plugin.storage().deleteReport(report.id());

                plugin.storage().deleteEvidence(report.id());

                purgedReports++;
              }
            }
          }

          if (autoClosed > 0) {

            plugin.audit("AUTO_CLOSE", null, 0L, "reports=" + autoClosed);
          }

          if (purgedReports > 0) {

            plugin.audit("RETENTION_PURGE", null, 0L, "reports=" + purgedReports);
          }

          long evidenceCutoff =
              config.evidence().retentionDays() <= 0
                  ? 0L
                  : now - config.evidence().retentionDays() * 86_400_000L;

          CompletableFuture<Integer> evidenceFuture =
              evidenceCutoff <= 0
                  ? CompletableFuture.completedFuture(0)
                  : plugin.storage().purgeEvidenceBefore(evidenceCutoff);

          long auditCutoff =
              config.audit().retentionDays() <= 0
                  ? 0L
                  : now - config.audit().retentionDays() * 86_400_000L;

          CompletableFuture<Integer> auditFuture =
              auditCutoff <= 0
                  ? CompletableFuture.completedFuture(0)
                  : plugin.auditStorage().purgeBefore(auditCutoff);

          // Size cap (complements time-based retention; 0 disables).
          long auditMaxBytes =
              config.audit().maxFileSizeMb() > 0
                  ? config.audit().maxFileSizeMb() * 1_048_576L
                  : 0L;

          if (auditMaxBytes > 0) {

            auditFuture =
                auditFuture.thenCombine(
                    plugin.auditStorage().purgeOverSize(auditMaxBytes),
                    (deleted, sized) ->
                        Math.max(
                            deleted, sized)); // both delete audit files; report max
          }

          int finalAutoClosed = autoClosed;

          int finalPurgedReports = purgedReports;

          return evidenceFuture
              .thenCombine(
                  auditFuture,
                  (evidence, audit) ->
                      new MaintenanceResult(finalAutoClosed, finalPurgedReports, evidence, audit))
              .join();
        });
  }

  /**
   * Finds open reports past their SLA deadline and alerts staff once per
   *
   * <p>report. Runs off the main thread; notifications hop back to it.
   */
  public CompletableFuture<Integer> alertOverdue() {

    PluginConfig.Priority priority = plugin.config().reports().priority();

    if (!priority.enabled() || !priority.overdueAlertEnabled()) {

      return CompletableFuture.completedFuture(0);
    }

    return CompletableFuture.supplyAsync(
        () -> {
          if (!plugin.repository().isReady()) {

            return 0;
          }

          long now = System.currentTimeMillis();

          List<Report> overdue = new ArrayList<>();

          for (Report report : plugin.repository().snapshot()) {

            if (report.isOverdue(now) && report.slaAlertedAt() <= 0L) {

              Report updated = report.withSlaAlerted(now);

              plugin.repository().put(updated);

              plugin.storage().saveReport(updated);

              overdue.add(updated);
            }
          }

          if (!overdue.isEmpty()) {

            plugin.audit("SLA_ALERT", null, 0L, "reports=" + overdue.size());

            for (Report report : overdue) {

              plugin.notification().onOverdue(report);
            }
          }

          return overdue.size();
        });
  }

  /**
   * Pushes the SLA breach summary to the webhook. This is the weekly SLA report referenced by the
   * roadmap; it is part of the notification enhancement layer and is a no-op (single switch check,
   * no allocation) when {@code notifications.sla.weekly-enabled} is off. The window covers the
   * configured interval and aggregation is a pure in-memory snapshot read.
   */
  public CompletableFuture<Void> pushSlaWeekly() {

    if (!plugin.config().notifications().sla().weeklyEnabled()) {

      return CompletableFuture.completedFuture(null);
    }

    return CompletableFuture.runAsync(
        () -> {
          if (!plugin.repository().isReady()) {

            return;
          }

          int days = Math.max(1, plugin.config().notifications().sla().weeklyIntervalHours() / 24);

          long now = System.currentTimeMillis();

          long windowStart = now - days * 86_400_000L;

          List<Report> window = new ArrayList<>();

          for (Report report : plugin.repository().snapshot()) {

            if (report.createdAt() >= windowStart) {

              window.add(report);
            }
          }

          plugin
              .webhook()
              .slaBreach(com.reportdesk.core.SlaStats.compute(window, now));
        });
  }

  /**
   * Manual /reports purge [days]. Removes terminal reports older than the
   *
   * <p>supplied number of days and their evidence files.
   */
  public CompletableFuture<MaintenanceResult> purgeOlderThan(int days) {

    return CompletableFuture.supplyAsync(
        () -> {
          long now = System.currentTimeMillis();

          long cutoff = now - Math.max(0, days) * 86_400_000L;

          int purgedReports = 0;

          for (Report report : plugin.repository().snapshot()) {

            if (report.isTerminal() && report.closedAt() > 0 && report.closedAt() <= cutoff) {

              plugin.repository().remove(report.id());

              plugin.storage().deleteReport(report.id());

              plugin.storage().deleteEvidence(report.id());

              purgedReports++;
            }
          }

          if (purgedReports > 0) {

            plugin.audit("MANUAL_PURGE", null, 0L, "days=" + days + " reports=" + purgedReports);
          }

          CompletableFuture<Integer> evidenceFuture = plugin.storage().purgeEvidenceBefore(cutoff);

          CompletableFuture<Integer> auditFuture = plugin.auditStorage().purgeBefore(cutoff);

          int finalPurgedReports = purgedReports;

          return evidenceFuture
              .thenCombine(
                  auditFuture,
                  (evidence, audit) ->
                      new MaintenanceResult(0, finalPurgedReports, evidence, audit))
              .join();
        });
  }
}
