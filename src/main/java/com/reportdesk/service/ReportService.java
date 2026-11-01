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
import com.reportdesk.ReportDeskPlugin; import com.reportdesk.config.PluginConfig; import com.reportdesk.core.Evidence; import com.reportdesk.core.EvidenceMeta; import com.reportdesk.core.PriorityPolicy; import com.reportdesk.core.Report; import com.reportdesk.core.ReportCategory; import com.reportdesk.core.ReportChatSelection; import com.reportdesk.core.ReportNote; import com.reportdesk.core.ReportPriority; import com.reportdesk.core.ReportReply; import com.reportdesk.core.ReportStatus; import com.reportdesk.core.ReportSubmissionService; import com.reportdesk.core.Verdict; import com.reportdesk.i18n.MessageKeys; import com.reportdesk.i18n.Messages; import org.bukkit.Bukkit; import org.bukkit.entity.Player;
import java.util.LinkedHashMap; import java.util.List; import java.util.Map; import java.util.Optional;
/**
* Bukkit-facing report workflow: validates through the pure core service,
* captures optional evidence, persists asynchronously, writes audit entries and
* emits notifications.
*/ public final class ReportService {


public static final String SYSTEM_NAME = "SYSTEM";


public record OpResult(boolean success, String messageKey, Map<String, String> placeholders, Report report) {




public OpResult {





 placeholders = placeholders == null ? Map.of() : Map.copyOf(placeholders);



 }




public static OpResult ok(String key, Map<String, String> placeholders, Report report) {





 return new OpResult(true, key, placeholders, report);



 }




public static OpResult fail(String key, Map<String, String> placeholders) {





 return new OpResult(false, key, placeholders, null);



 }

 }


private final ReportDeskPlugin plugin;


public ReportService(ReportDeskPlugin plugin) {



 this.plugin = plugin;

 }


public ReportSubmissionService.SubmitResult submit(Player reporter, String targetName,



























String category, String detail,



























boolean bypassLimits) {



 return submit(reporter, targetName, category, detail, bypassLimits, List.of());

 }


public ReportSubmissionService.SubmitResult submit(Player reporter, String targetName,



























String category, String detail,



























boolean bypassLimits,



























List<ReportChatSelection> chatSelections) {



 Player onlineTarget = Bukkit.getPlayerExact(targetName);



 String normalizedTarget = plugin.validator().normalizeName(targetName);



 ReportSubmissionService.SubmitRequest request = new ReportSubmissionService.SubmitRequest(





 reporter.getUniqueId(),





 reporter.getName(),





 onlineTarget == null ? null : onlineTarget.getUniqueId(),





 normalizedTarget,





 onlineTarget != null,





 reporter.getFirstPlayed(),





 category,





 detail,





 bypassLimits,





 false,





 chatSelections



 );



 ReportSubmissionService.SubmitResult result = plugin.submissionService().submit(request);



 if (result.success()) {





 Report finalized = finalizeNewReport(result.report(), onlineTarget);





 return new ReportSubmissionService.SubmitResult(result.reason(), finalized, result.placeholders());



 }



 return result;

 }


public Optional<Report> submitSystem(String targetName, String category, String detail) {



 String normalized = plugin.validator().normalizeName(targetName);



 if (normalized.isBlank()) {





 return Optional.empty();



 }



 Player onlineTarget = Bukkit.getPlayerExact(normalized);



 ReportSubmissionService.SubmitRequest request = new ReportSubmissionService.SubmitRequest(





 new java.util.UUID(0L, 0L),





 SYSTEM_NAME,





 onlineTarget == null ? null : onlineTarget.getUniqueId(),





 normalized,





 onlineTarget != null,





 0L,





 category,





 detail,





 true,





 true



 );



 ReportSubmissionService.SubmitResult result = plugin.submissionService().submit(request);



 if (!result.success()) {





 plugin.getLogger().warning("System report rejected: " + result.reason()







 + " " + result.placeholders());





 return Optional.empty();



 }



 return Optional.of(finalizeNewReport(result.report(), onlineTarget));

 }


public OpResult claim(Report report, Player staff) {



 if (report == null) {





 return OpResult.fail(MessageKeys.GENERAL_REPORT_NOT_FOUND, Map.of("id", "?"));



 }



 if (!report.isOpen()) {





 return OpResult.fail(MessageKeys.ADMIN_ALREADY_CLOSED, Map.of("id", String.valueOf(report.id())));



 }



 if (report.isClaimed()) {





 return OpResult.fail(MessageKeys.ADMIN_ALREADY_CLAIMED, Map.of(







 "id", String.valueOf(report.id()),







 "staff", report.claimedByName()





 ));



 }



 long now = System.currentTimeMillis();



 Report updated = report.withClaim(staff.getUniqueId(), staff.getName(), now);



 update(updated);



 plugin.audit("CLAIM", staff, report.id(), "");



 plugin.notification().onClaimed(updated, staff.getName());



 return OpResult.ok(MessageKeys.ADMIN_CLAIMED, Map.of("id", String.valueOf(updated.id())), updated);

 }


public OpResult release(Report report, Player staff) {



 if (report == null) {





 return OpResult.fail(MessageKeys.GENERAL_REPORT_NOT_FOUND, Map.of("id", "?"));



 }



 if (!report.isClaimed()) {





 return OpResult.fail(MessageKeys.ADMIN_NOT_CLAIMED, Map.of("id", String.valueOf(report.id())));



 }



 Report updated = report.withClaimReleased(System.currentTimeMillis());



 update(updated);



 plugin.audit("RELEASE", staff, report.id(), "");



 return OpResult.ok(MessageKeys.ADMIN_RELEASED, Map.of("id", String.valueOf(updated.id())), updated);

 }


public OpResult close(Report report, Verdict verdict, Player staff, String note) {



 if (report == null) {





 return OpResult.fail(MessageKeys.GENERAL_REPORT_NOT_FOUND, Map.of("id", "?"));



 }



 if (report.isTerminal()) {





 return OpResult.fail(MessageKeys.ADMIN_ALREADY_CLOSED, Map.of("id", String.valueOf(report.id())));



 }



 if (verdict == null) {





 return OpResult.fail(MessageKeys.GENERAL_INVALID_VERDICT, Map.of());



 }



 String safeNote = note == null ? "" : note.trim();



 if (!safeNote.isEmpty()) {





 var validation = plugin.validator().validateNote(safeNote);





 if (!validation.valid()) {







 return OpResult.fail(MessageKeys.GENERAL_INVALID_INPUT, Map.of(









 "reason", validation.reason().name().toLowerCase(java.util.Locale.ROOT)







 ));





 }



 }



 Report updated = report.withClosed(verdict, staff.getUniqueId(), staff.getName(), safeNote,





 System.currentTimeMillis());



 update(updated);



 plugin.audit("CLOSE", staff, report.id(), verdict.name() + (safeNote.isBlank() ? "" : " | " + safeNote));



 plugin.notification().onClosed(updated, staff.getName());



 plugin.punishment().apply(updated, verdict, staff.getName());



 return OpResult.ok(MessageKeys.ADMIN_CLOSED, Map.of(





 "id", String.valueOf(updated.id()),





 "verdict", plugin.messages().plain(NotificationService.verdictKey(verdict.name()))



 ), updated);

 }


public OpResult reopen(Report report, Player staff) {



 if (report == null) {





 return OpResult.fail(MessageKeys.GENERAL_REPORT_NOT_FOUND, Map.of("id", "?"));



 }



 if (!report.isTerminal()) {





 return OpResult.fail(MessageKeys.ADMIN_NOT_CLOSED, Map.of("id", String.valueOf(report.id())));



 }



 Report updated = report.withReopened(System.currentTimeMillis());



 update(updated);



 plugin.audit("REOPEN", staff, report.id(), "");



 plugin.notification().onReopened(updated, staff.getName());



 return OpResult.ok(MessageKeys.ADMIN_REOPENED, Map.of("id", String.valueOf(updated.id())), updated);

 }


public OpResult delete(Report report, Player staff) {



 if (report == null) {





 return OpResult.fail(MessageKeys.GENERAL_REPORT_NOT_FOUND, Map.of("id", "?"));



 }



 plugin.repository().remove(report.id());



 plugin.storage().deleteReport(report.id());



 plugin.storage().deleteEvidence(report.id());



 plugin.audit("DELETE", staff, report.id(), report.targetName());



 return OpResult.ok(MessageKeys.ADMIN_DELETED, Map.of("id", String.valueOf(report.id())), report);

 }


public OpResult addNote(Report report, Player staff, String text) {



 if (report == null) {





 return OpResult.fail(MessageKeys.GENERAL_REPORT_NOT_FOUND, Map.of("id", "?"));



 }



 var validation = plugin.validator().validateNote(text);



 if (!validation.valid()) {





 Map<String, String> placeholders = new LinkedHashMap<>();





 placeholders.put("reason", validation.reason().name().toLowerCase(java.util.Locale.ROOT));





 placeholders.put("word", validation.word());





 return OpResult.fail(MessageKeys.GENERAL_INVALID_INPUT, placeholders);



 }



 int max = plugin.config().security().maxNotesPerReport();



 if (report.notes().size() >= max) {





 return OpResult.fail(MessageKeys.ADMIN_NOTES_FULL, Map.of("limit", String.valueOf(max)));



 }



 ReportNote note = new ReportNote(staff.getUniqueId(), staff.getName(),





 System.currentTimeMillis(), text.trim());



 Report updated = report.withNote(note, System.currentTimeMillis());



 update(updated);



 plugin.audit("NOTE", staff, report.id(), text.trim());



 return OpResult.ok(MessageKeys.ADMIN_NOTE_ADDED, Map.of("id", String.valueOf(updated.id())), updated);

 }


/**


* Flushes pending write-ahead log entries into reports.json. Called on


* plugin disable so the on-disk state is a clean snapshot.


*/

 public void flush() {



 plugin.storage().compactReports();

 }


/**


* Adds a message to the report conversation. Staff replies notify the


* reporter; player replies notify staff. The reporter's own messages are


* always considered read.


*/

 public OpResult addReply(Report report, Player author, String text, boolean staff) {



 if (report == null) {





 return OpResult.fail(MessageKeys.GENERAL_REPORT_NOT_FOUND, Map.of("id", "?"));



 }



 PluginConfig.Replies settings = plugin.config().replies();



 if (!settings.enabled()) {





 return OpResult.fail(staff ? MessageKeys.ADMIN_REPLY_DISABLED







 : MessageKeys.REPORT_REPLY_DISABLED, Map.of());



 }



 if (!staff) {





 if (!settings.allowPlayerReplies()) {







 return OpResult.fail(MessageKeys.REPORT_REPLY_NOT_ALLOWED, Map.of());





 }





 if (!report.reporterId().equals(author.getUniqueId())) {







 return OpResult.fail(MessageKeys.GENERAL_REPORT_NOT_FOUND,









 Map.of("id", String.valueOf(report.id())));





 }



 }



 var validation = plugin.validator().validateReply(text, settings.maxLength());



 if (!validation.valid()) {





 Map<String, String> placeholders = new LinkedHashMap<>();





 placeholders.put("reason", validation.reason().name().toLowerCase(java.util.Locale.ROOT));





 placeholders.put("word", validation.word());





 placeholders.put("max", String.valueOf(settings.maxLength()));





 return OpResult.fail(MessageKeys.GENERAL_INVALID_INPUT, placeholders);



 }



 if (report.replies().size() >= settings.maxPerReport()) {





 return OpResult.fail(staff ? MessageKeys.ADMIN_REPLY_FULL : MessageKeys.REPORT_REPLY_FULL,







 Map.of("limit", String.valueOf(settings.maxPerReport())));



 }



 long now = System.currentTimeMillis();



 ReportReply reply = new ReportReply(author.getUniqueId(), author.getName(), staff,





 text.trim(), now);



 Report updated = report.withReply(reply, now);



 if (!staff) {





 updated = updated.withReporterReadAt(now);



 }



 update(updated);



 plugin.audit(staff ? "REPLY" : "PLAYER_REPLY", author, report.id(), text.trim());



 if (settings.notifyOnReply()) {





 plugin.notification().onReply(updated, reply, staff);



 }



 return OpResult.ok(staff ? MessageKeys.ADMIN_REPLY_SENT : MessageKeys.REPORT_REPLY_SENT,





 Map.of("id", String.valueOf(updated.id())), updated);

 }


/**


* Marks staff replies as read for the reporter. Persistence stays async.


*/

 public void markReporterRead(Report report) {



 if (report == null || report.unreadReplies() <= 0) {





 return;



 }



 update(report.withReporterReadAt(System.currentTimeMillis()));

 }


public static String messageKey(ReportSubmissionService.SubmitReason reason) {



 return switch (reason) {





 case SUCCESS -> MessageKeys.REPORT_SUCCESS;





 case NOT_READY -> MessageKeys.GENERAL_LOADING;





 case INVALID_TARGET -> MessageKeys.GENERAL_PLAYER_NOT_FOUND;





 case CATEGORY_UNKNOWN -> MessageKeys.REPORT_CATEGORY_UNKNOWN;





 case CATEGORY_DISABLED -> MessageKeys.REPORT_CATEGORY_DISABLED;





 case SELF_REPORT -> MessageKeys.GENERAL_SELF_REPORT;





 case REQUIRES_ONLINE -> MessageKeys.GENERAL_TARGET_OFFLINE;





 case OFFLINE_DISABLED -> MessageKeys.GENERAL_OFFLINE_TARGET_DISABLED;





 case ACCOUNT_TOO_YOUNG -> MessageKeys.REPORT_ACCOUNT_TOO_YOUNG;





 case DETAIL_EMPTY -> MessageKeys.REPORT_DETAIL_REQUIRED;





 case DETAIL_TOO_SHORT -> MessageKeys.REPORT_DETAIL_SHORT;





 case DETAIL_TOO_LONG -> MessageKeys.REPORT_DETAIL_LONG;





 case DETAIL_ILLEGAL -> MessageKeys.GENERAL_INVALID_INPUT;





 case DETAIL_BLOCKED -> MessageKeys.REPORT_DETAIL_BLOCKED;





 case COOLDOWN -> MessageKeys.REPORT_COOLDOWN;





 case DAILY_LIMIT -> MessageKeys.REPORT_DAILY_LIMIT;





 case SAME_TARGET -> MessageKeys.REPORT_SAME_TARGET;





 case DUPLICATE -> MessageKeys.REPORT_DUPLICATE;





 case MAX_OPEN -> MessageKeys.REPORT_MAX_OPEN;



 };

 }


private Report finalizeNewReport(Report report, Player onlineTarget) {



 Report updated = applyPriority(report);



 if (onlineTarget != null && plugin.config().evidence().enabled()) {





 Evidence evidence = plugin.evidence().capture(onlineTarget, updated);





 if (evidence != null) {







 updated = updated.withEvidence(EvidenceMeta.of(evidence), System.currentTimeMillis());







 plugin.storage().saveEvidence(evidence);





 }



 }



 plugin.repository().put(updated);



 plugin.storage().saveReport(updated);



 plugin.audit(updated.systemGenerated() ? "SYSTEM_REPORT" : "REPORT_CREATE",





 null, updated.id(), updated.targetName() + " | " + updated.category());



 plugin.notification().onCreated(updated);



 return updated;

 }


private Report applyPriority(Report report) {



 PriorityPolicy policy = plugin.config().priorityPolicy();



 ReportPriority categorySeverity = plugin.config().category(report.category())





 .map(ReportCategory::severity)





 .orElse(ReportPriority.NORMAL);



 int openForTarget = plugin.repository().countOpenForTarget(report.targetName());



 ReportPriority effective = policy.resolve(categorySeverity, openForTarget);



 long dueAt = policy.dueAt(effective, report.createdAt());



 Report updated = report.withPriority(effective, dueAt, report.createdAt());



 plugin.repository().put(updated);



 return updated;

 }


private void update(Report report) {



 plugin.repository().put(report);



 plugin.storage().saveReport(report);

 } }
