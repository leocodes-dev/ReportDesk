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

package com.reportdesk.gui;
import com.reportdesk.ReportDeskPlugin; import com.reportdesk.config.ConfigKey; import com.reportdesk.config.ConfigKeys; import com.reportdesk.config.ConfigLabels; import com.reportdesk.core.AuditEntry; import com.reportdesk.core.Page; import com.reportdesk.core.PlayerSummary; import com.reportdesk.core.Report; import com.reportdesk.core.ReportFilter; import com.reportdesk.core.ReportStats; import com.reportdesk.core.ReportStatus; import com.reportdesk.core.WatchEntry; import com.reportdesk.i18n.MessageKeys; import com.reportdesk.service.ConfigEditService; import com.reportdesk.service.NotificationService; import com.reportdesk.service.WatchlistService; import com.reportdesk.util.TimeText; import net.kyori.adventure.text.Component; import org.bukkit.Bukkit; import org.bukkit.Material; import org.bukkit.entity.Player; import org.bukkit.inventory.Inventory;
import java.util.ArrayList; import java.util.Comparator; import java.util.LinkedHashMap; import java.util.List; import java.util.Locale; import java.util.Map; import java.util.Optional; import java.util.UUID; import java.util.concurrent.ConcurrentHashMap;
/**
* Admin management menu: report queue shortcuts, player directory, statistics,
* watchlist, online config editing, audit viewer and maintenance tools.
*
* <p>Every render method runs on the main thread and only reads the in-memory
* repository. Audit reads, config writes, exports and maintenance run through
* the existing asynchronous services.</p>
*/ public final class AdminGuiService {


private static final int SIZE = 54;

 /** Bottom bar, shared with {@link GuiService}: prev 45, back 48, refresh 49, close 50, hub 52, next 53. */

 private static final int CLOSE_SLOT = 50;

 private static final int BACK_SLOT = 48;

 private static final int HUB_SLOT = 52;

 private static final int REFRESH_SLOT = 49;

 private static final int PREVIOUS_SLOT = 45;

 private static final int NEXT_SLOT = 53;

 /** Row above the bottom bar, used for menu-specific actions. */

 private static final int ACTION_SLOT = 40;

 private static final int EMPTY_SLOT = 22;


private static final int HUB_REPORTS = 19;

 private static final int HUB_NEXT = 21;

 private static final int HUB_PLAYERS = 23;

 private static final int HUB_STATS = 25;

 private static final int HUB_WATCH = 28;

 private static final int HUB_CONFIG = 30;

 private static final int HUB_AUDIT = 32;

 private static final int HUB_MISC = 34;


/** Symmetric 7x4 grid used by the config browser (28 slots). */

 private static final int[] CONFIG_GRID = {



 10, 11, 12, 13, 14, 15, 16,



 19, 20, 21, 22, 23, 24, 25,



 28, 29, 30, 31, 32, 33, 34,



 37, 38, 39, 40, 41, 42, 43

 };

 /**


* Airier 4x3 grid for the section overview. Sections are few and large, so


* leaving a gap between cards reads better than packing them against the


* left edge of the full 7x4 content area.


*/

 private static final int[] CONFIG_SECTION_GRID = {



 19, 21, 23, 25,



 28, 30, 32, 34,



 37, 39, 41, 43

 };

 private static final int CONFIG_INFO_SLOT = 4;

 private static final int CONFIG_VALUE_SLOT = 13;

 private static final int CONFIG_QUICK_SLOT = 20;

 private static final int CONFIG_EXACT_SLOT = 22;

 private static final int CONFIG_RESET_SLOT = 24;

 private static final int CONFIG_SEARCH_SLOT = 8;

 private static final int CONFIG_RELOAD_SLOT = 7;

 private static final int[] CONFIG_MINUS_SLOTS = {10, 11, 12};

 private static final int[] CONFIG_PLUS_SLOTS = {14, 15, 16};


private static final String ATTR_PLAYER = "player";

 private static final String ATTR_SECTION = "section";

 private static final String ATTR_SCOPE = "scope";

 private static final String ATTR_ACTION = "action";

 private static final String ATTR_QUERY = "query";

 private static final String ATTR_PATH = "path";


private static final String ACTION_MAINTENANCE = "maintenance";

 private static final String ACTION_PURGE = "purge";


private final ReportDeskPlugin plugin;

 private final GuiService gui;

 private final Map<UUID, String> playerQueries = new ConcurrentHashMap<>();


public AdminGuiService(ReportDeskPlugin plugin, GuiService gui) {



 this.plugin = plugin;



 this.gui = gui;

 }


/**


* Where a config list was opened from, so detail pages and chat prompts can


* return to the exact section or search result the admin was looking at.


*/

 private record ConfigOrigin(String section, String query, int page) {




static ConfigOrigin of(ReportDeskHolder holder) {





 return new ConfigOrigin(







 holder.attribute(ATTR_SECTION, ""),







 holder.attribute(ATTR_QUERY, ""),







 holder.page()





 );



 }

 }


public void forget(UUID playerId) {



 if (playerId != null) {





 playerQueries.remove(playerId);



 }

 }


// ---------------------------------------------------------------------

 // Hub

 // ---------------------------------------------------------------------


public void openHub(Player player) {



 if (!plugin.config().gui().enabled()) {





 send(player, MessageKeys.REPORT_GUI_DISABLED, Map.of());





 return;



 }



 if (!plugin.config().gui().admin().enabled()) {





 send(player, MessageKeys.ADMIN_GUI_DISABLED, Map.of());





 return;



 }



 if (!require(player, "reportdesk.admin")) {





 return;



 }



 ReportStats stats = ReportStats.compute(plugin.repository().all());



 Map<String, String> placeholders = new LinkedHashMap<>();



 placeholders.put("server", plugin.config().serverName());



 placeholders.put("total", String.valueOf(stats.total()));



 placeholders.put("open", String.valueOf(stats.open()));



 placeholders.put("claimed", String.valueOf(stats.claimed()));



 placeholders.put("closed", String.valueOf(stats.closed()));



 placeholders.put("overdue", String.valueOf(overdueCount()));



 placeholders.put("watching", String.valueOf(plugin.watchlist().watchedBy(player.getUniqueId()).size()));



 placeholders.put("config_keys", String.valueOf(ConfigKeys.all().size()));



 placeholders.put("config_modified", String.valueOf(ConfigGuiSupport.modifiedCount(





 ConfigKeys.all(), plugin.config(), plugin.configEditor().defaults())));



 placeholders.put("config_status", messages().plain(plugin.config().configEditor().enabled()





 ? MessageKeys.GENERAL_ENABLED : MessageKeys.GENERAL_DISABLED));




ReportDeskHolder holder = ReportDeskHolder.of(ReportDeskHolder.Type.ADMIN_HUB);



 Inventory inventory = gui.create(holder, MessageKeys.ADMIN_GUI_HUB_TITLE, placeholders);




inventory.setItem(4, gui.item(Material.NETHER_STAR,





 messages().component(MessageKeys.ADMIN_GUI_HUB_INFO_NAME, placeholders),





 messages().components(MessageKeys.ADMIN_GUI_HUB_INFO_LORE, placeholders)));




inventory.setItem(HUB_REPORTS, gui.item(Material.CHEST,





 messages().component(MessageKeys.ADMIN_GUI_HUB_REPORTS_NAME, placeholders),





 messages().components(MessageKeys.ADMIN_GUI_HUB_REPORTS_LORE, placeholders)));



 inventory.setItem(HUB_NEXT, gui.item(Material.SPECTRAL_ARROW,





 messages().component(MessageKeys.ADMIN_GUI_HUB_NEXT_NAME, placeholders),





 messages().components(MessageKeys.ADMIN_GUI_HUB_NEXT_LORE, placeholders)));



 inventory.setItem(HUB_PLAYERS, gui.item(Material.PLAYER_HEAD,





 messages().component(MessageKeys.ADMIN_GUI_HUB_PLAYERS_NAME, placeholders),





 messages().components(MessageKeys.ADMIN_GUI_HUB_PLAYERS_LORE, placeholders)));



 inventory.setItem(HUB_STATS, gui.item(Material.DIAMOND,





 messages().component(MessageKeys.ADMIN_GUI_HUB_STATS_NAME, placeholders),





 messages().components(MessageKeys.ADMIN_GUI_HUB_STATS_LORE, placeholders)));




if (player.hasPermission("reportdesk.watch")) {





 inventory.setItem(HUB_WATCH, gui.item(Material.BELL,







 messages().component(MessageKeys.ADMIN_GUI_HUB_WATCH_NAME, placeholders),







 messages().components(MessageKeys.ADMIN_GUI_HUB_WATCH_LORE, placeholders)));



 }



 if (player.hasPermission("reportdesk.config")) {





 inventory.setItem(HUB_CONFIG, gui.item(Material.COMPARATOR,







 messages().component(MessageKeys.ADMIN_GUI_HUB_CONFIG_NAME, placeholders),







 messages().components(MessageKeys.ADMIN_GUI_HUB_CONFIG_LORE, placeholders)));



 }



 inventory.setItem(HUB_AUDIT, gui.item(Material.WRITABLE_BOOK,





 messages().component(MessageKeys.ADMIN_GUI_HUB_AUDIT_NAME, placeholders),





 messages().components(MessageKeys.ADMIN_GUI_HUB_AUDIT_LORE, placeholders)));



 inventory.setItem(HUB_MISC, gui.item(Material.HOPPER,





 messages().component(MessageKeys.ADMIN_GUI_HUB_MISC_NAME, placeholders),





 messages().components(MessageKeys.ADMIN_GUI_HUB_MISC_LORE, placeholders)));




gui.refreshButton(inventory);



 gui.closeButton(inventory);



 gui.fill(inventory, holder);



 player.openInventory(inventory);

 }


// ---------------------------------------------------------------------

 // Players

 // ---------------------------------------------------------------------


public void openPlayers(Player player, int requestedPage) {



 if (!require(player, "reportdesk.admin")) {





 return;



 }



 if (!plugin.repository().isReady()) {





 send(player, MessageKeys.GENERAL_LOADING, Map.of());





 return;



 }



 String query = playerQueries.getOrDefault(player.getUniqueId(), "");



 List<PlayerSummary> summaries = PlayerSummary.build(plugin.repository().all(),





 onlineNames(), query);



 int pageSize = Math.min(plugin.config().reports().guiPageSize(), GuiService.listCapacity());



 Page<PlayerSummary> page = Page.of(summaries, requestedPage, pageSize);




ReportDeskHolder holder = ReportDeskHolder.paged(ReportDeskHolder.Type.ADMIN_PLAYERS,





 page.page());



 Map<String, String> values = new LinkedHashMap<>();



 values.put("page", String.valueOf(page.page()));



 values.put("pages", String.valueOf(page.totalPages()));



 values.put("query", query.isBlank() ? "-" : query);



 values.put("count", String.valueOf(summaries.size()));



 values.put("online", String.valueOf(Bukkit.getOnlinePlayers().size()));



 Inventory inventory = gui.create(holder, MessageKeys.ADMIN_GUI_PLAYERS_TITLE, values);




gui.header(inventory, Material.PLAYER_HEAD, MessageKeys.ADMIN_GUI_PLAYERS_HEADER_NAME,





 messages().components(MessageKeys.ADMIN_GUI_PLAYERS_HEADER_LORE, values));




int index = 0;



 for (PlayerSummary summary : page.items()) {





 if (index >= pageSize) {







 break;





 }





 int slot = GuiService.listSlot(index);





 holder.putValue(slot, summary.name());





 Map<String, String> placeholders = playerPlaceholders(summary);





 inventory.setItem(slot, gui.item(Material.PLAYER_HEAD,







 messages().component(MessageKeys.ADMIN_GUI_PLAYER_ITEM_NAME, placeholders),







 messages().components(MessageKeys.ADMIN_GUI_PLAYER_ITEM_LORE, placeholders)));





 index++;



 }



 if (page.items().isEmpty()) {





 inventory.setItem(EMPTY_SLOT, gui.item(Material.PAPER,







 messages().component(MessageKeys.ADMIN_GUI_PLAYERS_EMPTY), List.of()));



 }



 inventory.setItem(GuiService.TOP_RIGHT_SLOT, gui.item(Material.EMERALD,





 messages().component(MessageKeys.ADMIN_GUI_PLAYERS_SEARCH_NAME),





 messages().components(MessageKeys.ADMIN_GUI_PLAYERS_SEARCH_LORE, values)));



 gui.backButton(inventory);



 gui.hubButton(inventory);



 gui.refreshButton(inventory);



 gui.pageNavigation(inventory, page);



 gui.closeButton(inventory);



 gui.fill(inventory, holder);



 player.openInventory(inventory);

 }


private Map<String, String> playerPlaceholders(PlayerSummary summary) {



 Map<String, String> placeholders = new LinkedHashMap<>();



 placeholders.put("player", summary.name());



 placeholders.put("state", summary.online()





 ? messages().plain(MessageKeys.ADMIN_GUI_ONLINE)





 : messages().plain(MessageKeys.ADMIN_GUI_OFFLINE));



 placeholders.put("online", String.valueOf(summary.online()));



 placeholders.put("target", String.valueOf(summary.reportsAsTarget()));



 placeholders.put("reporter", String.valueOf(summary.reportsAsReporter()));



 placeholders.put("open", String.valueOf(summary.openAsTarget()));



 placeholders.put("resolved", String.valueOf(summary.resolvedAsTarget()));



 placeholders.put("total", String.valueOf(summary.totalReports()));



 placeholders.put("last", TimeText.absolute(summary.lastActivityAt(), messages()));



 return placeholders;

 }


public void openPlayer(Player player, String name) {



 if (!require(player, "reportdesk.admin")) {





 return;



 }



 if (name == null || name.isBlank()) {





 send(player, MessageKeys.GENERAL_INVALID_INPUT,







 Map.of("reason", messages().plain(MessageKeys.GENERAL_NONE)));





 return;



 }



 PlayerSummary summary = summary(name);



 if (summary == null) {





 summary = new PlayerSummary(name.trim(), 0, 0, 0, 0, 0L,







 Bukkit.getPlayerExact(name) != null);



 }



 boolean watching = plugin.watchlist().isWatching(player.getUniqueId(), summary.name());



 Map<String, String> placeholders = playerPlaceholders(summary);



 placeholders.put("uuid", onlineUuid(summary.name()));



 placeholders.put("watching", String.valueOf(watching));




ReportDeskHolder holder = ReportDeskHolder.of(ReportDeskHolder.Type.ADMIN_PLAYER);



 holder.putAttribute(ATTR_PLAYER, summary.name());



 Inventory inventory = gui.create(holder, MessageKeys.ADMIN_GUI_PLAYER_TITLE, placeholders);




inventory.setItem(4, gui.item(Material.PLAYER_HEAD,





 messages().component(MessageKeys.ADMIN_GUI_PLAYER_INFO_NAME, placeholders),





 messages().components(MessageKeys.ADMIN_GUI_PLAYER_INFO_LORE, placeholders)));



 inventory.setItem(10, gui.item(Material.LIME_DYE,





 messages().component(MessageKeys.ADMIN_GUI_PLAYER_OPEN_NAME, placeholders),





 messages().components(MessageKeys.ADMIN_GUI_PLAYER_OPEN_LORE, placeholders)));



 inventory.setItem(12, gui.item(Material.BOOK,





 messages().component(MessageKeys.ADMIN_GUI_PLAYER_ALL_NAME, placeholders),





 messages().components(MessageKeys.ADMIN_GUI_PLAYER_ALL_LORE, placeholders)));



 inventory.setItem(14, gui.item(Material.GREEN_DYE,





 messages().component(MessageKeys.ADMIN_GUI_PLAYER_VERDICT_NAME, placeholders),





 messages().components(MessageKeys.ADMIN_GUI_PLAYER_VERDICT_LORE, placeholders)));



 inventory.setItem(16, gui.item(Material.WRITABLE_BOOK,





 messages().component(MessageKeys.ADMIN_GUI_PLAYER_REPORTER_NAME, placeholders),





 messages().components(MessageKeys.ADMIN_GUI_PLAYER_REPORTER_LORE, placeholders)));




inventory.setItem(28, gui.item(Material.ENDER_PEARL,





 messages().component(MessageKeys.ADMIN_GUI_PLAYER_TP_NAME, placeholders),





 messages().components(MessageKeys.ADMIN_GUI_PLAYER_TP_LORE, placeholders)));



 if (player.hasPermission("reportdesk.watch")) {





 inventory.setItem(30, gui.item(watching ? Material.GRAY_DYE : Material.BELL,







 messages().component(watching











 ? MessageKeys.ADMIN_GUI_PLAYER_UNWATCH_NAME











 : MessageKeys.ADMIN_GUI_PLAYER_WATCH_NAME, placeholders),







 messages().components(watching









 ? MessageKeys.ADMIN_GUI_PLAYER_UNWATCH_LORE









 : MessageKeys.ADMIN_GUI_PLAYER_WATCH_LORE, placeholders)));



 }




gui.backButton(inventory, MessageKeys.ADMIN_GUI_BACK);



 gui.refreshButton(inventory);



 gui.hubButton(inventory);



 gui.closeButton(inventory);



 gui.fill(inventory, holder);



 player.openInventory(inventory);

 }


public void openPlayerReports(Player player, String name, String scope, int requestedPage) {



 if (!require(player, "reportdesk.admin")) {





 return;



 }



 String effectiveScope = scope == null ? "all" : scope.toLowerCase(Locale.ROOT);



 List<Report> matches = new ArrayList<>();



 for (Report report : plugin.repository().all()) {





 if (matchesScope(report, name, effectiveScope)) {







 matches.add(report);





 }



 }



 matches.sort(Comparator.comparingLong(Report::createdAt).reversed()





 .thenComparing(Comparator.comparingLong(Report::id).reversed()));



 int pageSize = Math.min(plugin.config().reports().guiPageSize(), GuiService.listCapacity());



 Page<Report> page = Page.of(matches, requestedPage, pageSize);




ReportDeskHolder holder = ReportDeskHolder.paged(ReportDeskHolder.Type.ADMIN_PLAYER_REPORTS,





 page.page());



 holder.putAttribute(ATTR_PLAYER, name);



 holder.putAttribute(ATTR_SCOPE, effectiveScope);



 Map<String, String> title = new LinkedHashMap<>();



 title.put("player", name);



 title.put("page", String.valueOf(page.page()));



 title.put("pages", String.valueOf(page.totalPages()));



 title.put("scope", scopeLabel(effectiveScope));



 title.put("count", String.valueOf(matches.size()));



 Inventory inventory = gui.create(holder, MessageKeys.ADMIN_GUI_PLAYER_REPORTS_TITLE, title);




gui.header(inventory, Material.PLAYER_HEAD,





 MessageKeys.ADMIN_GUI_PLAYER_REPORTS_HEADER_NAME,





 messages().components(MessageKeys.ADMIN_GUI_PLAYER_REPORTS_HEADER_LORE, title));




int index = 0;



 for (Report report : page.items()) {





 if (index >= pageSize) {







 break;





 }





 int slot = GuiService.listSlot(index);





 holder.putReportId(slot, report.id());





 Map<String, String> placeholders = plugin.notification().placeholders(report);





 inventory.setItem(slot, gui.item(gui.statusMaterial(report.status()),







 messages().component(MessageKeys.GUI_LIST_ITEM_NAME, placeholders),







 messages().components(MessageKeys.GUI_LIST_ITEM_LORE, placeholders)));





 index++;



 }



 if (page.items().isEmpty()) {





 inventory.setItem(EMPTY_SLOT, gui.item(Material.PAPER,







 messages().component(MessageKeys.GUI_EMPTY), List.of()));



 }



 gui.backButton(inventory);



 gui.hubButton(inventory);



 gui.refreshButton(inventory);



 gui.pageNavigation(inventory, page);



 gui.closeButton(inventory);



 gui.fill(inventory, holder);



 player.openInventory(inventory);

 }


private boolean matchesScope(Report report, String name, String scope) {



 boolean target = report.targetName() != null && report.targetName().equalsIgnoreCase(name);



 boolean reporter = report.reporterName() != null && report.reporterName().equalsIgnoreCase(name);



 return switch (scope) {





 case "open" -> target && report.isOpen();





 case "target" -> target;





 case "reporter" -> reporter;





 default -> target || reporter;



 };

 }


private String scopeLabel(String scope) {



 return messages().plain(switch (scope) {





 case "open" -> MessageKeys.ADMIN_GUI_SCOPE_OPEN;





 case "target" -> MessageKeys.ADMIN_GUI_SCOPE_TARGET;





 case "reporter" -> MessageKeys.ADMIN_GUI_SCOPE_REPORTER;





 default -> MessageKeys.ADMIN_GUI_SCOPE_ALL;



 });

 }


// ---------------------------------------------------------------------

 // Statistics

 // ---------------------------------------------------------------------


public void openStats(Player player) {



 if (!require(player, "reportdesk.admin")) {





 return;



 }



 ReportStats stats = ReportStats.compute(plugin.repository().all());



 Map<String, String> placeholders = new LinkedHashMap<>();



 placeholders.put("total", String.valueOf(stats.total()));



 placeholders.put("open", String.valueOf(stats.open()));



 placeholders.put("claimed", String.valueOf(stats.claimed()));



 placeholders.put("closed", String.valueOf(stats.closed()));



 placeholders.put("resolved", String.valueOf(stats.resolved()));



 placeholders.put("rejected", String.valueOf(stats.rejected()));



 placeholders.put("duplicate", String.valueOf(stats.duplicate()));



 placeholders.put("auto-closed", String.valueOf(stats.autoClosed()));



 placeholders.put("average", TimeText.duration(stats.averageHandlingMillis(), messages()));



 placeholders.put("overdue", String.valueOf(overdueCount()));



 placeholders.put("targets", String.valueOf(plugin.config().reports().statsTopTargets()));



 placeholders.put("staff", String.valueOf(plugin.config().reports().staffStatsTop()));




ReportDeskHolder holder = ReportDeskHolder.of(ReportDeskHolder.Type.ADMIN_STATS);



 Inventory inventory = gui.create(holder, MessageKeys.ADMIN_GUI_STATS_TITLE, placeholders);




inventory.setItem(4, gui.item(Material.DIAMOND,





 messages().component(MessageKeys.ADMIN_GUI_STATS_OVERVIEW_NAME, placeholders),





 messages().components(MessageKeys.ADMIN_GUI_STATS_OVERVIEW_LORE, placeholders)));




List<Component> statusLore = new ArrayList<>();



 statusLore.add(statsLine(MessageKeys.STATUS_OPEN, stats.open()));



 statusLore.add(statsLine(MessageKeys.STATUS_CLAIMED, stats.claimed()));



 statusLore.add(statsLine(MessageKeys.STATUS_RESOLVED, stats.resolved()));



 statusLore.add(statsLine(MessageKeys.STATUS_REJECTED, stats.rejected()));



 statusLore.add(statsLine(MessageKeys.STATUS_DUPLICATE, stats.duplicate()));



 statusLore.add(statsLine(MessageKeys.STATUS_AUTO_CLOSED, stats.autoClosed()));



 inventory.setItem(10, gui.item(Material.CHEST,





 messages().component(MessageKeys.ADMIN_GUI_STATS_STATUS_NAME, placeholders),





 statusLore));




inventory.setItem(12, gui.item(Material.GOLD_INGOT,





 messages().component(MessageKeys.ADMIN_GUI_STATS_CATEGORIES_NAME, placeholders),





 topLore(stats.byCategory(), plugin.config().reports().statsTopTargets())));



 inventory.setItem(14, gui.item(Material.IRON_SWORD,





 messages().component(MessageKeys.ADMIN_GUI_STATS_TARGETS_NAME, placeholders),





 topLore(stats.byTarget(), plugin.config().reports().statsTopTargets())));



 inventory.setItem(16, gui.item(Material.NAME_TAG,





 messages().component(MessageKeys.ADMIN_GUI_STATS_STAFF_NAME, placeholders),





 topLore(stats.byStaff(), plugin.config().reports().staffStatsTop())));




inventory.setItem(28, gui.item(Material.CLOCK,





 messages().component(MessageKeys.ADMIN_GUI_STATS_AVG_NAME, placeholders),





 messages().components(MessageKeys.ADMIN_GUI_STATS_AVG_LORE, placeholders)));



 inventory.setItem(30, gui.item(Material.REDSTONE_TORCH,





 messages().component(MessageKeys.ADMIN_GUI_STATS_OVERDUE_NAME, placeholders),





 messages().components(MessageKeys.ADMIN_GUI_STATS_OVERDUE_LORE, placeholders)));




gui.backButton(inventory);



 gui.refreshButton(inventory);



 gui.hubButton(inventory);



 gui.closeButton(inventory);



 gui.fill(inventory, holder);



 player.openInventory(inventory);

 }


private Component statsLine(String labelKey, int value) {



 return messages().component(MessageKeys.ADMIN_GUI_STATS_LINE, Map.of(





 "key", messages().plain(labelKey),





 "value", String.valueOf(value)



 ));

 }


private List<Component> topLore(Map<String, Integer> counts, int limit) {



 List<Component> lore = new ArrayList<>();



 int shown = 0;



 for (Map.Entry<String, Integer> entry : counts.entrySet()) {





 if (shown >= limit) {







 break;





 }





 lore.add(messages().component(MessageKeys.ADMIN_GUI_STATS_LINE, Map.of(







 "key", categoryOrName(entry.getKey()),







 "value", String.valueOf(entry.getValue())





 )));





 shown++;



 }



 if (lore.isEmpty()) {





 lore.add(messages().component(MessageKeys.ADMIN_GUI_STATS_NONE));



 }



 return lore;

 }


private String categoryOrName(String value) {



 // Category ids are translated by the category registry; names fall back



 // to the stored value, which is already human readable.



 return plugin.config().category(value).isPresent()





 ? plugin.categoryDisplay(value) : value;

 }


// ---------------------------------------------------------------------

 // Watchlist

 // ---------------------------------------------------------------------


public void openWatchlist(Player player, int requestedPage) {



 if (!require(player, "reportdesk.watch")) {





 return;



 }



 if (!plugin.watchlist().enabled()) {





 ReportDeskHolder holder = ReportDeskHolder.paged(







 ReportDeskHolder.Type.ADMIN_WATCHLIST, 1);





 Inventory inventory = gui.create(holder,







 MessageKeys.ADMIN_GUI_WATCHLIST_TITLE, Map.of("page", "1", "pages", "1"));





 inventory.setItem(EMPTY_SLOT, gui.item(Material.BARRIER,







 messages().component(MessageKeys.ADMIN_GUI_WATCHLIST_DISABLED), List.of()));





 gui.backButton(inventory);





 gui.hubButton(inventory);





 gui.closeButton(inventory);





 gui.fill(inventory, holder);





 player.openInventory(inventory);





 return;



 }



 List<WatchEntry> entries = plugin.watchlist().watchedBy(player.getUniqueId());



 int pageSize = Math.min(plugin.config().reports().guiPageSize(), GuiService.listCapacity());



 Page<WatchEntry> page = Page.of(entries, requestedPage, pageSize);




ReportDeskHolder holder = ReportDeskHolder.paged(ReportDeskHolder.Type.ADMIN_WATCHLIST,





 page.page());



 Map<String, String> values = new LinkedHashMap<>();



 values.put("page", String.valueOf(page.page()));



 values.put("pages", String.valueOf(page.totalPages()));



 values.put("count", String.valueOf(entries.size()));



 values.put("max", String.valueOf(plugin.config().notifications().watchlistMaxTargets()));



 Inventory inventory = gui.create(holder, MessageKeys.ADMIN_GUI_WATCHLIST_TITLE, values);




gui.header(inventory, Material.BELL, MessageKeys.ADMIN_GUI_WATCHLIST_HEADER_NAME,





 messages().components(MessageKeys.ADMIN_GUI_WATCHLIST_HEADER_LORE, values));




int index = 0;



 for (WatchEntry entry : page.items()) {





 if (index >= pageSize) {







 break;





 }





 int slot = GuiService.listSlot(index);





 holder.putValue(slot, entry.targetName());





 boolean online = Bukkit.getPlayerExact(entry.targetName()) != null;





 Map<String, String> placeholders = Map.of(







 "player", entry.targetName(),







 "state", online ? messages().plain(MessageKeys.ADMIN_GUI_ONLINE)









 : messages().plain(MessageKeys.ADMIN_GUI_OFFLINE),







 "created", TimeText.absolute(entry.createdAt(), messages())





 );





 inventory.setItem(slot, gui.item(Material.PLAYER_HEAD,







 messages().component(MessageKeys.ADMIN_GUI_WATCHLIST_ITEM_NAME, placeholders),







 messages().components(MessageKeys.ADMIN_GUI_WATCHLIST_ITEM_LORE, placeholders)));





 index++;



 }



 if (page.items().isEmpty()) {





 inventory.setItem(EMPTY_SLOT, gui.item(Material.PAPER,







 messages().component(MessageKeys.ADMIN_GUI_WATCHLIST_EMPTY), List.of()));



 }



 inventory.setItem(GuiService.TOP_RIGHT_SLOT, gui.item(Material.EMERALD,





 messages().component(MessageKeys.ADMIN_GUI_WATCHLIST_ADD_NAME),





 messages().components(MessageKeys.ADMIN_GUI_WATCHLIST_ADD_LORE)));



 gui.backButton(inventory);



 gui.hubButton(inventory);



 gui.refreshButton(inventory);



 gui.pageNavigation(inventory, page);



 gui.closeButton(inventory);



 gui.fill(inventory, holder);



 player.openInventory(inventory);

 }


// ---------------------------------------------------------------------

 // Online config

 // ---------------------------------------------------------------------


public void openConfigSections(Player player) {



 openConfigSections(player, 1);

 }


public void openConfigSections(Player player, int requestedPage) {



 if (!require(player, "reportdesk.config")) {





 return;



 }



 List<String> sections = ConfigKeys.sections();



 Page<String> page = Page.of(sections, requestedPage, configSectionPageSize());




ReportDeskHolder holder = ReportDeskHolder.paged(





 ReportDeskHolder.Type.ADMIN_CONFIG_SECTIONS, page.page());



 Map<String, String> header = configHeaderPlaceholders(ConfigKeys.all());



 Inventory inventory = gui.create(holder, MessageKeys.ADMIN_GUI_CONFIG_TITLE, header);




inventory.setItem(CONFIG_INFO_SLOT, gui.item(Material.COMPARATOR,





 messages().component(MessageKeys.ADMIN_GUI_CONFIG_SECTIONS_HEADER_NAME, header),





 messages().components(MessageKeys.ADMIN_GUI_CONFIG_SECTIONS_HEADER_LORE, header)));




int index = 0;



 for (String section : page.items()) {





 if (index >= CONFIG_SECTION_GRID.length) {







 break;





 }





 int slot = CONFIG_SECTION_GRID[index++];





 holder.putValue(slot, section);





 List<ConfigKey> keys = ConfigKeys.inSection(section);





 Map<String, String> placeholders = configHeaderPlaceholders(keys);





 placeholders.put("section", sectionDisplay(section));





 placeholders.put("description", ConfigLabels.sectionDescription(messages(), section));





 inventory.setItem(slot, gui.item(sectionMaterial(section),







 messages().component(MessageKeys.ADMIN_GUI_CONFIG_SECTION_NAME, placeholders),







 messages().components(MessageKeys.ADMIN_GUI_CONFIG_ENTRY_SECTION_LORE, placeholders)));



 }



 if (page.items().isEmpty()) {





 inventory.setItem(CONFIG_INFO_SLOT, gui.item(Material.PAPER,







 messages().component(MessageKeys.GUI_EMPTY), List.of()));



 }




inventory.setItem(CONFIG_SEARCH_SLOT, gui.item(Material.COMPASS,





 messages().component(MessageKeys.ADMIN_GUI_CONFIG_SEARCH_NAME),





 messages().components(MessageKeys.ADMIN_GUI_CONFIG_ENTRY_SEARCH_LORE, Map.of("query", "-"))));



 gui.backButton(inventory);



 gui.pageNavigation(inventory, page);



 gui.refreshButton(inventory);



 if (player.hasPermission("reportdesk.reload")) {





 inventory.setItem(CONFIG_RELOAD_SLOT, gui.item(Material.HOPPER,







 messages().component(MessageKeys.ADMIN_GUI_CONFIG_RELOAD_NAME),







 messages().components(MessageKeys.ADMIN_GUI_CONFIG_RELOAD_LORE)));



 }



 gui.hubButton(inventory);



 gui.closeButton(inventory);



 gui.fill(inventory, holder);



 player.openInventory(inventory);

 }


public void openConfigKeys(Player player, String section, int requestedPage) {



 openConfigList(player, section, "", requestedPage);

 }


public void openConfigSearch(Player player, String query, int requestedPage) {



 openConfigList(player, "", query, requestedPage);

 }


private void openConfigList(Player player, String section, String query, int requestedPage) {



 if (!require(player, "reportdesk.config")) {





 return;



 }



 String effectiveSection = section == null ? "" : section.trim();



 String effectiveQuery = query == null ? "" : query.trim();



 if (effectiveSection.isEmpty() && effectiveQuery.isEmpty()) {





 openConfigSections(player, 1);





 return;



 }




List<ConfigKey> keys = effectiveQuery.isEmpty()





 ? ConfigKeys.inSection(effectiveSection)





 : ConfigKeys.all().stream()







 .filter(key -> ConfigGuiSupport.matchesQuery(key, plugin.config(), effectiveQuery,









 ConfigLabels.label(messages(), key.path()),









 ConfigLabels.description(messages(), key.path())))







 .toList();



 Page<ConfigKey> page = Page.of(keys, requestedPage, configPageSize());




ReportDeskHolder holder = ReportDeskHolder.paged(





 ReportDeskHolder.Type.ADMIN_CONFIG_KEYS, page.page());



 holder.putAttribute(ATTR_SECTION, effectiveSection);



 holder.putAttribute(ATTR_QUERY, effectiveQuery);




Map<String, String> header = configHeaderPlaceholders(keys);



 header.put("scope", effectiveQuery.isEmpty()





 ? sectionDisplay(effectiveSection)





 : messages().plain(MessageKeys.ADMIN_GUI_CONFIG_SEARCH_SCOPE,







 Map.of("query", effectiveQuery)));



 header.put("query", effectiveQuery.isEmpty() ? "-" : effectiveQuery);



 header.put("page", String.valueOf(page.page()));



 header.put("pages", String.valueOf(page.totalPages()));




String titleKey = effectiveQuery.isEmpty()





 ? MessageKeys.ADMIN_GUI_CONFIG_KEYS_TITLE





 : MessageKeys.ADMIN_GUI_CONFIG_SEARCH_TITLE;



 Inventory inventory = gui.create(holder, titleKey, header);




if (page.items().isEmpty()) {





 inventory.setItem(CONFIG_INFO_SLOT, gui.item(Material.PAPER,







 messages().component(effectiveQuery.isEmpty()









 ? MessageKeys.GUI_EMPTY : MessageKeys.ADMIN_GUI_CONFIG_SEARCH_EMPTY,









 header), List.of()));



 } else {





 inventory.setItem(CONFIG_INFO_SLOT, gui.item(Material.COMPARATOR,







 messages().component(MessageKeys.ADMIN_GUI_CONFIG_LIST_HEADER_NAME, header),







 messages().components(MessageKeys.ADMIN_GUI_CONFIG_LIST_HEADER_LORE, header)));



 }




int index = 0;



 for (ConfigKey key : page.items()) {





 if (index >= CONFIG_GRID.length) {







 break;





 }





 int slot = CONFIG_GRID[index++];





 holder.putValue(slot, key.path());





 Map<String, String> placeholders = configPlaceholders(key);





 inventory.setItem(slot, gui.item(configMaterial(key),







 messages().component(MessageKeys.ADMIN_GUI_CONFIG_ENTRY_NAME, placeholders),







 configLore(key, placeholders)));



 }




gui.backButton(inventory);



 inventory.setItem(CONFIG_SEARCH_SLOT, gui.item(Material.COMPASS,





 messages().component(MessageKeys.ADMIN_GUI_CONFIG_SEARCH_NAME),





 messages().components(MessageKeys.ADMIN_GUI_CONFIG_ENTRY_SEARCH_LORE,







 Map.of("query", effectiveQuery.isEmpty() ? "-" : effectiveQuery))));



 gui.pageNavigation(inventory, page);



 gui.refreshButton(inventory);



 gui.hubButton(inventory);



 gui.closeButton(inventory);



 gui.fill(inventory, holder);



 player.openInventory(inventory);

 }


private void openConfigKeyDetail(Player player, ConfigKey key, ConfigOrigin origin) {



 if (!require(player, "reportdesk.config")) {





 return;



 }



 Map<String, String> placeholders = configPlaceholders(key);



 placeholders.put("path", key.path());



 ReportDeskHolder holder = ReportDeskHolder.paged(





 ReportDeskHolder.Type.ADMIN_CONFIG_KEY, origin.page());



 holder.putAttribute(ATTR_SECTION, origin.section());



 holder.putAttribute(ATTR_QUERY, origin.query());



 holder.putAttribute(ATTR_PATH, key.path());



 Inventory inventory = gui.create(holder,





 MessageKeys.ADMIN_GUI_CONFIG_ENTRY_DETAIL_TITLE, placeholders);




List<Component> info = new ArrayList<>(messages().components(





 MessageKeys.ADMIN_GUI_CONFIG_ENTRY_DETAIL_INFO_LORE, placeholders));



 if (key.kind() == ConfigKey.Kind.CHOICE) {





 info.add(messages().component(MessageKeys.ADMIN_GUI_CONFIG_ENTRY_CHOICES, placeholders));



 }



 info.add(messages().component(MessageKeys.ADMIN_GUI_CONFIG_ENTRY_DIVIDER));



 info.add(messages().component(ConfigGuiSupport.isDefault(key, plugin.config(),







 plugin.configEditor().defaults())





 ? MessageKeys.ADMIN_GUI_CONFIG_ENTRY_STATE_DEFAULT





 : MessageKeys.ADMIN_GUI_CONFIG_ENTRY_STATE_MODIFIED));



 if (key.restartRequired()) {





 info.add(messages().component(MessageKeys.CONFIG_RESTART));



 }



 inventory.setItem(CONFIG_INFO_SLOT, gui.item(configMaterial(key),





 messages().component(MessageKeys.ADMIN_GUI_CONFIG_ENTRY_DETAIL_INFO_NAME, placeholders),





 info));




inventory.setItem(CONFIG_VALUE_SLOT, gui.item(configMaterial(key),





 messages().component(MessageKeys.ADMIN_GUI_CONFIG_ENTRY_CURRENT_NAME, placeholders),





 List.of()));




if (key.kind() == ConfigKey.Kind.BOOLEAN || key.kind() == ConfigKey.Kind.CHOICE) {





 ConfigGuiSupport.quickChange(key, plugin.config()).ifPresent(next -> {







 placeholders.put("next", next);







 boolean toggle = key.kind() == ConfigKey.Kind.BOOLEAN;







 inventory.setItem(CONFIG_QUICK_SLOT, gui.item(









 toggle ? Material.LEVER : Material.HOPPER,









 messages().component(toggle











 ? MessageKeys.ADMIN_GUI_CONFIG_DETAIL_TOGGLE_NAME











 : MessageKeys.ADMIN_GUI_CONFIG_DETAIL_NEXT_NAME, placeholders),









 messages().components(toggle











 ? MessageKeys.ADMIN_GUI_CONFIG_DETAIL_TOGGLE_LORE











 : MessageKeys.ADMIN_GUI_CONFIG_DETAIL_NEXT_LORE, placeholders)));





 });



 }




if (key.kind() == ConfigKey.Kind.INTEGER || key.kind() == ConfigKey.Kind.LONG) {





 long[] steps = ConfigGuiSupport.adjustSteps(key);





 for (int i = 0; i < steps.length && i < CONFIG_MINUS_SLOTS.length; i++) {







 inventory.setItem(CONFIG_MINUS_SLOTS[i],









 adjustmentItem(placeholders, -steps[i]));





 }





 for (int i = 0; i < steps.length && i < CONFIG_PLUS_SLOTS.length; i++) {







 inventory.setItem(CONFIG_PLUS_SLOTS[i],









 adjustmentItem(placeholders, steps[i]));





 }



 }




inventory.setItem(CONFIG_EXACT_SLOT, gui.item(Material.NAME_TAG,





 messages().component(MessageKeys.ADMIN_GUI_CONFIG_DETAIL_EXACT_NAME, placeholders),





 messages().components(MessageKeys.ADMIN_GUI_CONFIG_DETAIL_EXACT_LORE, placeholders)));



 inventory.setItem(CONFIG_RESET_SLOT, gui.item(Material.GRAY_DYE,





 messages().component(MessageKeys.ADMIN_GUI_CONFIG_DETAIL_RESET_NAME, placeholders),





 messages().components(MessageKeys.ADMIN_GUI_CONFIG_DETAIL_RESET_LORE, placeholders)));




gui.backButton(inventory, MessageKeys.ADMIN_GUI_CONFIG_DETAIL_BACK);



 gui.refreshButton(inventory);



 gui.hubButton(inventory);



 gui.closeButton(inventory);



 gui.fill(inventory, holder);



 player.openInventory(inventory);

 }


private org.bukkit.inventory.ItemStack adjustmentItem(Map<String, String> placeholders,




























 long amount) {



 String label = (amount > 0 ? "+" : "") + amount;



 Map<String, String> values = new LinkedHashMap<>(placeholders);



 values.put("amount", label);



 return gui.item(amount > 0 ? Material.LIME_DYE : Material.RED_DYE,





 messages().component(MessageKeys.ADMIN_GUI_CONFIG_DETAIL_ADJUST_NAME, values),





 messages().components(MessageKeys.ADMIN_GUI_CONFIG_ENTRY_DETAIL_ADJUST_LORE, values));

 }


private Map<String, String> configHeaderPlaceholders(List<ConfigKey> keys) {



 Map<String, String> placeholders = new LinkedHashMap<>();



 placeholders.put("count", String.valueOf(keys.size()));



 placeholders.put("modified", String.valueOf(ConfigGuiSupport.modifiedCount(





 keys, plugin.config(), plugin.configEditor().defaults())));



 placeholders.put("restart", String.valueOf(keys.stream()





 .filter(ConfigKey::restartRequired).count()));



 placeholders.put("ready", configReadyLabel());



 return placeholders;

 }


private String configReadyLabel() {



 return messages().plain(plugin.configEditor().ready()





 ? MessageKeys.ADMIN_GUI_CONFIG_READY





 : MessageKeys.ADMIN_GUI_CONFIG_NOT_READY);

 }


private int configPageSize() {



 return Math.max(1, Math.min(plugin.config().reports().guiPageSize(),





 CONFIG_GRID.length));

 }


private int configSectionPageSize() {



 return Math.max(1, Math.min(plugin.config().reports().guiPageSize(),





 CONFIG_SECTION_GRID.length));

 }


private Map<String, String> configPlaceholders(ConfigKey key) {



 Map<String, String> placeholders = new LinkedHashMap<>();



 placeholders.put("key", key.path());



 placeholders.put("label", ConfigLabels.label(messages(), key.path()));



 placeholders.put("description", ConfigLabels.description(messages(), key.path()));



 placeholders.put("value", key.display(key.current(plugin.config())));



 placeholders.put("default", key.display(key.current(plugin.configEditor().defaults())));



 placeholders.put("type", ConfigLabels.typeName(messages(), key.kind()));



 placeholders.put("choices", key.suggestions().isEmpty()





 ? "-" : String.join(" / ", key.suggestions()));



 return placeholders;

 }


private List<Component> configLore(ConfigKey key, Map<String, String> placeholders) {



 List<Component> lore = new ArrayList<>(messages().components(





 MessageKeys.ADMIN_GUI_CONFIG_ENTRY_LORE, placeholders));



 if (key.kind() == ConfigKey.Kind.CHOICE) {





 lore.add(messages().component(MessageKeys.ADMIN_GUI_CONFIG_ENTRY_CHOICES, placeholders));



 }



 lore.add(messages().component(MessageKeys.ADMIN_GUI_CONFIG_ENTRY_DIVIDER));



 lore.add(messages().component(ConfigGuiSupport.isDefault(key, plugin.config(),







 plugin.configEditor().defaults())





 ? MessageKeys.ADMIN_GUI_CONFIG_ENTRY_STATE_DEFAULT





 : MessageKeys.ADMIN_GUI_CONFIG_ENTRY_STATE_MODIFIED));



 lore.add(Component.empty());



 lore.add(messages().component(configHintKey(key)));



 lore.add(messages().component(MessageKeys.ADMIN_GUI_CONFIG_KEY_HINT_SHIFT));



 lore.add(messages().component(MessageKeys.ADMIN_GUI_CONFIG_KEY_HINT_RESET));



 if (key.restartRequired()) {





 lore.add(messages().component(MessageKeys.CONFIG_RESTART));



 }



 return lore;

 }


private String configHintKey(ConfigKey key) {



 return switch (key.kind()) {





 case BOOLEAN -> MessageKeys.ADMIN_GUI_CONFIG_KEY_HINT_BOOLEAN;





 case CHOICE -> MessageKeys.ADMIN_GUI_CONFIG_KEY_HINT_CHOICE;





 default -> MessageKeys.ADMIN_GUI_CONFIG_KEY_HINT_VALUE;



 };

 }


// ---------------------------------------------------------------------

 // Misc / audit / maintenance

 // ---------------------------------------------------------------------


public void openMisc(Player player) {



 if (!require(player, "reportdesk.admin")) {





 return;



 }



 Map<String, String> placeholders = new LinkedHashMap<>();



 placeholders.put("server", plugin.config().serverName());



 placeholders.put("version", plugin.getPluginMeta().getVersion());



 placeholders.put("language", plugin.config().language());



 placeholders.put("storage", plugin.config().storage().directory());



 placeholders.put("total", String.valueOf(plugin.repository().size()));



 placeholders.put("online", String.valueOf(Bukkit.getOnlinePlayers().size()));



 placeholders.put("audit", messages().plain(plugin.config().audit().enabled()





 ? MessageKeys.GENERAL_ENABLED : MessageKeys.GENERAL_DISABLED));



 placeholders.put("days", String.valueOf(plugin.config().reports().retentionDays()));




ReportDeskHolder holder = ReportDeskHolder.of(ReportDeskHolder.Type.ADMIN_MISC);



 Inventory inventory = gui.create(holder, MessageKeys.ADMIN_GUI_MISC_TITLE, placeholders);



 inventory.setItem(4, gui.item(Material.BOOK,





 messages().component(MessageKeys.ADMIN_GUI_MISC_INFO_NAME, placeholders),





 messages().components(MessageKeys.ADMIN_GUI_MISC_INFO_LORE, placeholders)));




inventory.setItem(10, gui.item(Material.WRITABLE_BOOK,





 messages().component(MessageKeys.ADMIN_GUI_MISC_AUDIT_NAME, placeholders),





 messages().components(MessageKeys.ADMIN_GUI_MISC_AUDIT_LORE, placeholders)));



 inventory.setItem(12, gui.item(Material.MAP,





 messages().component(MessageKeys.ADMIN_GUI_MISC_EXPORT_NAME, placeholders),





 messages().components(MessageKeys.ADMIN_GUI_MISC_EXPORT_LORE, placeholders)));



 if (player.hasPermission("reportdesk.admin.delete")) {





 inventory.setItem(14, gui.item(Material.ANVIL,







 messages().component(MessageKeys.ADMIN_GUI_MISC_MAINTENANCE_NAME, placeholders),







 messages().components(MessageKeys.ADMIN_GUI_MISC_MAINTENANCE_LORE, placeholders)));





 inventory.setItem(16, gui.item(Material.LAVA_BUCKET,







 messages().component(MessageKeys.ADMIN_GUI_MISC_PURGE_NAME, placeholders),







 messages().components(MessageKeys.ADMIN_GUI_MISC_PURGE_LORE, placeholders)));



 }



 if (player.hasPermission("reportdesk.reload")) {





 inventory.setItem(28, gui.item(Material.REDSTONE,







 messages().component(MessageKeys.ADMIN_GUI_MISC_RELOAD_NAME, placeholders),







 messages().components(MessageKeys.ADMIN_GUI_MISC_RELOAD_LORE, placeholders)));



 }



 inventory.setItem(30, gui.item(Material.KNOWLEDGE_BOOK,





 messages().component(MessageKeys.ADMIN_GUI_MISC_HELP_NAME, placeholders),





 messages().components(MessageKeys.ADMIN_GUI_MISC_HELP_LORE, placeholders)));




gui.backButton(inventory);



 gui.hubButton(inventory);



 gui.closeButton(inventory);



 gui.fill(inventory, holder);



 player.openInventory(inventory);

 }


public void openAudit(Player player, int requestedPage) {



 if (!require(player, "reportdesk.admin")) {





 return;



 }



 if (!plugin.config().audit().enabled()) {





 send(player, MessageKeys.ADMIN_GUI_AUDIT_DISABLED, Map.of());





 return;



 }



 send(player, MessageKeys.GUI_LOADING, Map.of());



 int requested = Math.max(1, requestedPage);



 plugin.auditStorage().readPageInfo(requested, auditGuiPageSize())





 .whenComplete((result, throwable) -> gui.runSync(() -> {







 if (!player.isOnline()) {









 return;







 }







 if (throwable != null || result == null) {









 plugin.getLogger().warning("Audit GUI read failed: "











 + (throwable == null ? "no result" : throwable.getMessage()));









 send(player, MessageKeys.ADMIN_GUI_AUDIT_READ_FAILED, Map.of());









 return;







 }







 renderAudit(player, result);





 }));

 }


private void renderAudit(Player player, com.reportdesk.storage.AuditStorage.AuditPage page) {



 ReportDeskHolder holder = ReportDeskHolder.paged(ReportDeskHolder.Type.ADMIN_AUDIT,





 page.page());



 Map<String, String> title = new LinkedHashMap<>();



 title.put("page", String.valueOf(page.page()));



 title.put("pages", String.valueOf(page.totalPages()));



 title.put("count", String.valueOf(page.lines().size()));



 Inventory inventory = gui.create(holder, MessageKeys.ADMIN_GUI_AUDIT_TITLE, title);




gui.header(inventory, Material.WRITABLE_BOOK,





 MessageKeys.ADMIN_GUI_AUDIT_HEADER_NAME,





 messages().components(MessageKeys.ADMIN_GUI_AUDIT_HEADER_LORE, title));




int index = 0;



 for (String line : page.lines()) {





 if (index >= auditGuiPageSize()) {







 break;





 }





 AuditEntry entry = AuditEntry.fromLine(line);





 if (entry == null) {







 continue;





 }





 Map<String, String> placeholders = Map.of(







 "action", entry.action(),







 "actor", entry.actorName(),







 "time", TimeText.absolute(entry.timestamp(), messages()),







 "report", entry.reportId() <= 0 ? "-" : String.valueOf(entry.reportId())





 );





 inventory.setItem(GuiService.listSlot(index), gui.item(Material.PAPER,







 messages().component(MessageKeys.ADMIN_GUI_AUDIT_ITEM_NAME, placeholders),







 auditLore(entry, placeholders)));





 index++;



 }



 if (index == 0) {





 inventory.setItem(EMPTY_SLOT, gui.item(Material.PAPER,







 messages().component(MessageKeys.ADMIN_AUDIT_EMPTY), List.of()));



 }



 gui.backButton(inventory);



 gui.hubButton(inventory);



 gui.refreshButton(inventory);



 gui.pageNavigation(inventory, page.page(), page.totalPages());



 gui.closeButton(inventory);



 gui.fill(inventory, holder);



 player.openInventory(inventory);

 }


private List<Component> auditLore(AuditEntry entry, Map<String, String> placeholders) {



 List<Component> lore = new ArrayList<>(messages().components(





 MessageKeys.ADMIN_GUI_AUDIT_ITEM_LORE, placeholders));



 if (entry.detail() != null && !entry.detail().isBlank()) {





 for (String line : wrap(entry.detail(), 48)) {







 lore.add(Component.text(line));





 }



 }



 return lore;

 }


public void openConfirm(Player player, String action) {



 if (!require(player, "reportdesk.admin")) {





 return;



 }



 boolean purge = ACTION_PURGE.equals(action);



 ReportDeskHolder holder = ReportDeskHolder.of(ReportDeskHolder.Type.ADMIN_CONFIRM);



 holder.putAttribute(ATTR_ACTION, action);



 String days = String.valueOf(plugin.config().reports().retentionDays());



 Map<String, String> placeholders = Map.of(





 "action", messages().plain(purge







 ? MessageKeys.ADMIN_GUI_ACTION_PURGE







 : MessageKeys.ADMIN_GUI_ACTION_MAINTENANCE, Map.of("days", days)),





 "days", days



 );



 Inventory inventory = gui.create(holder, MessageKeys.ADMIN_GUI_CONFIRM_TITLE, placeholders);



 inventory.setItem(11, gui.item(Material.LIME_CONCRETE,





 messages().component(MessageKeys.ADMIN_GUI_CONFIRM_YES_NAME, placeholders),





 messages().components(MessageKeys.ADMIN_GUI_CONFIRM_YES_LORE, placeholders)));



 inventory.setItem(15, gui.item(Material.RED_CONCRETE,





 messages().component(MessageKeys.ADMIN_GUI_CONFIRM_NO_NAME, placeholders),





 messages().components(MessageKeys.ADMIN_GUI_CONFIRM_NO_LORE, placeholders)));



 gui.backButton(inventory);



 gui.closeButton(inventory);



 gui.fill(inventory, holder);



 player.openInventory(inventory);

 }


private void runMaintenance(Player player) {



 send(player, MessageKeys.ADMIN_GUI_MAINTENANCE_STARTED, Map.of());



 plugin.maintenance().runOnce().whenComplete((result, throwable) -> gui.runSync(() -> {





 if (!player.isOnline()) {







 return;





 }





 if (throwable != null || result == null) {







 plugin.getLogger().warning("Maintenance from GUI failed: "









 + (throwable == null ? "no result" : throwable.getMessage()));







 send(player, MessageKeys.GENERAL_UNKNOWN_ERROR, Map.of());







 return;





 }





 send(player, MessageKeys.ADMIN_GUI_MAINTENANCE_DONE, Map.of(







 "auto", String.valueOf(result.autoClosed()),







 "reports", String.valueOf(result.purgedReports()),







 "evidence", String.valueOf(result.purgedEvidence()),







 "audit", String.valueOf(result.purgedAudit())





 ));





 plugin.audit("MAINTENANCE", player, 0L,







 "auto=" + result.autoClosed() + " reports=" + result.purgedReports());



 }));

 }


private void runPurge(Player player) {



 int days = plugin.config().reports().retentionDays();



 if (days <= 0) {





 send(player, MessageKeys.ADMIN_PURGE_NONE, Map.of());





 return;



 }



 send(player, MessageKeys.GUI_LOADING, Map.of());



 plugin.maintenance().purgeOlderThan(days).whenComplete((result, throwable) -> gui.runSync(() -> {





 if (!player.isOnline()) {







 return;





 }





 if (throwable != null || result == null) {







 plugin.getLogger().warning("Purge from GUI failed: "









 + (throwable == null ? "no result" : throwable.getMessage()));







 send(player, MessageKeys.GENERAL_UNKNOWN_ERROR, Map.of());







 return;





 }





 send(player, MessageKeys.ADMIN_PURGED, Map.of(







 "reports", String.valueOf(result.purgedReports()),







 "evidence", String.valueOf(result.purgedEvidence()),







 "audit", String.valueOf(result.purgedAudit())





 ));





 plugin.audit("MANUAL_PURGE", player, 0L, "days=" + days







 + " reports=" + result.purgedReports());



 }));

 }


private void runExport(Player player) {



 send(player, MessageKeys.ADMIN_EXPORT_STARTED, Map.of());



 plugin.storage().exportCsv(plugin.repository().all())





 .whenComplete((path, throwable) -> gui.runSync(() -> {







 if (!player.isOnline()) {









 return;







 }







 if (throwable != null || path == null) {









 plugin.getLogger().warning("Export from GUI failed: "











 + (throwable == null ? "no result" : throwable.getMessage()));









 send(player, MessageKeys.ADMIN_EXPORT_FAILED, Map.of());









 return;







 }







 String display;







 try {









 display = plugin.getDataFolder().toPath().relativize(path).toString();







 } catch (IllegalArgumentException exception) {









 display = path.getFileName().toString();







 }







 send(player, MessageKeys.ADMIN_EXPORT_DONE, Map.of("file", display));







 plugin.audit("EXPORT", player, 0L, display);





 }));

 }


// ---------------------------------------------------------------------

 // Click handling

 // ---------------------------------------------------------------------


public void handleClick(Player player, ReportDeskHolder holder, int slot, boolean rightClick,













 boolean shiftClick) {



 switch (holder.type()) {





 case ADMIN_HUB -> clickHub(player, slot);





 case ADMIN_PLAYERS -> clickPlayers(player, holder, slot);





 case ADMIN_PLAYER -> clickPlayer(player, holder, slot);





 case ADMIN_PLAYER_REPORTS -> clickPlayerReports(player, holder, slot);





 case ADMIN_STATS -> clickStats(player, holder, slot);





 case ADMIN_WATCHLIST -> clickWatchlist(player, holder, slot);





 case ADMIN_CONFIG_SECTIONS -> clickConfigSections(player, holder, slot);





 case ADMIN_CONFIG_KEYS ->







 clickConfigKeys(player, holder, slot, rightClick, shiftClick);





 case ADMIN_CONFIG_KEY -> clickConfigKey(player, holder, slot);





 case ADMIN_MISC -> clickMisc(player, slot);





 case ADMIN_AUDIT -> clickAudit(player, holder, slot);





 case ADMIN_CONFIRM -> clickConfirm(player, holder, slot);





 default -> {







 // Not an admin menu type.





 }



 }

 }


private void clickHub(Player player, int slot) {



 switch (slot) {





 case CLOSE_SLOT -> player.closeInventory();





 case HUB_REPORTS -> {







 click(player);







 gui.openAdminList(player);





 }





 case HUB_NEXT -> {







 click(player);







 openNextOpen(player);





 }





 case HUB_PLAYERS -> {







 click(player);







 openPlayers(player, 1);





 }





 case HUB_STATS -> {







 click(player);







 openStats(player);





 }





 case HUB_WATCH -> {







 click(player);







 openWatchlist(player, 1);





 }





 case HUB_CONFIG -> {







 click(player);







 openConfigSections(player);





 }





 case HUB_AUDIT -> {







 click(player);







 openAudit(player, 1);





 }





 case HUB_MISC -> {







 click(player);







 openMisc(player);





 }





 case REFRESH_SLOT -> openHub(player);





 default -> {







 // Decorative slot.





 }



 }

 }


private void openNextOpen(Player player) {



 Report next = plugin.repository().filter(ReportFilter.ofStatus(ReportStatus.OPEN)







 .withSort(ReportFilter.SortOrder.OLDEST))





 .stream().findFirst()





 .orElseGet(() -> plugin.repository().filter(ReportFilter.ofStatus(ReportStatus.CLAIMED)









 .withSort(ReportFilter.SortOrder.OLDEST))







 .stream().findFirst().orElse(null));



 if (next == null) {





 send(player, MessageKeys.ADMIN_GUI_NO_OPEN, Map.of());





 return;



 }



 gui.openDetail(player, next);

 }


private void clickPlayers(Player player, ReportDeskHolder holder, int slot) {



 if (slot == CLOSE_SLOT) {





 player.closeInventory();





 return;



 }



 if (slot == GuiService.TOP_RIGHT_SLOT) {





 player.closeInventory();





 plugin.chatInput().request(player, MessageKeys.ADMIN_GUI_PLAYERS_SEARCH_PROMPT,







 Map.of(), text -> {









 if (!player.isOnline()) {











 return;









 }









 if (!plugin.validator().validateQuery(text).valid()) {











 send(player, MessageKeys.GENERAL_INVALID_INPUT,













 Map.of("reason", "illegal_characters"));











 return;









 }









 playerQueries.put(player.getUniqueId(), text.trim());









 openPlayers(player, 1);







 });





 return;



 }



 if (slot == HUB_SLOT) {





 openHub(player);





 return;



 }



 if (slot == REFRESH_SLOT) {





 openPlayers(player, holder.page());





 return;



 }



 if (slot == PREVIOUS_SLOT) {





 openPlayers(player, holder.page() - 1);





 return;



 }



 if (slot == NEXT_SLOT) {





 openPlayers(player, holder.page() + 1);





 return;



 }



 String name = holder.value(slot);



 if (name != null) {





 click(player);





 openPlayer(player, name);



 }

 }


private void clickPlayer(Player player, ReportDeskHolder holder, int slot) {



 String name = holder.attribute(ATTR_PLAYER);



 if (slot == CLOSE_SLOT) {





 player.closeInventory();





 return;



 }



 if (slot == BACK_SLOT) {





 openPlayers(player, 1);





 return;



 }



 if (slot == REFRESH_SLOT && name != null) {





 openPlayer(player, name);





 return;



 }



 if (name == null) {





 player.closeInventory();





 return;



 }



 ReportFilter filter;



 switch (slot) {





 case 10 -> {







 click(player);







 openPlayerReports(player, name, "open", 1);





 }





 case 12 -> {







 click(player);







 openPlayerReports(player, name, "all", 1);





 }





 case 14 -> {







 click(player);







 openPlayerReports(player, name, "target", 1);





 }





 case 16 -> {







 click(player);







 openPlayerReports(player, name, "reporter", 1);





 }





 case 28 -> {







 Player target = Bukkit.getPlayerExact(name);







 if (target == null) {









 send(player, MessageKeys.ADMIN_TARGET_NOT_ONLINE, Map.of());









 return;







 }







 if (!gui.adminActionAllowed(player)) {









 return;







 }







 player.teleport(target.getLocation());







 plugin.audit("TELEPORT", player, 0L, name);







 send(player, MessageKeys.ADMIN_TELEPORTED, Map.of("target", target.getName()));







 openPlayer(player, name);





 }





 case 30 -> {







 if (!require(player, "reportdesk.watch")) {









 return;







 }







 if (!gui.adminActionAllowed(player)) {









 return;







 }







 if (plugin.watchlist().isWatching(player.getUniqueId(), name)) {









 WatchlistService.ToggleResult result = plugin.watchlist().remove(player, name);









 send(player, result == WatchlistService.ToggleResult.REMOVED











 ? MessageKeys.ADMIN_WATCH_REMOVED : MessageKeys.ADMIN_WATCH_NOT_FOUND,











 Map.of("target", name));









 plugin.audit("UNWATCH", player, 0L, name);







 } else {









 WatchlistService.ToggleResult result = plugin.watchlist().toggle(player, name);









 switch (result) {











 case ADDED -> {













 send(player, MessageKeys.ADMIN_WATCH_ADDED, Map.of("target", name));













 plugin.audit("WATCH", player, 0L, name);











 }











 case LIMIT_REACHED -> send(player, MessageKeys.ADMIN_WATCH_LIMIT, Map.of(













 "limit", String.valueOf(plugin.config().notifications()















 .watchlistMaxTargets())));











 case DISABLED -> send(player, MessageKeys.ADMIN_WATCH_DISABLED, Map.of());











 default -> send(player, MessageKeys.ADMIN_GUI_WATCHLIST_ALREADY,













 Map.of("target", name));









 }







 }







 openPlayer(player, name);





 }





 default -> {







 // Decorative slot.





 }



 }

 }


private void clickPlayerReports(Player player, ReportDeskHolder holder, int slot) {



 if (slot == CLOSE_SLOT) {





 player.closeInventory();





 return;



 }



 if (slot == BACK_SLOT) {





 openPlayer(player, holder.attribute(ATTR_PLAYER, "?"));





 return;



 }



 if (slot == REFRESH_SLOT) {





 openPlayerReports(player, holder.attribute(ATTR_PLAYER, "?"),







 holder.attribute(ATTR_SCOPE, "all"), holder.page());





 return;



 }



 if (slot == PREVIOUS_SLOT) {





 openPlayerReports(player, holder.attribute(ATTR_PLAYER, "?"),







 holder.attribute(ATTR_SCOPE, "all"), holder.page() - 1);





 return;



 }



 if (slot == NEXT_SLOT) {





 openPlayerReports(player, holder.attribute(ATTR_PLAYER, "?"),







 holder.attribute(ATTR_SCOPE, "all"), holder.page() + 1);





 return;



 }



 Long reportId = holder.reportId(slot);



 if (reportId != null) {





 click(player);





 gui.openDetail(player, reportId);



 }

 }


private void clickStats(Player player, ReportDeskHolder holder, int slot) {



 switch (slot) {





 case CLOSE_SLOT -> player.closeInventory();





 case BACK_SLOT, HUB_SLOT -> openHub(player);





 case REFRESH_SLOT -> openStats(player);





 case 30 -> {







 click(player);







 gui.openAdminList(player, ReportFilter.all().withOverdueOnly(true), 1);





 }





 default -> {







 // Decorative slot.





 }



 }

 }


private void clickWatchlist(Player player, ReportDeskHolder holder, int slot) {



 if (slot == CLOSE_SLOT) {





 player.closeInventory();





 return;



 }



 if (slot == BACK_SLOT || slot == HUB_SLOT) {





 openHub(player);





 return;



 }



 if (slot == GuiService.TOP_RIGHT_SLOT) {





 player.closeInventory();





 plugin.chatInput().request(player, MessageKeys.ADMIN_GUI_WATCHLIST_ADD_PROMPT,







 Map.of(), text -> {









 if (!player.isOnline()) {











 return;









 }









 if (!plugin.validator().validatePlayerName(text).valid()) {











 send(player, MessageKeys.GENERAL_INVALID_INPUT,













 Map.of("reason", "illegal_characters"));











 return;









 }









 String target = text.trim();









 if (plugin.watchlist().isWatching(player.getUniqueId(), target)) {











 send(player, MessageKeys.ADMIN_GUI_WATCHLIST_ALREADY,













 Map.of("target", target));











 openWatchlist(player, 1);











 return;









 }









 WatchlistService.ToggleResult result = plugin.watchlist().toggle(player, target);









 switch (result) {











 case ADDED -> {













 send(player, MessageKeys.ADMIN_WATCH_ADDED, Map.of("target", target));













 plugin.audit("WATCH", player, 0L, target);











 }











 case LIMIT_REACHED -> send(player, MessageKeys.ADMIN_WATCH_LIMIT, Map.of(













 "limit", String.valueOf(plugin.config().notifications()















 .watchlistMaxTargets())));











 case DISABLED -> send(player, MessageKeys.ADMIN_WATCH_DISABLED, Map.of());











 default -> send(player, MessageKeys.GENERAL_INVALID_INPUT,













 Map.of("reason", "illegal_characters"));









 }









 openWatchlist(player, 1);







 });





 return;



 }



 if (slot == REFRESH_SLOT) {





 openWatchlist(player, holder.page());





 return;



 }



 if (slot == PREVIOUS_SLOT) {





 openWatchlist(player, holder.page() - 1);





 return;



 }



 if (slot == NEXT_SLOT) {





 openWatchlist(player, holder.page() + 1);





 return;



 }



 String target = holder.value(slot);



 if (target == null) {





 return;



 }



 click(player);



 WatchlistService.ToggleResult result = plugin.watchlist().remove(player, target);



 if (result == WatchlistService.ToggleResult.REMOVED) {





 send(player, MessageKeys.ADMIN_WATCH_REMOVED, Map.of("target", target));





 plugin.audit("UNWATCH", player, 0L, target);



 } else if (result == WatchlistService.ToggleResult.DISABLED) {





 send(player, MessageKeys.ADMIN_WATCH_DISABLED, Map.of());



 } else {





 send(player, MessageKeys.ADMIN_WATCH_NOT_FOUND, Map.of("target", target));



 }



 openWatchlist(player, holder.page());

 }


private void clickConfigSections(Player player, ReportDeskHolder holder, int slot) {



 switch (slot) {





 case CLOSE_SLOT -> player.closeInventory();





 case BACK_SLOT, HUB_SLOT -> openHub(player);





 case CONFIG_SEARCH_SLOT -> promptConfigSearch(player);





 case REFRESH_SLOT -> openConfigSections(player, holder.page());





 case PREVIOUS_SLOT -> openConfigSections(player, holder.page() - 1);





 case NEXT_SLOT -> openConfigSections(player, holder.page() + 1);





 case CONFIG_RELOAD_SLOT -> reloadConfig(player);





 default -> {







 String section = holder.value(slot);







 if (section != null) {









 click(player);









 openConfigKeys(player, section, 1);







 }





 }



 }

 }


private void clickConfigKeys(Player player, ReportDeskHolder holder, int slot,
















boolean rightClick, boolean shiftClick) {



 String section = holder.attribute(ATTR_SECTION, "");



 String query = holder.attribute(ATTR_QUERY, "");



 ConfigOrigin origin = ConfigOrigin.of(holder);



 if (slot == CLOSE_SLOT) {





 player.closeInventory();





 return;



 }



 if (slot == HUB_SLOT) {





 openHub(player);





 return;



 }



 if (slot == BACK_SLOT) {





 openConfigSections(player, 1);





 return;



 }



 if (slot == CONFIG_SEARCH_SLOT) {





 promptConfigSearch(player);





 return;



 }



 if (slot == REFRESH_SLOT) {





 openConfigOrigin(player, new ConfigOrigin(section, query, holder.page()));





 return;



 }



 if (slot == PREVIOUS_SLOT) {





 openConfigOrigin(player, new ConfigOrigin(section, query, holder.page() - 1));





 return;



 }



 if (slot == NEXT_SLOT) {





 openConfigOrigin(player, new ConfigOrigin(section, query, holder.page() + 1));





 return;



 }



 String path = holder.value(slot);



 if (path == null) {





 return;



 }



 ConfigKey key = ConfigKeys.find(path).orElse(null);



 if (key == null) {





 send(player, MessageKeys.CONFIG_UNKNOWN, Map.of("key", path));





 return;



 }



 if (rightClick) {





 resetConfig(player, key, origin, false);





 return;



 }



 if (shiftClick) {





 click(player);





 openConfigKeyDetail(player, key, origin);





 return;



 }



 Optional<String> quick = ConfigGuiSupport.quickChange(key, plugin.config());



 if (quick.isPresent()) {





 click(player);





 setConfig(player, key, quick.get(), origin, false);



 } else {





 promptConfig(player, key, origin, false);



 }

 }


private void clickConfigKey(Player player, ReportDeskHolder holder, int slot) {



 String path = holder.attribute(ATTR_PATH, "");



 ConfigKey key = ConfigKeys.find(path).orElse(null);



 if (key == null) {





 send(player, MessageKeys.CONFIG_UNKNOWN, Map.of("key", path));





 return;



 }



 ConfigOrigin origin = ConfigOrigin.of(holder);



 if (slot == CLOSE_SLOT) {





 player.closeInventory();





 return;



 }



 if (slot == HUB_SLOT) {





 openHub(player);





 return;



 }



 if (slot == BACK_SLOT) {





 openConfigOrigin(player, origin);





 return;



 }



 if (slot == REFRESH_SLOT) {





 openConfigKeyDetail(player, key, origin);





 return;



 }



 if (slot == CONFIG_QUICK_SLOT) {





 ConfigGuiSupport.quickChange(key, plugin.config()).ifPresent(next -> {







 click(player);







 setConfig(player, key, next, origin, true);





 });





 return;



 }



 if (slot == CONFIG_EXACT_SLOT) {





 promptConfig(player, key, origin, true);





 return;



 }



 if (slot == CONFIG_RESET_SLOT) {





 resetConfig(player, key, origin, true);





 return;



 }



 long[] steps = ConfigGuiSupport.adjustSteps(key);



 for (int i = 0; i < steps.length; i++) {





 if (slot == CONFIG_MINUS_SLOTS[i]) {







 adjustConfig(player, key, -steps[i], origin);







 return;





 }





 if (slot == CONFIG_PLUS_SLOTS[i]) {







 adjustConfig(player, key, steps[i], origin);







 return;





 }



 }

 }


private void adjustConfig(Player player, ConfigKey key, long delta, ConfigOrigin origin) {



 ConfigGuiSupport.adjustedValue(key, plugin.config(), delta).ifPresentOrElse(value -> {





 click(player);





 setConfig(player, key, value, origin, true);



 }, () -> send(player, MessageKeys.ADMIN_GUI_CONFIG_DETAIL_ADJUST_FAILED, Map.of()));

 }


private void promptConfig(Player player, ConfigKey key, ConfigOrigin origin, boolean detail) {



 player.closeInventory();



 Map<String, String> placeholders = configPlaceholders(key);



 plugin.chatInput().request(player, MessageKeys.ADMIN_GUI_CONFIG_ENTRY_PROMPT,





 placeholders, text -> {







 if (!player.isOnline()) {









 return;







 }







 setConfig(player, key, text, origin, detail);





 });

 }


private void promptConfigSearch(Player player) {



 player.closeInventory();



 plugin.chatInput().request(player, MessageKeys.ADMIN_GUI_CONFIG_SEARCH_PROMPT,





 Map.of(), text -> {







 if (!player.isOnline()) {









 return;







 }







 if (!plugin.validator().validateQuery(text).valid()) {









 send(player, MessageKeys.GENERAL_INVALID_INPUT,











 Map.of("reason", "illegal_characters"));









 openConfigSections(player, 1);









 return;







 }







 openConfigSearch(player, text.trim(), 1);





 });

 }


private void setConfig(Player player, ConfigKey key, String raw, ConfigOrigin origin,













boolean detail) {



 plugin.configEditor().set(key, raw).whenComplete((result, error) ->





 handleConfigResult(player, key, result, error, origin, detail, "CONFIG_SET"));

 }


private void resetConfig(Player player, ConfigKey key, ConfigOrigin origin, boolean detail) {



 plugin.configEditor().reset(key).whenComplete((result, error) ->





 handleConfigResult(player, key, result, error, origin, detail, "CONFIG_RESET"));

 }


private void handleConfigResult(Player player, ConfigKey key, ConfigEditService.Result result,

















 Throwable error, ConfigOrigin origin, boolean detail,

















 String auditAction) {



 gui.runSync(() -> {





 if (!player.isOnline()) {







 return;





 }





 sendConfigResult(player, result, error);





 if (result != null && result.ok()) {







 plugin.audit(auditAction, player, 0L,









 result.key() + "=" + result.newValue());





 }





 if (detail) {







 openConfigKeyDetail(player, key, origin);





 } else {







 openConfigOrigin(player, origin);





 }



 });

 }


private void openConfigOrigin(Player player, ConfigOrigin origin) {



 if (origin.query() != null && !origin.query().isBlank()) {





 openConfigSearch(player, origin.query(), origin.page());



 } else if (origin.section() != null && !origin.section().isBlank()) {





 openConfigKeys(player, origin.section(), origin.page());



 } else {





 openConfigSections(player, Math.max(1, origin.page()));



 }

 }


private void reloadConfig(Player player) {



 if (!require(player, "reportdesk.reload")) {





 return;



 }



 player.closeInventory();



 try {





 plugin.reloadAll();





 send(player, MessageKeys.ADMIN_RELOADED, Map.of());





 plugin.audit("RELOAD", player, 0L, "");



 } catch (RuntimeException exception) {





 plugin.getLogger().warning("Reload failed: " + exception.getMessage());





 send(player, MessageKeys.GENERAL_RELOAD_FAILED, Map.of());



 }

 }


private void sendConfigResult(Player player, ConfigEditService.Result result,
















 Throwable error) {



 if (error != null || result == null) {





 plugin.getLogger().warning("Config GUI action failed: "







 + (error == null ? "no result" : error.getMessage()));





 send(player, MessageKeys.GENERAL_UNKNOWN_ERROR, Map.of());





 gui.play(player, plugin.config().gui().soundError());





 return;



 }



 gui.play(player, result.ok()





 ? plugin.config().gui().soundSuccess()





 : plugin.config().gui().soundError());



 switch (result.status()) {





 case OK -> send(player, result.reset()









 ? MessageKeys.CONFIG_RESET : MessageKeys.CONFIG_APPLIED, Map.of(







 "key", result.key(),







 "old", result.oldValue(),







 "new", result.newValue(),







 "value", result.newValue()





 ));





 case DISABLED -> send(player, MessageKeys.CONFIG_DISABLED, Map.of());





 case LOADING -> send(player, MessageKeys.CONFIG_LOADING, Map.of());





 case BUSY -> send(player, MessageKeys.CONFIG_BUSY, Map.of());





 case UNKNOWN -> send(player, MessageKeys.CONFIG_UNKNOWN,







 Map.of("key", result.key()));





 case INVALID, REJECTED -> send(player, MessageKeys.CONFIG_REJECTED,







 Map.of("reason", result.reason()));





 case WRITE_FAILED -> send(player, MessageKeys.CONFIG_WRITE_FAILED,







 Map.of("reason", result.reason()));



 }

 }


private void clickMisc(Player player, int slot) {



 switch (slot) {





 case CLOSE_SLOT -> player.closeInventory();





 case BACK_SLOT, HUB_SLOT -> openHub(player);





 case 10 -> {







 click(player);







 openAudit(player, 1);





 }





 case 12 -> {







 click(player);







 runExport(player);





 }





 case 14 -> {







 if (!require(player, "reportdesk.admin.delete")) {









 return;







 }







 click(player);







 if (plugin.config().gui().admin().confirmDestructive()) {









 openConfirm(player, ACTION_MAINTENANCE);







 } else {









 runMaintenance(player);







 }





 }





 case 16 -> {







 if (!require(player, "reportdesk.admin.delete")) {









 return;







 }







 click(player);







 if (plugin.config().reports().retentionDays() <= 0) {









 send(player, MessageKeys.ADMIN_PURGE_NONE, Map.of());









 return;







 }







 if (plugin.config().gui().admin().confirmDestructive()) {









 openConfirm(player, ACTION_PURGE);







 } else {









 runPurge(player);







 }





 }





 case 28 -> {







 if (!require(player, "reportdesk.reload")) {









 return;







 }







 player.closeInventory();







 try {









 plugin.reloadAll();









 send(player, MessageKeys.ADMIN_RELOADED, Map.of());









 plugin.audit("RELOAD", player, 0L, "");







 } catch (RuntimeException exception) {









 plugin.getLogger().warning("Reload failed: " + exception.getMessage());









 send(player, MessageKeys.GENERAL_RELOAD_FAILED, Map.of());







 }





 }





 case 30 -> {







 player.closeInventory();







 player.performCommand("reports help");





 }





 default -> {







 // Decorative slot.





 }



 }

 }


private void clickAudit(Player player, ReportDeskHolder holder, int slot) {



 if (slot == CLOSE_SLOT) {





 player.closeInventory();





 return;



 }



 if (slot == BACK_SLOT) {





 openMisc(player);





 return;



 }



 if (slot == REFRESH_SLOT) {





 openAudit(player, holder.page());





 return;



 }



 if (slot == PREVIOUS_SLOT) {





 openAudit(player, holder.page() - 1);





 return;



 }



 if (slot == NEXT_SLOT) {





 openAudit(player, holder.page() + 1);



 }

 }


private void clickConfirm(Player player, ReportDeskHolder holder, int slot) {



 if (slot == CLOSE_SLOT) {





 player.closeInventory();





 return;



 }



 if (slot == BACK_SLOT) {





 openMisc(player);





 return;



 }



 if (slot == 15) {





 openMisc(player);





 return;



 }



 if (slot != 11) {





 return;



 }



 String action = holder.attribute(ATTR_ACTION, "");



 player.closeInventory();



 if (!gui.adminActionAllowed(player)) {





 return;



 }



 if (ACTION_PURGE.equals(action)) {





 runPurge(player);



 } else if (ACTION_MAINTENANCE.equals(action)) {





 runMaintenance(player);



 }

 }


// ---------------------------------------------------------------------

 // Helpers

 // ---------------------------------------------------------------------


private PlayerSummary summary(String name) {



 for (PlayerSummary summary : PlayerSummary.build(plugin.repository().all(),





 onlineNames())) {





 if (summary.name().equalsIgnoreCase(name)) {







 return summary;





 }



 }



 return null;

 }


private List<String> onlineNames() {



 List<String> names = new ArrayList<>();



 for (Player online : Bukkit.getOnlinePlayers()) {





 names.add(online.getName());



 }



 return names;

 }


private String onlineUuid(String name) {



 Player online = Bukkit.getPlayerExact(name);



 return online == null ? "-" : online.getUniqueId().toString();

 }


private int overdueCount() {



 long now = System.currentTimeMillis();



 int count = 0;



 for (Report report : plugin.repository().all()) {





 if (report.isOverdue(now)) {







 count++;





 }



 }



 return count;

 }


/**


* The GUI can only show the first 28 slots, so audit pages use the smaller


* of the configured audit page size and the GUI page size. This keeps the


* rendered page and the previous/next page count aligned.


*/

 private int auditGuiPageSize() {



 return Math.max(1, Math.min(plugin.config().audit().pageSize(),





 Math.min(plugin.config().reports().guiPageSize(), GuiService.listCapacity())));

 }


private String sectionDisplay(String section) {



 return ConfigLabels.sectionLabel(messages(), section);

 }


private Material sectionMaterial(String section) {



 return switch (section) {





 case "general" -> Material.NAME_TAG;





 case "limits" -> Material.IRON_DOOR;





 case "reports" -> Material.BOOK;





 case "security" -> Material.SHIELD;





 case "notifications" -> Material.BELL;





 case "gui" -> Material.PAINTING;





 case "evidence" -> Material.SPYGLASS;





 case "punishments" -> Material.IRON_SWORD;





 case "audit" -> Material.WRITABLE_BOOK;





 case "storage" -> Material.CHEST;





 case "config-editor" -> Material.COMPARATOR;





 default -> Material.PAPER;



 };

 }


private Material configMaterial(ConfigKey key) {



 return switch (key.kind()) {





 case BOOLEAN -> Material.LEVER;





 case INTEGER, LONG -> Material.REPEATER;





 case CHOICE -> Material.BOOKSHELF;





 default -> Material.PAPER;



 };

 }


private void click(Player player) {



 gui.play(player, plugin.config().gui().soundClick());

 }


private boolean require(Player player, String permission) {



 if (player.hasPermission(permission)) {





 return true;



 }



 send(player, MessageKeys.GENERAL_NO_PERMISSION, Map.of());



 return false;

 }


private com.reportdesk.i18n.Messages messages() {



 return plugin.messages();

 }


private void send(Player player, String key, Map<String, String> placeholders) {



 if (player != null && player.isOnline()) {





 player.sendMessage(messages().prefixed(key, placeholders));



 }

 }


private static List<String> wrap(String text, int width) {



 List<String> lines = new ArrayList<>();



 if (text == null || text.isBlank()) {





 return lines;



 }



 String remaining = text.replace('\n', ' ').replace('\r', ' ').trim();



 while (remaining.length() > width) {





 int cut = remaining.lastIndexOf(' ', width);





 if (cut <= 0) {







 cut = width;





 }





 lines.add(remaining.substring(0, cut));





 remaining = remaining.substring(cut).trim();



 }



 if (!remaining.isEmpty()) {





 lines.add(remaining);



 }



 return lines;

 } }
