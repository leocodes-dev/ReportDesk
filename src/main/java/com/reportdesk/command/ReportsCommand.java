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
import com.reportdesk.ReportDeskPlugin; import com.reportdesk.config.ConfigKey; import com.reportdesk.config.ConfigKeys; import com.reportdesk.core.Page; import com.reportdesk.core.Report; import com.reportdesk.core.ReportFilter; import com.reportdesk.core.ReportReply; import com.reportdesk.core.ReportStatus; import com.reportdesk.core.Verdict; import com.reportdesk.core.WatchEntry; import com.reportdesk.i18n.MessageKeys; import com.reportdesk.service.ConfigEditService; import com.reportdesk.service.ReportService; import com.reportdesk.util.TimeText; import org.bukkit.Bukkit; import org.bukkit.command.Command; import org.bukkit.command.CommandExecutor; import org.bukkit.command.CommandSender; import org.bukkit.command.TabCompleter; import org.bukkit.entity.Player;
import java.util.ArrayList; import java.util.Arrays; import java.util.LinkedHashMap; import java.util.List; import java.util.Locale; import java.util.Map; import java.util.Optional; import java.util.UUID; import java.util.concurrent.CompletableFuture; import java.util.concurrent.ConcurrentHashMap;
/**
* Admin /reports command. Every branch checks a permission, parses/validates
* input, and delegates persistence to asynchronous services.
*/ public final class ReportsCommand implements CommandExecutor, TabCompleter {


private static final List<String> SUBCOMMANDS = List.of(



 "help", "menu", "list", "search", "view", "claim", "release", "close", "reopen", "tp",



 "evidence", "note", "reply", "replies", "delete", "purge", "overdue", "stats",



 "export", "audit", "reload", "config", "watch", "unwatch", "watching"

 );


private final ReportDeskPlugin plugin;

 private final Map<UUID, Long> lastUse = new ConcurrentHashMap<>();


public ReportsCommand(ReportDeskPlugin plugin) {



 this.plugin = plugin;

 }


@Override

 public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {



 if (!sender.hasPermission("reportdesk.admin")) {





 sender.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_NO_PERMISSION, Map.of()));





 return true;



 }



 if (args.length == 0) {





 if (sender instanceof Player player







 && plugin.config().gui().enabled()







 && plugin.config().gui().admin().enabled()) {







 plugin.gui().adminGui().openHub(player);





 } else {







 sendHelp(sender);





 }





 return true;



 }



 if (args[0].equalsIgnoreCase("help")) {





 sendHelp(sender);





 return true;



 }




String sub = args[0].toLowerCase(Locale.ROOT);



 switch (sub) {





 case "menu", "gui", "admin" -> menu(sender);





 case "list" -> list(sender, args);





 case "search" -> search(sender, args);





 case "view", "info" -> view(sender, args);





 case "claim" -> claim(sender, args);





 case "release" -> release(sender, args);





 case "close" -> close(sender, args);





 case "reopen" -> reopen(sender, args);





 case "tp" -> teleport(sender, args);





 case "evidence" -> evidence(sender, args);





 case "note" -> note(sender, args);





 case "reply" -> reply(sender, args);





 case "replies", "thread" -> replies(sender, args);





 case "delete" -> delete(sender, args);





 case "purge" -> purge(sender, args);





 case "overdue" -> overdue(sender, args);





 case "watch" -> watch(sender, args);





 case "unwatch" -> unwatch(sender, args);





 case "watching" -> watching(sender, args);





 case "stats" -> plugin.view().sendStats(sender);





 case "export" -> export(sender);





 case "audit" -> audit(sender, args);





 case "reload" -> reload(sender);





 case "config" -> config(sender, args);





 default -> sender.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_UNKNOWN_COMMAND,







 Map.of("command", label)));



 }



 return true;

 }


private void menu(CommandSender sender) {



 if (!(sender instanceof Player player)) {





 sender.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_PLAYER_ONLY, Map.of()));





 return;



 }



 if (!plugin.config().gui().enabled()) {





 sender.sendMessage(plugin.messages().prefixed(MessageKeys.REPORT_GUI_DISABLED, Map.of()));





 return;



 }



 if (!plugin.config().gui().admin().enabled()) {





 sender.sendMessage(plugin.messages().prefixed(MessageKeys.ADMIN_GUI_DISABLED, Map.of()));





 return;



 }



 plugin.gui().adminGui().openHub(player);

 }


private void list(CommandSender sender, String[] args) {



 if (!plugin.repository().isReady()) {





 sender.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_LOADING, Map.of()));





 return;



 }



 ReportFilter filter = ReportFilter.all();



 int page = 1;



 for (int index = 1; index < args.length; index++) {





 String value = args[index];





 String lower = value.toLowerCase(Locale.ROOT);





 Integer number = parseInteger(value);





 if (number != null) {







 page = Math.max(1, number);







 continue;





 }





 if ("all".equals(lower)) {







 filter = filter.withStatus(null);







 continue;





 }





 if ("mine".equals(lower)) {







 Player player = actor(sender);







 if (player == null) {









 sender.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_PLAYER_ONLY, Map.of()));









 return;







 }







 filter = filter.withClaimedBy(player.getUniqueId());







 continue;





 }





 if ("oldest".equals(lower) || "newest".equals(lower)) {







 filter = filter.withSort("oldest".equals(lower)









 ? ReportFilter.SortOrder.OLDEST : ReportFilter.SortOrder.NEWEST);







 continue;





 }





 if ("priority".equals(lower)) {







 filter = filter.withSort(ReportFilter.SortOrder.PRIORITY);







 continue;





 }





 if ("overdue".equals(lower)) {







 filter = filter.withOverdueOnly(true);







 continue;





 }





 if (lower.startsWith("priority:") || lower.startsWith("prio:")) {







 String rawPriority = value.substring(value.indexOf(':') + 1);







 com.reportdesk.core.ReportPriority parsed =









 com.reportdesk.core.ReportPriority.parse(rawPriority, null);







 if (parsed == null) {









 sender.sendMessage(plugin.messages().prefixed(MessageKeys.ADMIN_LIST_UNKNOWN_FILTER,











 Map.of("value", value)));









 return;







 }







 filter = filter.withPriority(parsed);







 continue;





 }





 if (lower.startsWith("cat:") || lower.startsWith("category:")) {







 String rawCategory = value.substring(value.indexOf(':') + 1);







 var category = plugin.config().category(rawCategory);







 if (category.isEmpty()) {









 sender.sendMessage(plugin.messages().prefixed(MessageKeys.ADMIN_LIST_UNKNOWN_FILTER,











 Map.of("value", value)));









 return;







 }







 filter = filter.withCategory(category.get().id());







 continue;





 }





 ReportStatus status = ReportStatus.parse(value, null);





 if (status != null) {







 filter = filter.withStatus(status);







 continue;





 }





 sender.sendMessage(plugin.messages().prefixed(MessageKeys.ADMIN_LIST_UNKNOWN_FILTER,







 Map.of("value", value)));





 return;



 }



 Page<Report> result = plugin.repository().page(filter, page, plugin.config().reports().textPageSize());



 plugin.view().sendList(sender, result, filter);

 }


private void search(CommandSender sender, String[] args) {



 if (!plugin.repository().isReady()) {





 sender.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_LOADING, Map.of()));





 return;



 }



 if (args.length < 2) {





 usage(sender, "/reports search <keyword...>");





 return;



 }



 String query = join(args, 1);



 var validation = plugin.validator().validateQuery(query);



 if (!validation.valid()) {





 sender.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_INVALID_INPUT,







 Map.of("reason", validation.reason().name().toLowerCase(Locale.ROOT))));





 return;



 }



 ReportFilter filter = ReportFilter.all().withQuery(query);



 Page<Report> result = plugin.repository().page(filter, 1,





 plugin.config().reports().textPageSize());



 plugin.view().sendList(sender, result, filter);

 }


private void view(CommandSender sender, String[] args) {



 Report report = report(sender, args, 1);



 if (report == null) {





 return;



 }



 if (sender instanceof Player player && plugin.config().gui().enabled()) {





 plugin.gui().openDetail(player, report);



 } else {





 plugin.view().sendInfo(sender, report);



 }

 }


private void claim(CommandSender sender, String[] args) {



 Report report = report(sender, args, 1);



 if (report == null || !allowAction(sender) || !require(sender, "reportdesk.admin")) {





 return;



 }



 Player staff = actor(sender);



 if (staff == null) {





 sender.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_PLAYER_ONLY, Map.of()));





 return;



 }



 sendResult(sender, plugin.reportService().claim(report, staff));

 }


private void release(CommandSender sender, String[] args) {



 Report report = report(sender, args, 1);



 if (report == null || !allowAction(sender) || !require(sender, "reportdesk.admin")) {





 return;



 }



 Player staff = actor(sender);



 if (staff == null) {





 sender.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_PLAYER_ONLY, Map.of()));





 return;



 }



 sendResult(sender, plugin.reportService().release(report, staff));

 }


private void close(CommandSender sender, String[] args) {



 if (args.length < 3) {





 usage(sender, "/reports close <id> <verdict> [note...]");





 return;



 }



 Report report = report(sender, args, 1);



 if (report == null || !allowAction(sender) || !require(sender, "reportdesk.admin.close")) {





 return;



 }



 Verdict verdict = Verdict.parse(args[2]);



 if (verdict == null || verdict == Verdict.AUTO_CLOSED) {





 sender.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_INVALID_VERDICT, Map.of()));





 return;



 }



 Player staff = actor(sender);



 if (staff == null) {





 sender.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_PLAYER_ONLY, Map.of()));





 return;



 }



 sendResult(sender, plugin.reportService().close(report, verdict, staff, join(args, 3)));

 }


private void reopen(CommandSender sender, String[] args) {



 Report report = report(sender, args, 1);



 if (report == null || !allowAction(sender) || !require(sender, "reportdesk.admin.close")) {





 return;



 }



 Player staff = actor(sender);



 if (staff == null) {





 sender.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_PLAYER_ONLY, Map.of()));





 return;



 }



 sendResult(sender, plugin.reportService().reopen(report, staff));

 }


private void teleport(CommandSender sender, String[] args) {



 Report report = report(sender, args, 1);



 if (report == null || !allowAction(sender) || !require(sender, "reportdesk.admin")) {





 return;



 }



 Player player = actor(sender);



 if (player == null) {





 sender.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_PLAYER_ONLY, Map.of()));





 return;



 }



 Player target = Bukkit.getPlayerExact(report.targetName());



 if (target == null) {





 player.sendMessage(plugin.messages().prefixed(MessageKeys.ADMIN_TARGET_NOT_ONLINE, Map.of()));





 return;



 }



 player.teleport(target.getLocation());



 player.sendMessage(plugin.messages().prefixed(MessageKeys.ADMIN_TELEPORTED,





 Map.of("target", target.getName())));



 plugin.audit("TELEPORT", player, report, target.getName());

 }


private void evidence(CommandSender sender, String[] args) {



 Report report = report(sender, args, 1);



 if (report == null || !allowAction(sender) || !require(sender, "reportdesk.evidence")) {





 return;



 }



 if (!plugin.config().evidence().enabled()) {





 sender.sendMessage(plugin.messages().prefixed(MessageKeys.ADMIN_EVIDENCE_DISABLED, Map.of()));





 return;



 }



 if (!report.evidence().present()) {





 sender.sendMessage(plugin.messages().prefixed(MessageKeys.ADMIN_EVIDENCE_EMPTY,







 Map.of("id", String.valueOf(report.id()))));





 return;



 }



 plugin.audit("VIEW_EVIDENCE", actor(sender), report, "");



 sender.sendMessage(plugin.messages().prefixed(MessageKeys.GUI_EVIDENCE_LOADING, Map.of()));



 plugin.evidence().load(report.id()).whenComplete((loaded, throwable) -> runOnMain(() -> {





 if (throwable != null) {







 plugin.getLogger().warning("Failed to load evidence #" + report.id() + ": "









 + throwable.getMessage());







 sender.sendMessage(plugin.messages().prefixed(MessageKeys.ADMIN_EVIDENCE_EMPTY,









 Map.of("id", String.valueOf(report.id()))));







 return;





 }





 if (loaded.isEmpty()) {







 sender.sendMessage(plugin.messages().prefixed(MessageKeys.ADMIN_EVIDENCE_EMPTY,









 Map.of("id", String.valueOf(report.id()))));







 return;





 }





 plugin.view().sendEvidence(sender, report, loaded.get());



 }));

 }


private void note(CommandSender sender, String[] args) {



 if (args.length < 3) {





 usage(sender, "/reports note <id> <text...>");





 return;



 }



 Report report = report(sender, args, 1);



 if (report == null || !allowAction(sender) || !require(sender, "reportdesk.admin")) {





 return;



 }



 Player staff = actor(sender);



 if (staff == null) {





 sender.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_PLAYER_ONLY, Map.of()));





 return;



 }



 sendResult(sender, plugin.reportService().addNote(report, staff, join(args, 2)));

 }


private void reply(CommandSender sender, String[] args) {



 if (args.length < 3) {





 usage(sender, "/reports reply <id> <message...>");





 return;



 }



 Report report = report(sender, args, 1);



 if (report == null || !allowAction(sender) || !require(sender, "reportdesk.admin")) {





 return;



 }



 Player staff = actor(sender);



 if (staff == null) {





 sender.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_PLAYER_ONLY, Map.of()));





 return;



 }



 sendResult(sender, plugin.reportService().addReply(report, staff, join(args, 2), true));

 }


private void replies(CommandSender sender, String[] args) {



 if (!plugin.repository().isReady()) {





 sender.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_LOADING, Map.of()));





 return;



 }



 if (args.length < 2) {





 usage(sender, "/reports replies <id> [page]");





 return;



 }



 Report report = report(sender, args, 1);



 if (report == null || !allowAction(sender)) {





 return;



 }



 int page = 1;



 if (args.length >= 3) {





 Integer parsed = parseInteger(args[2]);





 if (parsed == null || parsed <= 0) {







 sender.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_INVALID_INPUT,









 Map.of("reason", "invalid_page")));







 return;





 }





 page = parsed;



 }



 Page<ReportReply> result = Page.of(report.replies(), page,





 plugin.config().reports().textPageSize());



 plugin.view().sendReplies(sender, report, result, true);

 }


private void overdue(CommandSender sender, String[] args) {



 if (!plugin.repository().isReady()) {





 sender.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_LOADING, Map.of()));





 return;



 }



 int page = 1;



 if (args.length >= 2) {





 Integer parsed = parseInteger(args[1]);





 if (parsed == null || parsed <= 0) {







 sender.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_INVALID_INPUT,









 Map.of("reason", "invalid_page")));







 return;





 }





 page = parsed;



 }



 ReportFilter filter = ReportFilter.all()





 .withOverdueOnly(true)





 .withSort(ReportFilter.SortOrder.PRIORITY);



 Page<Report> result = plugin.repository().page(filter, page,





 plugin.config().reports().textPageSize());



 plugin.view().sendList(sender, result, filter);

 }


private void watch(CommandSender sender, String[] args) {



 if (args.length < 2) {





 usage(sender, "/reports watch <player>");





 return;



 }



 if (!require(sender, "reportdesk.watch") || !allowAction(sender)) {





 return;



 }



 Player staff = actor(sender);



 if (staff == null) {





 sender.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_PLAYER_ONLY, Map.of()));





 return;



 }



 String target = args[1];



 var validation = plugin.validator().validatePlayerName(target);



 if (!validation.valid()) {





 sender.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_INVALID_INPUT,







 Map.of("reason", validation.reason().name().toLowerCase(Locale.ROOT))));





 return;



 }



 var result = plugin.watchlist().toggle(staff, target);



 String key = switch (result) {





 case ADDED -> MessageKeys.ADMIN_WATCH_ADDED;





 case REMOVED -> MessageKeys.ADMIN_WATCH_REMOVED;





 case LIMIT_REACHED -> MessageKeys.ADMIN_WATCH_LIMIT;





 case DISABLED -> MessageKeys.ADMIN_WATCH_DISABLED;





 case INVALID, NOT_WATCHED -> MessageKeys.GENERAL_INVALID_INPUT;



 };



 Map<String, String> placeholders = new LinkedHashMap<>();



 placeholders.put("target", target);



 placeholders.put("limit",





 String.valueOf(plugin.config().notifications().watchlistMaxTargets()));



 if (result == com.reportdesk.service.WatchlistService.ToggleResult.INVALID) {





 placeholders.put("reason", "invalid_player_name");



 }



 sender.sendMessage(plugin.messages().prefixed(key, placeholders));



 if (result == com.reportdesk.service.WatchlistService.ToggleResult.ADDED





 || result == com.reportdesk.service.WatchlistService.ToggleResult.REMOVED) {





 plugin.audit(result == com.reportdesk.service.WatchlistService.ToggleResult.ADDED







 ? "WATCH" : "UNWATCH", staff, 0L, target);



 }

 }


private void unwatch(CommandSender sender, String[] args) {



 if (args.length < 2) {





 usage(sender, "/reports unwatch <player>");





 return;



 }



 if (!require(sender, "reportdesk.watch") || !allowAction(sender)) {





 return;



 }



 Player staff = actor(sender);



 if (staff == null) {





 sender.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_PLAYER_ONLY, Map.of()));





 return;



 }



 String target = args[1];



 var validation = plugin.validator().validatePlayerName(target);



 if (!validation.valid()) {





 sender.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_INVALID_INPUT,







 Map.of("reason", validation.reason().name().toLowerCase(Locale.ROOT))));





 return;



 }



 var result = plugin.watchlist().remove(staff, target);



 String key = switch (result) {





 case REMOVED -> MessageKeys.ADMIN_WATCH_REMOVED;





 case NOT_WATCHED -> MessageKeys.ADMIN_WATCH_NOT_FOUND;





 case DISABLED -> MessageKeys.ADMIN_WATCH_DISABLED;





 case ADDED, LIMIT_REACHED, INVALID -> MessageKeys.GENERAL_INVALID_INPUT;



 };



 Map<String, String> placeholders = new LinkedHashMap<>();



 placeholders.put("target", target);



 if (result == com.reportdesk.service.WatchlistService.ToggleResult.INVALID) {





 placeholders.put("reason", "invalid_player_name");



 }



 sender.sendMessage(plugin.messages().prefixed(key, placeholders));



 if (result == com.reportdesk.service.WatchlistService.ToggleResult.REMOVED) {





 plugin.audit("UNWATCH", staff, 0L, target);



 }

 }


private void watching(CommandSender sender, String[] args) {



 if (!require(sender, "reportdesk.watch")) {





 return;



 }



 Player staff = actor(sender);



 if (staff == null) {





 sender.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_PLAYER_ONLY, Map.of()));





 return;



 }



 int pageNumber = 1;



 if (args.length >= 2) {





 Integer parsed = parseInteger(args[1]);





 if (parsed == null || parsed <= 0) {







 sender.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_INVALID_INPUT,









 Map.of("reason", "invalid_page")));







 return;





 }





 pageNumber = parsed;



 }



 List<WatchEntry> entries = plugin.watchlist().watchedBy(staff.getUniqueId());



 Page<WatchEntry> page = Page.of(entries, pageNumber,





 plugin.config().reports().textPageSize());



 sender.sendMessage(plugin.messages().component(MessageKeys.ADMIN_WATCHING_HEADER,





 Map.of("count", String.valueOf(entries.size()))));



 if (page.items().isEmpty()) {





 sender.sendMessage(plugin.messages().component(MessageKeys.ADMIN_WATCHING_EMPTY, Map.of()));





 return;



 }



 for (WatchEntry entry : page.items()) {





 sender.sendMessage(plugin.messages().component(MessageKeys.ADMIN_WATCHING_LINE, Map.of(







 "target", entry.targetName(),







 "created", TimeText.absolute(entry.createdAt(), plugin.messages())





 )));



 }



 if (page.totalPages() > 1) {





 sender.sendMessage(plugin.messages().component(MessageKeys.GENERAL_PAGE, Map.of(







 "page", String.valueOf(page.page()),







 "pages", String.valueOf(page.totalPages())





 )));



 }

 }


private void delete(CommandSender sender, String[] args) {



 Report report = report(sender, args, 1);



 if (report == null || !allowAction(sender) || !require(sender, "reportdesk.admin.delete")) {





 return;



 }



 Player staff = actor(sender);



 if (staff == null) {





 sender.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_PLAYER_ONLY, Map.of()));





 return;



 }



 sendResult(sender, plugin.reportService().delete(report, staff));

 }


private void purge(CommandSender sender, String[] args) {



 if (!require(sender, "reportdesk.admin.delete")) {





 return;



 }



 int days = plugin.config().reports().retentionDays();



 if (args.length >= 2) {





 Integer parsed = parseInteger(args[1]);





 if (parsed == null || parsed <= 0) {







 sender.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_INVALID_INPUT,









 Map.of("reason", "invalid_days")));







 return;





 }





 days = parsed;



 }



 if (days <= 0) {





 usage(sender, "/reports purge <days>");





 return;



 }



 if (!allowAction(sender)) {





 return;



 }



 int finalDays = days;



 plugin.maintenance().purgeOlderThan(finalDays).whenComplete((result, throwable) -> runOnMain(() -> {





 if (throwable != null) {







 plugin.getLogger().warning("Purge failed: " + throwable.getMessage());







 sender.sendMessage(plugin.messages().prefixed(MessageKeys.ADMIN_EXPORT_FAILED, Map.of()));







 return;





 }





 if (result == null || !result.changed()) {







 sender.sendMessage(plugin.messages().prefixed(MessageKeys.ADMIN_PURGE_NONE, Map.of()));







 return;





 }





 sender.sendMessage(plugin.messages().prefixed(MessageKeys.ADMIN_PURGED, Map.of(







 "reports", String.valueOf(result.purgedReports()),







 "evidence", String.valueOf(result.purgedEvidence()),







 "audit", String.valueOf(result.purgedAudit())





 )));



 }));

 }


private void export(CommandSender sender) {



 if (!allowAction(sender)) {





 return;



 }



 sender.sendMessage(plugin.messages().prefixed(MessageKeys.ADMIN_EXPORT_STARTED, Map.of()));



 plugin.storage().exportCsv(plugin.repository().all()).whenComplete((path, throwable) -> runOnMain(() -> {





 if (throwable != null) {







 plugin.getLogger().warning("Export failed: " + throwable.getMessage());







 sender.sendMessage(plugin.messages().prefixed(MessageKeys.ADMIN_EXPORT_FAILED, Map.of()));







 return;





 }





 String display;





 try {







 display = plugin.getDataFolder().toPath().relativize(path).toString();





 } catch (IllegalArgumentException exception) {







 display = path.getFileName().toString();





 }





 sender.sendMessage(plugin.messages().prefixed(MessageKeys.ADMIN_EXPORT_DONE,







 Map.of("file", display)));





 plugin.audit("EXPORT", actor(sender), 0L, display);



 }));

 }


private void audit(CommandSender sender, String[] args) {



 if (!allowAction(sender)) {





 return;



 }



 int page = 1;



 if (args.length >= 2) {





 Integer parsed = parseInteger(args[1]);





 if (parsed == null || parsed <= 0) {







 sender.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_INVALID_INPUT,









 Map.of("reason", "invalid_page")));







 return;





 }





 page = parsed;



 }



 int requested = page;



 plugin.auditStorage().readPage(requested, plugin.config().audit().pageSize())





 .whenComplete((lines, throwable) -> runOnMain(() -> {







 if (throwable != null) {









 plugin.getLogger().warning("Audit read failed: " + throwable.getMessage());









 sender.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_UNKNOWN_ERROR, Map.of()));









 return;







 }







 plugin.view().sendAudit(sender, requested, lines);





 }));

 }


private void reload(CommandSender sender) {



 if (!require(sender, "reportdesk.reload") || !allowAction(sender)) {





 return;



 }



 try {





 plugin.reloadAll();





 sender.sendMessage(plugin.messages().prefixed(MessageKeys.ADMIN_RELOADED, Map.of()));





 plugin.audit("RELOAD", actor(sender), 0L, "");



 } catch (RuntimeException exception) {





 plugin.getLogger().warning("Reload failed: " + exception.getMessage());





 sender.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_RELOAD_FAILED, Map.of()));



 }

 }


private void config(CommandSender sender, String[] args) {



 if (!require(sender, "reportdesk.config")) {





 return;



 }



 if (args.length < 2) {





 usage(sender, "/reports config <list|get|set|reset> [key] [value...]");





 return;



 }



 switch (args[1].toLowerCase(Locale.ROOT)) {





 case "list" -> configList(sender, args);





 case "get", "info" -> configGet(sender, args);





 case "set" -> configSet(sender, args, false);





 case "reset" -> configSet(sender, args, true);





 default -> usage(sender, "/reports config <list|get|set|reset> [key] [value...]");



 }

 }


private void configList(CommandSender sender, String[] args) {



 String query = args.length >= 3 ? args[2].toLowerCase(Locale.ROOT) : "";



 int pageNumber = 1;



 if (args.length >= 4) {





 Integer parsed = parseInteger(args[3]);





 if (parsed == null || parsed <= 0) {







 sender.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_INVALID_INPUT,









 Map.of("reason", "invalid_page")));







 return;





 }





 pageNumber = parsed;



 }



 List<ConfigKey> keys = new ArrayList<>();



 for (ConfigKey key : ConfigKeys.all()) {





 if (query.isEmpty() || key.path().contains(query)) {







 keys.add(key);





 }



 }



 Page<ConfigKey> page = Page.of(keys, pageNumber,





 plugin.config().reports().textPageSize());



 sender.sendMessage(plugin.messages().prefixed(MessageKeys.CONFIG_LIST_HEADER, Map.of(





 "count", String.valueOf(keys.size()),





 "page", String.valueOf(page.page()),





 "pages", String.valueOf(page.totalPages())



 )));



 if (page.items().isEmpty()) {





 sender.sendMessage(plugin.messages().prefixed(MessageKeys.CONFIG_LIST_EMPTY, Map.of()));





 return;



 }



 for (ConfigKey key : page.items()) {





 sender.sendMessage(plugin.messages().prefixed(MessageKeys.CONFIG_LIST_LINE, Map.of(







 "key", key.path(),







 "value", key.display(key.current(plugin.config()))





 )));



 }



 if (page.totalPages() > 1) {





 sender.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_PAGE, Map.of(







 "page", String.valueOf(page.page()),







 "pages", String.valueOf(page.totalPages())





 )));



 }

 }


private void configGet(CommandSender sender, String[] args) {



 if (args.length < 3) {





 usage(sender, "/reports config get <key>");





 return;



 }



 Optional<ConfigKey> found = ConfigKeys.find(args[2]);



 if (found.isEmpty()) {





 sender.sendMessage(plugin.messages().prefixed(MessageKeys.CONFIG_UNKNOWN,







 Map.of("key", args[2])));





 return;



 }



 ConfigKey key = found.get();



 sender.sendMessage(plugin.messages().prefixed(MessageKeys.CONFIG_GET, Map.of(





 "key", key.path(),





 "value", key.display(key.current(plugin.config())),





 "type", configType(key)



 )));



 sender.sendMessage(plugin.messages().prefixed(MessageKeys.CONFIG_DEFAULT, Map.of(





 "default", key.display(key.current(plugin.configEditor().defaults()))



 )));



 if (key.restartRequired()) {





 sender.sendMessage(plugin.messages().prefixed(MessageKeys.CONFIG_RESTART, Map.of()));



 }

 }


private void configSet(CommandSender sender, String[] args, boolean reset) {



 if (args.length < 3) {





 usage(sender, reset







 ? "/reports config reset <key>"







 : "/reports config set <key> <value...>");





 return;



 }



 Optional<ConfigKey> found = ConfigKeys.find(args[2]);



 if (found.isEmpty()) {





 sender.sendMessage(plugin.messages().prefixed(MessageKeys.CONFIG_UNKNOWN,







 Map.of("key", args[2])));





 return;



 }



 ConfigKey key = found.get();



 if (!reset && args.length < 4) {





 usage(sender, "/reports config set " + key.path() + " <value>");





 return;



 }



 if (!allowAction(sender)) {





 return;



 }



 CompletableFuture<ConfigEditService.Result> pending = reset





 ? plugin.configEditor().reset(key)





 : plugin.configEditor().set(key, join(args, 3));



 pending.whenComplete((result, error) -> runOnMain(() -> {





 if (error != null || result == null) {







 plugin.getLogger().warning("Config edit failed: "









 + (error == null ? "no result" : error.getMessage()));







 sender.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_UNKNOWN_ERROR,









 Map.of()));







 return;





 }





 sendConfigResult(sender, result, reset);





 if (result.ok()) {







 plugin.audit(reset ? "CONFIG_RESET" : "CONFIG_SET", actor(sender), 0L,









 result.key() + "=" + result.newValue());





 }



 }));

 }


private void sendConfigResult(CommandSender sender, ConfigEditService.Result result,
















 boolean reset) {



 switch (result.status()) {





 case OK -> {







 sender.sendMessage(plugin.messages().prefixed(









 reset ? MessageKeys.CONFIG_RESET : MessageKeys.CONFIG_APPLIED, Map.of(











 "key", result.key(),











 "old", result.oldValue(),











 "new", result.newValue(),











 "value", result.newValue()









 )));







 if (result.restartRequired()) {









 sender.sendMessage(plugin.messages().prefixed(MessageKeys.CONFIG_RESTART,











 Map.of()));







 }





 }





 case DISABLED -> sender.sendMessage(plugin.messages().prefixed(







 MessageKeys.CONFIG_DISABLED, Map.of()));





 case LOADING -> sender.sendMessage(plugin.messages().prefixed(







 MessageKeys.CONFIG_LOADING, Map.of()));





 case BUSY -> sender.sendMessage(plugin.messages().prefixed(







 MessageKeys.CONFIG_BUSY, Map.of()));





 case UNKNOWN -> sender.sendMessage(plugin.messages().prefixed(







 MessageKeys.CONFIG_UNKNOWN, Map.of("key", result.key())));





 case INVALID, REJECTED -> sender.sendMessage(plugin.messages().prefixed(







 MessageKeys.CONFIG_REJECTED, Map.of("reason", result.reason())));





 case WRITE_FAILED -> sender.sendMessage(plugin.messages().prefixed(







 MessageKeys.CONFIG_WRITE_FAILED, Map.of("reason", result.reason())));



 }

 }


private static String configType(ConfigKey key) {



 return switch (key.kind()) {





 case BOOLEAN -> "boolean";





 case INTEGER -> "integer";





 case LONG -> "long";





 case STRING -> "string";





 case CHOICE -> "choice: " + String.join("/", key.suggestions());



 };

 }


private void sendHelp(CommandSender sender) {



 sender.sendMessage(plugin.messages().prefixed(MessageKeys.ADMIN_HELP_HEADER, Map.of()));



 for (String key : List.of(





 MessageKeys.ADMIN_HELP_LIST, MessageKeys.ADMIN_HELP_SEARCH, MessageKeys.ADMIN_HELP_VIEW,





 MessageKeys.ADMIN_HELP_CLAIM, MessageKeys.ADMIN_HELP_RELEASE, MessageKeys.ADMIN_HELP_CLOSE,





 MessageKeys.ADMIN_HELP_REOPEN, MessageKeys.ADMIN_HELP_TP, MessageKeys.ADMIN_HELP_EVIDENCE,





 MessageKeys.ADMIN_HELP_NOTE, MessageKeys.ADMIN_HELP_REPLY, MessageKeys.ADMIN_HELP_REPLIES,





 MessageKeys.ADMIN_HELP_DELETE, MessageKeys.ADMIN_HELP_PURGE, MessageKeys.ADMIN_HELP_OVERDUE,





 MessageKeys.ADMIN_HELP_WATCH, MessageKeys.ADMIN_HELP_UNWATCH,





 MessageKeys.ADMIN_HELP_WATCHING,





 MessageKeys.ADMIN_HELP_STATS, MessageKeys.ADMIN_HELP_EXPORT, MessageKeys.ADMIN_HELP_AUDIT,





 MessageKeys.ADMIN_HELP_RELOAD, MessageKeys.ADMIN_HELP_CONFIG, MessageKeys.ADMIN_HELP_MENU



 )) {





 sender.sendMessage(plugin.messages().component(key, Map.of()));



 }

 }


private Report report(CommandSender sender, String[] args, int index) {



 if (args.length <= index) {





 usage(sender, "/reports " + args[0] + " <id>");





 return null;



 }



 Long id = parseLong(args[index]);



 if (id == null || id <= 0) {





 sender.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_INVALID_ID, Map.of()));





 return null;



 }



 Optional<Report> report = plugin.repository().get(id);



 if (report.isEmpty()) {





 sender.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_REPORT_NOT_FOUND,







 Map.of("id", String.valueOf(id))));





 return null;



 }



 return report.get();

 }


private void sendResult(CommandSender sender, ReportService.OpResult result) {



 sender.sendMessage(plugin.messages().prefixed(result.messageKey(), result.placeholders()));

 }


private boolean require(CommandSender sender, String permission) {



 if (sender.hasPermission(permission)) {





 return true;



 }



 sender.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_NO_PERMISSION, Map.of()));



 return false;

 }


private boolean allowAction(CommandSender sender) {



 long cooldown = plugin.config().security().adminCooldownMillis();



 if (cooldown <= 0) {





 return true;



 }



 if (sender instanceof Player player





 && player.hasPermission(plugin.config().limits().bypassPermission())) {





 return true;



 }



 UUID key = sender instanceof Player player ? player.getUniqueId() : new UUID(0L, 0L);



 long now = System.currentTimeMillis();



 Long last = lastUse.get(key);



 if (last != null && now - last < cooldown) {





 sender.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_ACTION_COOLDOWN, Map.of()));





 return false;



 }



 lastUse.put(key, now);



 return true;

 }


private Player actor(CommandSender sender) {



 return sender instanceof Player player ? player : null;

 }


private void runOnMain(Runnable task) {



 if (!plugin.isEnabled()) {





 return;



 }



 try {





 Bukkit.getScheduler().runTask(plugin, task);



 } catch (RuntimeException exception) {





 plugin.getLogger().warning("Could not run command callback: " + exception.getMessage());



 }

 }


private void usage(CommandSender sender, String usage) {



 sender.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_USAGE, Map.of("usage", usage)));

 }


private static String join(String[] args, int start) {



 if (start >= args.length) {





 return "";



 }



 return String.join(" ", Arrays.copyOfRange(args, start, args.length)).trim();

 }


private static Integer parseInteger(String value) {



 try {





 return Integer.parseInt(value.trim());



 } catch (RuntimeException exception) {





 return null;



 }

 }


private static Long parseLong(String value) {



 try {





 return Long.parseLong(value.trim());



 } catch (RuntimeException exception) {





 return null;



 }

 }


@Override

 public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {



 if (!sender.hasPermission("reportdesk.admin")) {





 return List.of();



 }



 if (args.length == 1) {





 return filter(SUBCOMMANDS, args[0]);



 }



 if (args.length == 2) {





 String sub = args[0].toLowerCase(Locale.ROOT);





 if (sub.equals("config")) {







 return filter(List.of("list", "get", "set", "reset"), args[1]);





 }





 if (sub.equals("list")) {







 return filter(listSuggestions(), args[1]);





 }





 if (sub.equals("search") || sub.equals("close") || sub.equals("purge")







 || sub.equals("overdue") || sub.equals("watching")) {







 return List.of();





 }





 if (sub.equals("watch") || sub.equals("unwatch")) {







 return filter(onlinePlayerNames(), args[1]);





 }





 if (List.of("view", "info", "claim", "release", "reopen", "tp", "evidence", "note",







 "reply", "replies", "thread", "delete").contains(sub)) {







 return filter(reportIds(), args[1]);





 }



 }



 if (args.length == 3 && args[0].equalsIgnoreCase("close")) {





 return filter(List.of("resolved", "rejected", "duplicate", "insufficient"), args[2]);



 }



 if (args.length >= 3 && args[0].equalsIgnoreCase("list")) {





 return filter(listSuggestions(), args[args.length - 1]);



 }



 if (args[0].equalsIgnoreCase("config")) {





 String action = args[1].toLowerCase(Locale.ROOT);





 if (args.length == 3 && List.of("get", "set", "reset").contains(action)) {







 return filter(ConfigKeys.paths(), args[2]);





 }





 if (args.length == 4 && action.equals("set")) {







 Optional<ConfigKey> key = ConfigKeys.find(args[2]);







 if (key.isPresent()) {









 if (key.get().kind() == ConfigKey.Kind.BOOLEAN) {











 return filter(List.of("true", "false"), args[3]);









 }









 if (key.get().kind() == ConfigKey.Kind.CHOICE) {











 return filter(key.get().suggestions(), args[3]);









 }







 }





 }





 return List.of();



 }



 return List.of();

 }


private List<String> listSuggestions() {



 List<String> values = new ArrayList<>(List.of(





 "all", "open", "claimed", "resolved", "rejected", "duplicate", "auto-closed",





 "mine", "oldest", "newest", "priority", "overdue",





 "priority:low", "priority:normal", "priority:high", "priority:urgent"



 ));



 for (var category : plugin.config().categories()) {





 values.add("cat:" + category.id());



 }



 return values;

 }


private List<String> reportIds() {



 List<String> ids = new ArrayList<>();



 for (Report report : plugin.repository().all()) {





 ids.add(String.valueOf(report.id()));





 if (ids.size() >= 50) {







 break;





 }



 }



 return ids;

 }


private List<String> onlinePlayerNames() {



 List<String> names = new ArrayList<>();



 for (Player online : Bukkit.getOnlinePlayers()) {





 names.add(online.getName());



 }



 return names;

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
