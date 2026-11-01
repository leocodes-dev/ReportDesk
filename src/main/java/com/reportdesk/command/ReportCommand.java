/*
 * ReportDesk - Enterprise-grade player reporting and evidence system.
 * Copyright (C) 2026 William
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.reportdesk.command;
import com.reportdesk.ReportDeskPlugin; import com.reportdesk.core.Page; import com.reportdesk.core.Report; import com.reportdesk.core.ReportCategory; import com.reportdesk.core.ReportReply; import com.reportdesk.core.ReportSubmissionService; import com.reportdesk.i18n.MessageKeys; import com.reportdesk.service.ReportService; import org.bukkit.Bukkit; import org.bukkit.command.Command; import org.bukkit.command.CommandExecutor; import org.bukkit.command.CommandSender; import org.bukkit.command.TabCompleter; import org.bukkit.entity.Player;
import java.util.ArrayList; import java.util.Arrays; import java.util.LinkedHashMap; import java.util.List; import java.util.Locale; import java.util.Map; import java.util.Optional; import java.util.UUID; import java.util.concurrent.ConcurrentHashMap;
/**
* Player-facing /report command. It opens the GUI flow or submits directly and
* delegates every anti-abuse decision to the pure submission service.
*/ public final class ReportCommand implements CommandExecutor, TabCompleter {


private final ReportDeskPlugin plugin;

 private final Map<UUID, Long> lastUse = new ConcurrentHashMap<>();

 private final Map<UUID, Long> lastReply = new ConcurrentHashMap<>();


public ReportCommand(ReportDeskPlugin plugin) {



 this.plugin = plugin;

 }


@Override

 public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {



 if (!(sender instanceof Player player)) {





 sender.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_PLAYER_ONLY, Map.of()));





 return true;



 }



 if (!player.hasPermission("reportdesk.report")) {





 player.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_NO_PERMISSION, Map.of()));





 return true;



 }




if (args.length == 0) {





 if (!allowUse(player)) {







 return true;





 }





 plugin.gui().beginReport(player);





 return true;



 }




String first = args[0].toLowerCase(Locale.ROOT);



 switch (first) {





 case "help", "?" -> {







 sendHelp(player);







 return true;





 }





 case "status", "list" -> {







 showOwnReports(player, args);







 return true;





 }





 case "view", "info" -> {







 showOwnReport(player, args);







 return true;





 }





 case "reply" -> {







 reply(player, args);







 return true;





 }





 case "replies", "thread" -> {







 showReplies(player, args);







 return true;





 }





 case "cancel" -> {







 plugin.chatInput().cancel(player, true);







 plugin.gui().forget(player.getUniqueId());







 return true;





 }





 default -> {







 // Fall through to a target name.





 }



 }




if (!allowUse(player)) {





 return true;



 }




String target = args[0].trim();



 var targetCheck = plugin.validator().validatePlayerName(target);



 if (!targetCheck.valid()) {





 player.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_INVALID_INPUT,







 Map.of("reason", reasonName(targetCheck.reason()))));





 return true;



 }




if (args.length == 1) {





 if (plugin.config().gui().enabled()) {







 plugin.gui().beginReport(player, target);





 } else {







 submit(player, target, plugin.config().defaultCategory(), "");





 }





 return true;



 }




Optional<ReportCategory> category = plugin.config().category(args[1]);



 if (category.isPresent()) {





 submit(player, target, category.get().id(), join(args, 2));



 } else {





 submit(player, target, plugin.config().defaultCategory(), join(args, 1));



 }



 return true;

 }


private void submit(Player player, String target, String category, String detail) {



 boolean bypass = player.hasPermission(plugin.config().limits().bypassPermission());



 var result = plugin.reportService().submit(player, target, category, detail, bypass);



 String key = ReportService.messageKey(result.reason());



 Map<String, String> placeholders = new LinkedHashMap<>(result.placeholders());



 placeholders.put("player", target);



 placeholders.put("target", target);



 placeholders.put("category", plugin.categoryDisplay(category));



 if (!result.success()) {





 placeholders.putIfAbsent("reason", reasonNameFor(result.reason()));



 }



 player.sendMessage(plugin.messages().prefixed(key, placeholders));

 }


private void showOwnReport(Player player, String[] args) {



 if (!plugin.repository().isReady()) {





 player.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_LOADING, Map.of()));





 return;



 }



 if (args.length < 2) {





 player.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_USAGE,







 Map.of("usage", "/report view <id>")));





 return;



 }



 Long id = parseLong(args[1]);



 if (id == null || id <= 0) {





 player.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_INVALID_ID, Map.of()));





 return;



 }



 Optional<Report> report = plugin.repository().get(id);



 if (report.isEmpty() || !player.getUniqueId().equals(report.get().reporterId())) {





 player.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_REPORT_NOT_FOUND,







 Map.of("id", String.valueOf(id))));





 return;



 }



 plugin.view().sendOwnInfo(player, report.get());



 plugin.reportService().markReporterRead(report.get());

 }


private void reply(Player player, String[] args) {



 if (!plugin.repository().isReady()) {





 player.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_LOADING, Map.of()));





 return;



 }



 if (args.length < 3) {





 player.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_USAGE,







 Map.of("usage", "/report reply <id> <message...>")));





 return;



 }



 if (!allowReply(player)) {





 return;



 }



 Report report = ownReport(player, args[1]);



 if (report == null) {





 return;



 }



 var result = plugin.reportService().addReply(report, player, join(args, 2), false);



 player.sendMessage(plugin.messages().prefixed(result.messageKey(), result.placeholders()));

 }


private void showReplies(Player player, String[] args) {



 if (!plugin.repository().isReady()) {





 player.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_LOADING, Map.of()));





 return;



 }



 if (args.length < 2) {





 player.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_USAGE,







 Map.of("usage", "/report replies <id> [page]")));





 return;



 }



 Report report = ownReport(player, args[1]);



 if (report == null) {





 return;



 }



 int page = 1;



 if (args.length >= 3) {





 try {







 page = Math.max(1, Integer.parseInt(args[2]));





 } catch (NumberFormatException ignored) {







 page = 1;





 }



 }



 Page<ReportReply> result = Page.of(report.replies(), page,





 plugin.config().reports().textPageSize());



 plugin.view().sendReplies(player, report, result, false);



 plugin.reportService().markReporterRead(report);

 }


private Report ownReport(Player player, String rawId) {



 Long id = parseLong(rawId);



 if (id == null || id <= 0) {





 player.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_INVALID_ID, Map.of()));





 return null;



 }



 Optional<Report> report = plugin.repository().get(id);



 if (report.isEmpty() || !player.getUniqueId().equals(report.get().reporterId())) {





 player.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_REPORT_NOT_FOUND,







 Map.of("id", String.valueOf(id))));





 return null;



 }



 return report.get();

 }


private void showOwnReports(Player player, String[] args) {



 if (!plugin.repository().isReady()) {





 player.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_LOADING, Map.of()));





 return;



 }



 int page = 1;



 if (args.length >= 2) {





 try {







 page = Math.max(1, Integer.parseInt(args[1]));





 } catch (NumberFormatException ignored) {







 page = 1;





 }



 }



 List<Report> reports = plugin.repository().byReporter(player.getUniqueId());



 Page<Report> result = Page.of(reports, page, plugin.config().reports().textPageSize());



 plugin.view().sendOwnReports(player, result);

 }


private void sendHelp(Player player) {



 player.sendMessage(plugin.messages().prefixed(MessageKeys.REPORT_HELP_HEADER, Map.of()));



 for (String key : List.of(MessageKeys.REPORT_HELP_LINE_REPORT, MessageKeys.REPORT_HELP_LINE_TARGET,





 MessageKeys.REPORT_HELP_LINE_DIRECT, MessageKeys.REPORT_HELP_LINE_STATUS,





 MessageKeys.REPORT_HELP_LINE_VIEW, MessageKeys.REPORT_HELP_LINE_REPLY,





 MessageKeys.REPORT_HELP_LINE_REPLIES, MessageKeys.REPORT_HELP_LINE_CANCEL)) {





 player.sendMessage(plugin.messages().component(key, Map.of()));



 }

 }


private boolean allowUse(Player player) {



 long cooldown = plugin.config().limits().commandCooldownMillis();



 if (cooldown <= 0 || player.hasPermission(plugin.config().limits().bypassPermission())) {





 lastUse.put(player.getUniqueId(), System.currentTimeMillis());





 return true;



 }



 long now = System.currentTimeMillis();



 Long last = lastUse.get(player.getUniqueId());



 if (last != null && now - last < cooldown) {





 player.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_ACTION_COOLDOWN, Map.of()));





 return false;



 }



 lastUse.put(player.getUniqueId(), now);



 return true;

 }


/**


* Reuses the configured command cooldown so players cannot flood a report


* thread with replies. The counter is separate from /report submission so a


* reply does not block the next report and vice versa.


*/

 private boolean allowReply(Player player) {



 long cooldown = plugin.config().limits().commandCooldownMillis();



 if (cooldown <= 0 || player.hasPermission(plugin.config().limits().bypassPermission())) {





 lastReply.put(player.getUniqueId(), System.currentTimeMillis());





 return true;



 }



 long now = System.currentTimeMillis();



 Long last = lastReply.get(player.getUniqueId());



 if (last != null && now - last < cooldown) {





 player.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_ACTION_COOLDOWN, Map.of()));





 return false;



 }



 lastReply.put(player.getUniqueId(), now);



 return true;

 }


private static String join(String[] args, int start) {



 if (start >= args.length) {





 return "";



 }



 return String.join(" ", Arrays.copyOfRange(args, start, args.length)).trim();

 }


private static Long parseLong(String value) {



 try {





 return Long.parseLong(value.trim());



 } catch (RuntimeException exception) {





 return null;



 }

 }


private static String reasonNameFor(ReportSubmissionService.SubmitReason reason) {



 if (reason == null) {





 return "invalid";



 }



 return switch (reason) {





 case DETAIL_ILLEGAL -> "illegal_characters";





 case DETAIL_EMPTY -> "empty";





 case DETAIL_TOO_SHORT -> "too_short";





 case DETAIL_TOO_LONG -> "too_long";





 default -> reason.name().toLowerCase(Locale.ROOT);



 };

 }


private static String reasonName(com.reportdesk.core.InputValidator.Reason reason) {



 return reason == null ? "invalid" : reason.name().toLowerCase(Locale.ROOT);

 }


@Override

 public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {



 if (!(sender instanceof Player player) || !player.hasPermission("reportdesk.report")) {





 return List.of();



 }



 if (args.length == 1) {





 List<String> suggestions = new ArrayList<>(List.of("help", "status", "cancel", "view", "reply", "replies"));





 for (Player online : Bukkit.getOnlinePlayers()) {







 if (!online.getUniqueId().equals(player.getUniqueId())) {









 suggestions.add(online.getName());







 }





 }





 return filter(suggestions, args[0]);



 }



 if (args.length == 2) {





 String first = args[0].toLowerCase(Locale.ROOT);





 if (first.equals("help") || first.equals("status") || first.equals("cancel")) {







 return List.of();





 }





 if (first.equals("view") || first.equals("info") || first.equals("reply")







 || first.equals("replies") || first.equals("thread")) {







 return filter(ownReportIds(player), args[1]);





 }





 return filter(plugin.config().categoryRegistry().commandSuggestions(), args[1]);



 }



 return List.of();

 }


private List<String> ownReportIds(Player player) {



 List<String> ids = new ArrayList<>();



 for (Report report : plugin.repository().byReporter(player.getUniqueId())) {





 ids.add(String.valueOf(report.id()));





 if (ids.size() >= 50) {







 break;





 }



 }



 return ids;

 }


private static List<String> filter(List<String> values, String prefix) {



 String lower = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);



 List<String> result = new ArrayList<>();



 for (String value : values) {





 if (value != null && value.toLowerCase(Locale.ROOT).startsWith(lower)) {







 result.add(value);





 }



 }



 return result;

 } }
