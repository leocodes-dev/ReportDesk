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
import com.reportdesk.ReportDeskPlugin; import com.reportdesk.config.PluginConfig; import com.reportdesk.core.Report; import com.reportdesk.core.ReportPriority; import com.reportdesk.core.ReportReply; import com.reportdesk.i18n.MessageKeys; import com.reportdesk.i18n.Messages; import com.reportdesk.util.TimeText; import net.kyori.adventure.text.Component; import org.bukkit.Bukkit; import org.bukkit.entity.Player;
import java.util.LinkedHashMap; import java.util.Map;
/**
* Staff/reporter notifications. Everything is sent on the main thread; the
* webhook call is asynchronous and never blocks.
*/ public final class NotificationService {


private final ReportDeskPlugin plugin;


public NotificationService(ReportDeskPlugin plugin) {



 this.plugin = plugin;

 }


public void onCreated(Report report) {



 runSync(() -> {





 Messages messages = plugin.messages();





 PluginConfig.Notifications settings = plugin.config().notifications();





 Map<String, String> placeholders = placeholders(report);





 if (settings.staffEnabled()) {







 sendStaff(messages.prefixed(MessageKeys.NOTIFY_STAFF_NEW, placeholders));







 playStaffSound(settings);





 }





 if (settings.broadcastEnabled()) {







 Component broadcast = messages.prefixed(MessageKeys.NOTIFY_BROADCAST_NEW, placeholders);







 for (Player online : Bukkit.getOnlinePlayers()) {









 online.sendMessage(broadcast);







 }





 }





 plugin.webhook().created(report);





 plugin.watchlist().onReportCreated(report);



 });

 }


public void onClaimed(Report report, String staffName) {



 runSync(() -> {





 Messages messages = plugin.messages();





 Map<String, String> placeholders = placeholders(report);





 placeholders.put("staff", staffName == null ? "" : staffName);





 if (plugin.config().notifications().staffEnabled()) {







 sendStaff(messages.prefixed(MessageKeys.NOTIFY_STAFF_CLAIMED, placeholders));





 }





 notifyReporter(report, MessageKeys.NOTIFY_REPORTER_CLAIMED, placeholders);





 plugin.webhook().claimed(report, staffName);



 });

 }


public void onClosed(Report report, String staffName) {



 runSync(() -> {





 Messages messages = plugin.messages();





 Map<String, String> placeholders = placeholders(report);





 placeholders.put("staff", staffName == null ? "" : staffName);





 if (plugin.config().notifications().staffEnabled()) {







 sendStaff(messages.prefixed(MessageKeys.NOTIFY_STAFF_CLOSED, placeholders));





 }





 notifyReporter(report, MessageKeys.NOTIFY_REPORTER_CLOSED, placeholders);





 plugin.webhook().closed(report, staffName);



 });

 }


public void onReopened(Report report, String staffName) {



 runSync(() -> {





 Messages messages = plugin.messages();





 Map<String, String> placeholders = placeholders(report);





 placeholders.put("staff", staffName == null ? "" : staffName);





 if (plugin.config().notifications().staffEnabled()) {







 sendStaff(messages.prefixed(MessageKeys.NOTIFY_STAFF_REOPENED, placeholders));





 }





 notifyReporter(report, MessageKeys.NOTIFY_REPORTER_REOPENED, placeholders);



 });

 }


public void onAutoDetected(Report report, int count, int windowSeconds) {



 runSync(() -> {





 Messages messages = plugin.messages();





 Map<String, String> placeholders = placeholders(report);





 placeholders.put("count", String.valueOf(count));





 placeholders.put("seconds", String.valueOf(windowSeconds));





 sendStaff(messages.prefixed(MessageKeys.EVIDENCE_AUTO_REPORT_STAFF, placeholders));





 plugin.webhook().created(report);



 });

 }


public void staffAlert(Component message) {



 runSync(() -> sendStaff(message));

 }


/**


* Staff replies go to the reporter; player replies go to online staff.


*/

 public void onReply(Report report, ReportReply reply, boolean staff) {



 runSync(() -> {





 Messages messages = plugin.messages();





 Map<String, String> placeholders = placeholders(report);





 placeholders.put("author", reply.authorName());





 placeholders.put("text", reply.text());





 if (staff) {







 notifyReporter(report, MessageKeys.NOTIFY_REPORTER_REPLY, placeholders);





 } else {







 sendStaff(messages.prefixed(MessageKeys.NOTIFY_STAFF_REPLY, placeholders));





 }



 });

 }


public void onOverdue(Report report) {



 runSync(() -> {





 Messages messages = plugin.messages();





 Map<String, String> placeholders = placeholders(report);





 long now = System.currentTimeMillis();





 long over = report.slaDueAt() <= 0 ? 0L : Math.max(0L, now - report.slaDueAt());





 placeholders.put("overdue", TimeText.duration(over, messages));





 sendStaff(messages.prefixed(MessageKeys.NOTIFY_STAFF_OVERDUE, placeholders));



 });

 }


private void notifyReporter(Report report, String key, Map<String, String> placeholders) {



 if (!plugin.config().notifications().reporterEnabled()) {





 return;



 }



 Player reporter = Bukkit.getPlayer(report.reporterId());



 if (reporter == null) {





 plugin.offlineNotices().enqueue(report, key, placeholders);





 return;



 }



 reporter.sendMessage(plugin.messages().prefixed(key, placeholders));

 }


/**


* Extra alert for staff who explicitly follow the reported player.


*/

 public void onWatchedReport(Report report, java.util.Set<java.util.UUID> watcherIds) {



 if (report == null || watcherIds == null || watcherIds.isEmpty()) {





 return;



 }



 runSync(() -> {





 Messages messages = plugin.messages();





 Map<String, String> placeholders = placeholders(report);





 Component message = messages.prefixed(MessageKeys.NOTIFY_STAFF_WATCHED, placeholders);





 for (java.util.UUID watcherId : watcherIds) {







 Player online = Bukkit.getPlayer(watcherId);







 if (online != null && online.hasPermission("reportdesk.admin")) {









 online.sendMessage(message);







 }





 }



 });

 }


private void sendStaff(Component message) {



 PluginConfig.Notifications settings = plugin.config().notifications();



 String permission = settings.staffPermission();



 for (Player online : Bukkit.getOnlinePlayers()) {





 if (online.hasPermission(permission)) {







 online.sendMessage(message);





 }



 }



 Bukkit.getConsoleSender().sendMessage(message);

 }


private void playStaffSound(PluginConfig.Notifications settings) {



 if (!settings.staffSoundEnabled() || settings.staffSound().isBlank()) {





 return;



 }



 for (Player online : Bukkit.getOnlinePlayers()) {





 if (online.hasPermission(settings.staffPermission())) {







 try {









 online.playSound(online.getLocation(), settings.staffSound(), 0.8f, 1.2f);







 } catch (RuntimeException ignored) {









 // Invalid sound configured; do not break notifications.







 }





 }



 }

 }


public Map<String, String> placeholders(Report report) {



 Messages messages = plugin.messages();



 Map<String, String> placeholders = new LinkedHashMap<>();



 placeholders.put("id", String.valueOf(report.id()));



 placeholders.put("target", report.targetName());



 placeholders.put("reporter", report.reporterName());



 placeholders.put("category", plugin.categoryDisplay(report.category()));



 placeholders.put("status", messages.plain(statusKey(report.status())));



 placeholders.put("verdict", report.verdict().isBlank()





 ? messages.plain(MessageKeys.GENERAL_NONE)





 : messages.plain(verdictKey(report.verdict())));



 placeholders.put("created", com.reportdesk.util.TimeText.absolute(report.createdAt(), messages));



 placeholders.put("claimed", report.claimedByName().isBlank()





 ? messages.plain(MessageKeys.GENERAL_NONE) : report.claimedByName());



 placeholders.put("closed", report.closedByName().isBlank()





 ? messages.plain(MessageKeys.GENERAL_NONE) : report.closedByName());



 placeholders.put("detail", report.detail().isBlank()





 ? messages.plain(MessageKeys.GENERAL_NONE) : report.detail());



 placeholders.put("priority", messages.plain(priorityKey(report.priority())));



 placeholders.put("sla", report.slaDueAt() <= 0





 ? messages.plain(MessageKeys.GENERAL_SLA_NONE)





 : TimeText.absolute(report.slaDueAt(), messages));



 placeholders.put("overdue", messages.plain(report.isOverdue(System.currentTimeMillis())





 ? MessageKeys.GENERAL_YES : MessageKeys.GENERAL_NO));



 placeholders.put("replies", String.valueOf(report.replies().size()));



 placeholders.put("unread", String.valueOf(report.unreadReplies()));



 return placeholders;

 }


public static String statusKey(com.reportdesk.core.ReportStatus status) {



 return switch (status) {





 case OPEN -> MessageKeys.STATUS_OPEN;





 case CLAIMED -> MessageKeys.STATUS_CLAIMED;





 case RESOLVED -> MessageKeys.STATUS_RESOLVED;





 case REJECTED -> MessageKeys.STATUS_REJECTED;





 case DUPLICATE -> MessageKeys.STATUS_DUPLICATE;





 case AUTO_CLOSED -> MessageKeys.STATUS_AUTO_CLOSED;



 };

 }


public static String priorityKey(ReportPriority priority) {



 if (priority == null) {





 return MessageKeys.PRIORITY_NORMAL;



 }



 return switch (priority) {





 case LOW -> MessageKeys.PRIORITY_LOW;





 case HIGH -> MessageKeys.PRIORITY_HIGH;





 case URGENT -> MessageKeys.PRIORITY_URGENT;





 default -> MessageKeys.PRIORITY_NORMAL;



 };

 }


public static String verdictKey(String verdict) {



 if (verdict == null || verdict.isBlank()) {





 return MessageKeys.GENERAL_NONE;



 }



 return switch (verdict.toUpperCase(java.util.Locale.ROOT)) {





 case "RESOLVED" -> MessageKeys.VERDICT_RESOLVED;





 case "REJECTED" -> MessageKeys.VERDICT_REJECTED;





 case "DUPLICATE" -> MessageKeys.VERDICT_DUPLICATE;





 case "INSUFFICIENT" -> MessageKeys.VERDICT_INSUFFICIENT;





 case "AUTO_CLOSED" -> MessageKeys.VERDICT_AUTO_CLOSED;





 default -> MessageKeys.GENERAL_NONE;



 };

 }


private void runSync(Runnable action) {



 if (Bukkit.isPrimaryThread()) {





 action.run();



 } else {





 Bukkit.getScheduler().runTask(plugin, action);



 }

 } }
