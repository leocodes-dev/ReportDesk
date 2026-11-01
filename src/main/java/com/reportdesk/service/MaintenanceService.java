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
import com.reportdesk.ReportDeskPlugin; import com.reportdesk.config.PluginConfig; import com.reportdesk.core.Report; import com.reportdesk.core.Verdict;
import java.util.ArrayList; import java.util.List; import java.util.UUID; import java.util.concurrent.CompletableFuture;
/**
* Auto-close and retention maintenance. Runs asynchronously; repository access
* is thread-safe and all file work is delegated to the storage executors.
*/ public final class MaintenanceService {


private static final UUID SYSTEM_ID = new UUID(0L, 0L);


public record MaintenanceResult(int autoClosed, int purgedReports, int purgedEvidence, int purgedAudit) {



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



 return CompletableFuture.supplyAsync(() -> {





 if (!plugin.repository().isReady()) {







 return MaintenanceResult.empty();





 }





 PluginConfig config = plugin.config();





 long now = System.currentTimeMillis();





 int autoClosed = 0;





 int purgedReports = 0;





 List<Report> snapshot = plugin.repository().all();






if (config.reports().autoCloseEnabled() && config.reports().autoCloseDays() > 0) {







 long cutoff = now - config.reports().autoCloseDays() * 86_400_000L;







 for (Report report : snapshot) {









 if (report.isOpen() && report.createdAt() <= cutoff) {











 Report closed = report.withClosed(Verdict.AUTO_CLOSED, SYSTEM_ID,













 ReportService.SYSTEM_NAME, "auto-closed", now);











 plugin.repository().put(closed);











 plugin.storage().saveReport(closed);











 autoClosed++;









 }







 }





 }






if (config.reports().retentionDays() > 0) {







 long cutoff = now - config.reports().retentionDays() * 86_400_000L;







 for (Report report : new ArrayList<>(plugin.repository().all())) {









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






long evidenceCutoff = config.evidence().retentionDays() <= 0







 ? 0L : now - config.evidence().retentionDays() * 86_400_000L;





 CompletableFuture<Integer> evidenceFuture = evidenceCutoff <= 0







 ? CompletableFuture.completedFuture(0)







 : plugin.storage().purgeEvidenceBefore(evidenceCutoff);





 long auditCutoff = config.audit().retentionDays() <= 0







 ? 0L : now - config.audit().retentionDays() * 86_400_000L;





 CompletableFuture<Integer> auditFuture = auditCutoff <= 0







 ? CompletableFuture.completedFuture(0)







 : plugin.auditStorage().purgeBefore(auditCutoff);






int finalAutoClosed = autoClosed;





 int finalPurgedReports = purgedReports;





 return evidenceFuture.thenCombine(auditFuture, (evidence, audit) ->







 new MaintenanceResult(finalAutoClosed, finalPurgedReports, evidence, audit)).join();



 });

 }


/**


* Finds open reports past their SLA deadline and alerts staff once per


* report. Runs off the main thread; notifications hop back to it.


*/

 public CompletableFuture<Integer> alertOverdue() {



 PluginConfig.Priority priority = plugin.config().reports().priority();



 if (!priority.enabled() || !priority.overdueAlertEnabled()) {





 return CompletableFuture.completedFuture(0);



 }



 return CompletableFuture.supplyAsync(() -> {





 if (!plugin.repository().isReady()) {







 return 0;





 }





 long now = System.currentTimeMillis();





 List<Report> overdue = new ArrayList<>();





 for (Report report : plugin.repository().all()) {







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


* Manual /reports purge [days]. Removes terminal reports older than the


* supplied number of days and their evidence files.


*/

 public CompletableFuture<MaintenanceResult> purgeOlderThan(int days) {



 return CompletableFuture.supplyAsync(() -> {





 long now = System.currentTimeMillis();





 long cutoff = now - Math.max(0, days) * 86_400_000L;





 int purgedReports = 0;





 for (Report report : new ArrayList<>(plugin.repository().all())) {







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





 return evidenceFuture.thenCombine(auditFuture, (evidence, audit) ->







 new MaintenanceResult(0, finalPurgedReports, evidence, audit)).join();



 });

 } }
