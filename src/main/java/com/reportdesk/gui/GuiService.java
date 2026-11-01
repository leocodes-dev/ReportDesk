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

package com.reportdesk.gui;
import com.reportdesk.ReportDeskPlugin; import com.reportdesk.core.BreakSample; import com.reportdesk.core.ChatSample; import com.reportdesk.core.ChatSelectionSupport; import com.reportdesk.core.Evidence; import com.reportdesk.core.MoveSample; import com.reportdesk.core.Page; import com.reportdesk.core.Report; import com.reportdesk.core.ReportCategory; import com.reportdesk.core.ReportChatSelection; import com.reportdesk.core.ReportFilter; import com.reportdesk.core.ReportNote; import com.reportdesk.core.ReportPriority; import com.reportdesk.core.ReportReply; import com.reportdesk.core.ReportStats; import com.reportdesk.core.ReportStatus; import com.reportdesk.core.Verdict; import com.reportdesk.i18n.MessageKeys; import com.reportdesk.service.NotificationService; import com.reportdesk.service.ReportService; import com.reportdesk.util.TimeText; import net.kyori.adventure.text.Component; import org.bukkit.Bukkit; import org.bukkit.Material; import org.bukkit.entity.Player; import org.bukkit.event.inventory.InventoryClickEvent; import org.bukkit.inventory.Inventory; import org.bukkit.inventory.ItemStack; import org.bukkit.inventory.meta.ItemMeta;
import java.util.ArrayList; import java.util.LinkedHashMap; import java.util.LinkedHashSet; import java.util.List; import java.util.Locale; import java.util.Map; import java.util.Set; import java.util.UUID; import java.util.concurrent.ConcurrentHashMap;
/**
* Inventory rendering and actions. All methods run on the main thread except
* {@link #loadEvidence}, which reads the snapshot asynchronously and then
* returns to the main thread before touching an inventory.
*/ public final class GuiService {


private static final int SIZE = 54;

 /**


* Bottom navigation bar, mirroring the layout used by similar admin menus:


* slot 45 previous page, 48 back, 49 refresh, 50 close, 52 hub, 53 next page.


*/

 private static final int PREVIOUS_SLOT = 45;

 private static final int BACK_SLOT = 48;

 private static final int REFRESH_SLOT = 49;

 private static final int CLOSE_SLOT = 50;

 private static final int HUB_SLOT = 52;

 private static final int NEXT_SLOT = 53;

 /** Row above the navigation bar, used for menu-specific action buttons. */

 private static final int ACTION_LEFT_SLOT = 38;

 private static final int ACTION_MIDDLE_SLOT = 40;

 private static final int ACTION_RIGHT_SLOT = 42;

 /** Reply shortcut on the report detail page. */

 private static final int REPLIES_SLOT = 42;

 private static final int EMPTY_SLOT = 22;


/** Header card slot, matching the reference layout used by the admin menus. */

 static final int HEADER_SLOT = 4;

 /** Utility slots on the top border row. */

 static final int TOP_LEFT_SLOT = 0;

 static final int TOP_MIDDLE_SLOT = 2;

 static final int TOP_RIGHT_SLOT = 6;

 static final int TOP_FAR_SLOT = 8;

 /**


* 7 x 4 content grid (28 entries) framed by one row of decoration on each


* side; used by every paged admin list so the layout stays identical.


*/

 static final int[] LIST_GRID = {



 10, 11, 12, 13, 14, 15, 16,



 19, 20, 21, 22, 23, 24, 25,



 28, 29, 30, 31, 32, 33, 34,



 37, 38, 39, 40, 41, 42, 43

 };


private static final int FILTER_STATUS_START = 10;

 private static final int FILTER_CATEGORY_SLOT = 28;

 private static final int FILTER_MINE_SLOT = 30;

 private static final int FILTER_SORT_SLOT = 32;

 private static final int FILTER_PRIORITY_SLOT = 34;

 private static final int FILTER_OVERDUE_SLOT = 36;

 private static final int FILTER_RESET_SLOT = 38;

 private static final ReportStatus[] FILTER_STATUSES = {



 null, ReportStatus.OPEN, ReportStatus.CLAIMED, ReportStatus.RESOLVED,



 ReportStatus.REJECTED, ReportStatus.DUPLICATE, ReportStatus.AUTO_CLOSED

 };


private static final class FlowState {



 private String target = "";



 private String category = "";



 private String detail = "";



 private int page = 1;



 private String query = "";



 private List<ChatSample> chatSamples = List.of();



 private final Set<Integer> selectedChat = new LinkedHashSet<>();



 private boolean chatFlow;




private void resetChat() {





 chatSamples = List.of();





 selectedChat.clear();





 chatFlow = false;



 }




private List<ReportChatSelection> selectedChatSelections() {





 return ChatSelectionSupport.fromSamples(chatSamples, selectedChat);



 }

 }


private static final class ChatReviewState {



 private long reportId;



 private List<ChatSample> samples = List.of();

 }


private static final class AdminState {



 private ReportFilter filter = ReportFilter.all();



 private int page = 1;

 }


private final ReportDeskPlugin plugin;

 private final AdminGuiService adminGui;

 private final Map<UUID, FlowState> flows = new ConcurrentHashMap<>();

 private final Map<UUID, AdminState> adminStates = new ConcurrentHashMap<>();

 private final Map<UUID, ChatReviewState> chatReviews = new ConcurrentHashMap<>();

 private final Map<UUID, Long> lastAdminAction = new ConcurrentHashMap<>();


public GuiService(ReportDeskPlugin plugin) {



 this.plugin = plugin;



 this.adminGui = new AdminGuiService(plugin, this);

 }


public AdminGuiService adminGui() {



 return adminGui;

 }


public void forget(UUID playerId) {



 if (playerId == null) {





 return;



 }



 flows.remove(playerId);



 adminStates.remove(playerId);



 chatReviews.remove(playerId);



 lastAdminAction.remove(playerId);



 adminGui.forget(playerId);

 }


// ---------------------------------------------------------------------

 // Player report flow

 // ---------------------------------------------------------------------


public void beginReport(Player player) {



 if (!plugin.config().gui().enabled()) {





 send(player, MessageKeys.REPORT_GUI_DISABLED, Map.of());





 return;



 }



 FlowState flow = new FlowState();



 flow.category = plugin.config().defaultCategory();



 flows.put(player.getUniqueId(), flow);



 openPlayerSelect(player, 1);

 }


public void beginReport(Player player, String target) {



 if (!plugin.config().gui().enabled()) {





 send(player, MessageKeys.REPORT_GUI_DISABLED, Map.of());





 return;



 }



 FlowState flow = flow(player);



 flow.target = target == null ? "" : target.trim();



 flow.category = plugin.config().defaultCategory();



 flow.detail = "";



 flow.resetChat();



 flows.put(player.getUniqueId(), flow);



 openCategorySelect(player);

 }


private FlowState flow(Player player) {



 return flows.computeIfAbsent(player.getUniqueId(), ignored -> {





 FlowState created = new FlowState();





 created.category = plugin.config().defaultCategory();





 return created;



 });

 }


public void openPlayerSelect(Player player, int requestedPage) {



 FlowState flow = flow(player);



 int pageSize = plugin.config().reports().guiPageSize();



 String query = flow.query.toLowerCase(Locale.ROOT);



 List<String> names = new ArrayList<>();



 for (Player online : Bukkit.getOnlinePlayers()) {





 if (plugin.config().limits().blockSelfReport() && online.getUniqueId().equals(player.getUniqueId())) {







 continue;





 }





 if (query.isEmpty() || online.getName().toLowerCase(Locale.ROOT).contains(query)) {







 names.add(online.getName());





 }



 }



 names.sort(String.CASE_INSENSITIVE_ORDER);



 Page<String> page = Page.of(names, requestedPage, pageSize);



 flow.page = page.page();




ReportDeskHolder holder = ReportDeskHolder.paged(ReportDeskHolder.Type.PLAYER_SELECT, page.page());



 Inventory inventory = create(holder, MessageKeys.REPORT_MENU_TITLE_PLAYERS, Map.of());



 int slot = 0;



 for (String name : page.items()) {





 if (slot >= pageSize) {







 break;





 }





 holder.putValue(slot, name);





 inventory.setItem(slot, item(Material.PLAYER_HEAD,







 plugin.messages().component(MessageKeys.REPORT_PLAYER_ITEM_NAME, Map.of("player", name)),







 plugin.messages().components(MessageKeys.REPORT_PLAYER_ITEM_LORE, Map.of(









 "player", name,









 "online", String.valueOf(Bukkit.getOnlinePlayers().size())







 ))));





 slot++;



 }



 if (page.items().isEmpty()) {





 inventory.setItem(EMPTY_SLOT, item(Material.PAPER,







 plugin.messages().component(MessageKeys.GUI_EMPTY), List.of()));



 }



 inventory.setItem(ACTION_RIGHT_SLOT, item(Material.COMPASS,





 component(MessageKeys.GUI_SEARCH_NAME),





 components(MessageKeys.GUI_SEARCH_LORE)));



 backButton(inventory);



 pageNavigation(inventory, page);



 closeButton(inventory);



 fill(inventory, holder);



 player.openInventory(inventory);

 }


public void openCategorySelect(Player player) {



 FlowState flow = flow(player);



 if (flow.target.isBlank()) {





 send(player, MessageKeys.REPORT_TARGET_REQUIRED, Map.of());





 return;



 }



 ReportDeskHolder holder = ReportDeskHolder.of(ReportDeskHolder.Type.CATEGORY_SELECT);



 Inventory inventory = create(holder, MessageKeys.REPORT_MENU_TITLE_CATEGORIES, Map.of());



 int pageSize = plugin.config().reports().guiPageSize();



 int slot = 0;



 for (ReportCategory category : plugin.config().categories().stream()





 .filter(ReportCategory::enabled)





 .sorted(java.util.Comparator.comparingInt(ReportCategory::priority).reversed()







 .thenComparing(ReportCategory::id))





 .toList()) {





 if (slot >= pageSize) {







 break;





 }





 holder.putValue(slot, category.id());





 List<Component> lore = new ArrayList<>();





 for (String line : category.description()) {







 lore.add(plugin.messages().render(line, Map.of()));





 }





 inventory.setItem(slot, item(material(category.icon(), Material.PAPER),







 plugin.messages().render(category.display(), Map.of()), lore));





 slot++;



 }



 backButton(inventory);



 closeButton(inventory);



 fill(inventory, holder);



 player.openInventory(inventory);

 }


/**


* Freezes the target's captured chat history for this report flow and


* opens the multi-select page. Chat capture itself is read from the bounded


* in-memory buffer only; no IO happens here.


*/

 private void beginChatSelect(Player player) {



 FlowState flow = flow(player);



 if (!chatPickerEnabled(flow)) {





 openConfirm(player);





 return;



 }



 List<ChatSample> history = plugin.evidence().chatHistory(flow.target);



 flow.chatSamples = history;



 flow.selectedChat.clear();



 flow.chatFlow = true;



 if (history.isEmpty()) {





 flow.chatFlow = false;





 play(player, plugin.config().gui().soundError());





 send(player, MessageKeys.REPORT_CHAT_EMPTY, Map.of("target", flow.target));





 openConfirm(player);





 return;



 }



 renderChatSelect(player, 1);

 }


private void renderChatSelect(Player player, int requestedPage) {



 FlowState flow = flow(player);



 if (!flow.chatFlow || !chatPickerEnabled(flow)) {





 openConfirm(player);





 return;



 }



 int pageSize = plugin.config().reports().guiPageSize();



 Page<ChatSample> page = Page.of(flow.chatSamples, requestedPage, pageSize);



 ReportDeskHolder holder = ReportDeskHolder.paged(ReportDeskHolder.Type.CHAT_SELECT, page.page());



 Inventory inventory = create(holder, MessageKeys.REPORT_CHAT_TITLE, Map.of(





 "target", flow.target,





 "page", String.valueOf(page.page())



 ));




int base = (page.page() - 1) * pageSize;



 int slot = 0;



 for (ChatSample sample : page.items()) {





 if (slot >= pageSize) {







 break;





 }





 int index = base + slot;





 boolean selected = flow.selectedChat.contains(index);





 holder.putValue(slot, String.valueOf(index));





 Map<String, String> placeholders = Map.of(







 "player", sample.playerName(),







 "time", TimeText.absolute(sample.createdAt(), plugin.messages()),







 "message", sample.message(),







 "state", plugin.messages().plain(selected









 ? MessageKeys.REPORT_CHAT_STATE_SELECTED









 : MessageKeys.REPORT_CHAT_STATE_UNSELECTED)





 );





 inventory.setItem(slot, item(selected ? Material.WRITTEN_BOOK : Material.PAPER,







 plugin.messages().component(MessageKeys.REPORT_CHAT_ITEM_NAME, placeholders),







 plugin.messages().components(MessageKeys.REPORT_CHAT_ITEM_LORE, placeholders)));





 slot++;



 }



 if (page.items().isEmpty()) {





 inventory.setItem(EMPTY_SLOT, item(Material.PAPER,







 plugin.messages().component(MessageKeys.REPORT_CHAT_EMPTY, Map.of()), List.of()));



 }




backButton(inventory);



 pageNavigation(inventory, page);



 inventory.setItem(ACTION_MIDDLE_SLOT, item(Material.BARRIER,





 component(MessageKeys.REPORT_CHAT_CLEAR_NAME),





 components(MessageKeys.REPORT_CHAT_CLEAR_LORE)));



 inventory.setItem(ACTION_RIGHT_SLOT, item(Material.LIME_CONCRETE,





 plugin.messages().component(MessageKeys.REPORT_CHAT_CONTINUE, Map.of(







 "count", String.valueOf(flow.selectedChat.size()),







 "max", String.valueOf(plugin.config().reports().chatReport().maxSelected())





 )),





 plugin.messages().components(MessageKeys.REPORT_CHAT_CONTINUE_LORE, Map.of(







 "max", String.valueOf(plugin.config().reports().chatReport().maxSelected())





 ))));



 closeButton(inventory);



 fill(inventory, holder);



 player.openInventory(inventory);

 }


private void clickChatSelect(Player player, ReportDeskHolder holder, int slot) {



 if (slot == CLOSE_SLOT) {





 player.closeInventory();





 return;



 }



 FlowState flow = flow(player);



 if (slot == BACK_SLOT) {





 play(player, plugin.config().gui().soundClick());





 openCategorySelect(player);





 return;



 }



 if (slot == PREVIOUS_SLOT) {





 renderChatSelect(player, holder.page() - 1);





 return;



 }



 if (slot == NEXT_SLOT) {





 renderChatSelect(player, holder.page() + 1);





 return;



 }



 if (slot == ACTION_MIDDLE_SLOT) {





 play(player, plugin.config().gui().soundClick());





 flow.selectedChat.clear();





 renderChatSelect(player, holder.page());





 return;



 }



 if (slot == ACTION_RIGHT_SLOT) {





 if (flow.selectedChat.isEmpty()) {







 play(player, plugin.config().gui().soundError());







 send(player, MessageKeys.REPORT_CHAT_SELECT_NONE, Map.of());







 return;





 }





 play(player, plugin.config().gui().soundClick());





 openConfirm(player);





 return;



 }



 String raw = holder.value(slot);



 if (raw == null) {





 return;



 }



 int index;



 try {





 index = Integer.parseInt(raw);



 } catch (NumberFormatException exception) {





 return;



 }



 if (index < 0 || index >= flow.chatSamples.size()) {





 return;



 }



 if (flow.selectedChat.contains(index)) {





 flow.selectedChat.remove(index);





 play(player, plugin.config().gui().soundClick());



 } else {





 int max = plugin.config().reports().chatReport().maxSelected();





 if (flow.selectedChat.size() >= max) {







 play(player, plugin.config().gui().soundError());







 send(player, MessageKeys.REPORT_CHAT_LIMIT, Map.of("limit", String.valueOf(max)));







 return;





 }





 flow.selectedChat.add(index);





 play(player, plugin.config().gui().soundSuccess());



 }



 renderChatSelect(player, holder.page());

 }


private boolean chatPickerEnabled(FlowState flow) {



 if (flow == null || !plugin.config().chatReportAvailable()) {





 return false;



 }



 String configured = plugin.config().reports().chatReport().category();



 return flow.category != null && flow.category.equalsIgnoreCase(configured);

 }


public void openConfirm(Player player) {



 FlowState flow = flows.get(player.getUniqueId());



 if (flow == null || flow.target.isBlank()) {





 send(player, MessageKeys.GENERAL_REPORT_NOT_FOUND, Map.of("id", "?"));





 return;



 }



 ReportDeskHolder holder = ReportDeskHolder.of(ReportDeskHolder.Type.CONFIRM);



 Inventory inventory = create(holder, MessageKeys.REPORT_MENU_TITLE_CONFIRM, Map.of());




inventory.setItem(11, item(Material.PLAYER_HEAD,





 plugin.messages().component(MessageKeys.REPORT_PLAYER_ITEM_NAME, Map.of("player", flow.target)),





 List.of(plugin.messages().component(MessageKeys.REPORT_CONFIRM_TARGET,







 Map.of("target", flow.target)))));




ReportCategory category = plugin.config().category(flow.category)





 .orElseGet(() -> plugin.config().category(plugin.config().defaultCategory())







 .orElse(new ReportCategory("other", "Other", "PAPER", List.of(), true, 0, List.of())));



 inventory.setItem(13, item(material(category.icon(), Material.PAPER),





 plugin.messages().render(category.display(), Map.of()),





 List.of(plugin.messages().component(MessageKeys.REPORT_CONFIRM_CATEGORY,







 Map.of("category", plugin.categoryDisplay(category.id()))))));




List<Component> detailLore = new ArrayList<>();



 if (flow.detail.isBlank()) {





 detailLore.add(plugin.messages().component(MessageKeys.REPORT_CONFIRM_DETAIL_EMPTY));



 } else {





 detailLore.add(plugin.messages().component(MessageKeys.REPORT_CONFIRM_DETAIL,







 Map.of("detail", flow.detail)));



 }



 inventory.setItem(15, item(Material.BOOK, component(MessageKeys.GENERAL_DETAIL), detailLore));




if (flow.chatFlow) {





 List<ReportChatSelection> selections = flow.selectedChatSelections();





 List<Component> chatLore = new ArrayList<>(components(MessageKeys.REPORT_CONFIRM_CHAT_LORE));





 if (selections.isEmpty()) {







 chatLore.add(component(MessageKeys.REPORT_CONFIRM_CHAT_EMPTY));





 } else {







 for (ReportChatSelection selection : selections) {









 chatLore.add(plugin.messages().component(MessageKeys.REPORT_CONFIRM_CHAT_LINE, Map.of(











 "time", TimeText.absolute(selection.createdAt(), plugin.messages()),











 "player", selection.playerName(),











 "message", selection.message()









 )));







 }





 }





 inventory.setItem(22, item(Material.WRITTEN_BOOK,







 plugin.messages().component(MessageKeys.REPORT_CONFIRM_CHAT_NAME, Map.of(









 "count", String.valueOf(selections.size())







 )), chatLore));





 inventory.setItem(27, item(Material.PAPER,







 component(MessageKeys.REPORT_CONFIRM_CHAT_EDIT_NAME),







 components(MessageKeys.REPORT_CONFIRM_CHAT_EDIT_LORE)));



 }




inventory.setItem(29, item(Material.LIME_CONCRETE, component(MessageKeys.REPORT_CONFIRM_SUBMIT),





 List.of(plugin.messages().component(MessageKeys.GENERAL_CONFIRM))));



 inventory.setItem(31, item(Material.WRITABLE_BOOK, component(MessageKeys.REPORT_CONFIRM_EDIT),





 List.of(plugin.messages().component(MessageKeys.REPORT_DETAIL_PROMPT))));



 inventory.setItem(33, item(Material.RED_CONCRETE, component(MessageKeys.REPORT_CONFIRM_CANCEL),





 List.of(plugin.messages().component(MessageKeys.GENERAL_CANCEL))));



 closeButton(inventory);



 fill(inventory, holder);



 player.openInventory(inventory);

 }


// ---------------------------------------------------------------------

 // Admin list / detail

 // ---------------------------------------------------------------------


public void openAdminList(Player player, ReportFilter filter, int requestedPage) {



 AdminState state = adminStates.computeIfAbsent(player.getUniqueId(), ignored -> new AdminState());



 state.filter = filter == null ? ReportFilter.all() : filter;



 state.page = Math.max(1, requestedPage);



 renderAdminList(player, state);

 }


public void openAdminList(Player player) {



 AdminState state = adminStates.computeIfAbsent(player.getUniqueId(), ignored -> new AdminState());



 renderAdminList(player, state);

 }


private void renderAdminList(Player player, AdminState state) {



 if (!plugin.config().gui().enabled()) {





 send(player, MessageKeys.REPORT_GUI_DISABLED, Map.of());





 return;



 }



 int pageSize = Math.min(plugin.config().reports().guiPageSize(), listCapacity());



 Page<Report> page = plugin.repository().page(state.filter, state.page, pageSize);



 state.page = page.page();




ReportDeskHolder holder = ReportDeskHolder.list(state.filter, page.page());



 ReportStats stats = ReportStats.compute(plugin.repository().all());



 String filterLabel = plugin.view().filterLabel(state.filter);



 Map<String, String> placeholders = new LinkedHashMap<>();



 placeholders.put("total", String.valueOf(stats.total()));



 placeholders.put("open", String.valueOf(stats.open()));



 placeholders.put("claimed", String.valueOf(stats.claimed()));



 placeholders.put("closed", String.valueOf(





 stats.total() - stats.open() - stats.claimed()));



 placeholders.put("overdue", String.valueOf(overdueCount()));



 placeholders.put("filter", filterLabel);



 placeholders.put("query", state.filter.query().isBlank() ? "-" : state.filter.query());



 placeholders.put("page", String.valueOf(page.page()));



 placeholders.put("pages", String.valueOf(page.totalPages()));




Inventory inventory = create(holder, MessageKeys.ADMIN_GUI_QUEUE_TITLE, placeholders);



 header(inventory, Material.BELL, MessageKeys.ADMIN_GUI_QUEUE_HEADER_NAME,





 plugin.messages().components(MessageKeys.ADMIN_GUI_QUEUE_HEADER_LORE, placeholders));




Map<String, String> filterPlaceholders = Map.of("status", filterLabel);



 inventory.setItem(TOP_LEFT_SLOT, item(Material.HOPPER,





 plugin.messages().component(MessageKeys.GUI_FILTER_NAME, filterPlaceholders),





 plugin.messages().components(MessageKeys.GUI_FILTER_LORE, filterPlaceholders)));



 inventory.setItem(TOP_MIDDLE_SLOT, item(Material.BOOK,





 plugin.messages().component(MessageKeys.GUI_STATS_NAME),





 plugin.messages().components(MessageKeys.GUI_STATS_LORE, Map.of(







 "total", String.valueOf(stats.total()),







 "open", String.valueOf(stats.open()),







 "claimed", String.valueOf(stats.claimed()),







 "closed", String.valueOf(stats.total() - stats.open() - stats.claimed())





 ))));



 inventory.setItem(TOP_RIGHT_SLOT, item(Material.COMPASS,





 plugin.messages().component(MessageKeys.GUI_SEARCH_NAME),





 plugin.messages().components(MessageKeys.GUI_SEARCH_LORE)));



 inventory.setItem(TOP_FAR_SLOT, item(Material.SPECTRAL_ARROW,





 plugin.messages().component(MessageKeys.ADMIN_GUI_QUICK_NEXT_NAME),





 plugin.messages().components(MessageKeys.ADMIN_GUI_QUICK_NEXT_LORE)));




int index = 0;



 for (Report report : page.items()) {





 if (index >= pageSize) {







 break;





 }





 int slot = listSlot(index);





 holder.putReportId(slot, report.id());





 Map<String, String> itemPlaceholders = plugin.notification().placeholders(report);





 inventory.setItem(slot, item(statusMaterial(report.status()),







 plugin.messages().component(MessageKeys.GUI_LIST_ITEM_NAME, itemPlaceholders),







 plugin.messages().components(MessageKeys.GUI_LIST_ITEM_LORE, itemPlaceholders)));





 index++;



 }



 if (page.items().isEmpty()) {





 inventory.setItem(EMPTY_SLOT, item(Material.PAPER,







 plugin.messages().component(MessageKeys.GUI_EMPTY), List.of()));



 }




backButton(inventory);



 hubButton(inventory);



 refreshButton(inventory);



 pageNavigation(inventory, page);



 closeButton(inventory);



 fill(inventory, holder);



 player.openInventory(inventory);

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


* Filter menu used by the admin list. Every click updates the working filter


* in {@link AdminState} and re-renders this inventory; the back button


* applies it to the list.


*/

 public void openFilter(Player player, ReportFilter filter, int page) {



 AdminState state = adminStates.computeIfAbsent(player.getUniqueId(), ignored -> new AdminState());



 state.filter = filter == null ? ReportFilter.all() : filter;



 state.page = Math.max(1, page);



 renderFilter(player, state);

 }


private void renderFilter(Player player, AdminState state) {



 if (!plugin.config().gui().enabled()) {





 send(player, MessageKeys.REPORT_GUI_DISABLED, Map.of());





 return;



 }



 ReportDeskHolder holder = ReportDeskHolder.filter(state.filter, state.page);



 ReportStats stats = ReportStats.compute(plugin.repository().all());



 Map<String, String> placeholders = new LinkedHashMap<>();



 placeholders.put("total", String.valueOf(stats.total()));



 placeholders.put("open", String.valueOf(stats.open()));



 placeholders.put("claimed", String.valueOf(stats.claimed()));



 placeholders.put("overdue", String.valueOf(overdueCount()));



 placeholders.put("filter", plugin.view().filterLabel(state.filter));



 Inventory inventory = create(holder, MessageKeys.GUI_FILTER_MENU_TITLE, placeholders);




header(inventory, Material.HOPPER, MessageKeys.ADMIN_GUI_FILTER_HEADER_NAME,





 plugin.messages().components(MessageKeys.ADMIN_GUI_FILTER_HEADER_LORE, placeholders));




for (int index = 0; index < FILTER_STATUSES.length; index++) {





 ReportStatus status = FILTER_STATUSES[index];





 int slot = FILTER_STATUS_START + index;





 boolean selected = status == null







 ? state.filter.statuses().isEmpty()







 : state.filter.statuses().contains(status);





 List<Component> lore = new ArrayList<>(components(MessageKeys.GUI_FILTER_STATUS_LORE));





 if (selected) {







 lore.add(component(MessageKeys.GUI_FILTER_SELECTED));





 }





 inventory.setItem(slot, item(







 status == null ? Material.NETHER_STAR : statusMaterial(status),







 status == null ? component(MessageKeys.STATUS_ALL)









 : component(NotificationService.statusKey(status)),







 lore));



 }




String category = state.filter.category().isBlank()





 ? plugin.messages().plain(MessageKeys.GUI_FILTER_ALL_CATEGORIES)





 : plugin.categoryDisplay(state.filter.category());



 inventory.setItem(FILTER_CATEGORY_SLOT, item(Material.CHEST,





 plugin.messages().component(MessageKeys.GUI_FILTER_CATEGORY_NAME,







 Map.of("category", category)),





 components(MessageKeys.GUI_FILTER_CATEGORY_LORE)));




String mine = state.filter.claimedBy() == null





 ? plugin.messages().plain(MessageKeys.GENERAL_DISABLED)





 : plugin.messages().plain(MessageKeys.GENERAL_ENABLED);



 inventory.setItem(FILTER_MINE_SLOT, item(Material.PLAYER_HEAD,





 plugin.messages().component(MessageKeys.GUI_FILTER_MINE_NAME, Map.of("state", mine)),





 components(MessageKeys.GUI_FILTER_MINE_LORE)));




String sort = switch (state.filter.sort()) {





 case OLDEST -> plugin.messages().plain(MessageKeys.GUI_FILTER_SORT_OLDEST);





 case PRIORITY -> plugin.messages().plain(MessageKeys.GUI_FILTER_SORT_PRIORITY);





 default -> plugin.messages().plain(MessageKeys.GUI_FILTER_SORT_NEWEST);



 };



 inventory.setItem(FILTER_SORT_SLOT, item(Material.CLOCK,





 plugin.messages().component(MessageKeys.GUI_FILTER_SORT_NAME, Map.of("sort", sort)),





 components(MessageKeys.GUI_FILTER_SORT_LORE)));




String priority = state.filter.priorities().isEmpty()





 ? plugin.messages().plain(MessageKeys.GUI_FILTER_ALL_PRIORITIES)





 : plugin.messages().plain(NotificationService.priorityKey(







 state.filter.priorities().iterator().next()));



 inventory.setItem(FILTER_PRIORITY_SLOT, item(Material.EXPERIENCE_BOTTLE,





 plugin.messages().component(MessageKeys.GUI_FILTER_PRIORITY_NAME,







 Map.of("priority", priority)),





 components(MessageKeys.GUI_FILTER_PRIORITY_LORE)));




inventory.setItem(FILTER_OVERDUE_SLOT, item(Material.CLOCK,





 plugin.messages().component(MessageKeys.GUI_FILTER_OVERDUE_NAME),





 components(MessageKeys.GUI_FILTER_OVERDUE_LORE, Map.of(







 "state", plugin.messages().plain(state.filter.overdueOnly()









 ? MessageKeys.GENERAL_ENABLED : MessageKeys.GENERAL_DISABLED)





 ))));




inventory.setItem(FILTER_RESET_SLOT, item(Material.BARRIER,





 component(MessageKeys.GUI_FILTER_RESET_NAME),





 components(MessageKeys.GUI_FILTER_RESET_LORE)));



 backButton(inventory);



 closeButton(inventory);



 fill(inventory, holder);



 player.openInventory(inventory);

 }


public void openDetail(Player player, long reportId) {



 Report report = plugin.repository().get(reportId).orElse(null);



 if (report == null) {





 send(player, MessageKeys.GENERAL_REPORT_NOT_FOUND, Map.of("id", String.valueOf(reportId)));





 return;



 }



 openDetail(player, report);

 }


public void openDetail(Player player, Report report) {



 if (report == null) {





 send(player, MessageKeys.GENERAL_REPORT_NOT_FOUND, Map.of("id", "?"));





 return;



 }



 ReportDeskHolder holder = ReportDeskHolder.of(ReportDeskHolder.Type.ADMIN_DETAIL, report.id());



 Map<String, String> placeholders = plugin.notification().placeholders(report);



 placeholders.put("targetOpen",





 String.valueOf(plugin.repository().countOpenForTarget(report.targetName())));



 placeholders.put("targetTotal",





 String.valueOf(plugin.repository().countForTarget(report.targetName())));



 Inventory inventory = create(holder, MessageKeys.GUI_DETAIL_TITLE, placeholders);




inventory.setItem(4, item(Material.BOOK,





 plugin.messages().component(MessageKeys.GUI_DETAIL_INFO_NAME, placeholders),





 plugin.messages().components(MessageKeys.GUI_DETAIL_INFO_LORE, placeholders)));



 inventory.setItem(10, item(Material.WRITABLE_BOOK,





 plugin.messages().component(MessageKeys.GUI_DETAIL_DETAIL_NAME),





 plugin.messages().components(MessageKeys.GUI_DETAIL_DETAIL_LORE, Map.of(







 "detail", report.detail().isBlank()









 ? plugin.messages().plain(MessageKeys.GENERAL_NONE) : report.detail()





 ))));



 inventory.setItem(12, item(Material.SPYGLASS,





 plugin.messages().component(MessageKeys.GUI_DETAIL_EVIDENCE_NAME),





 plugin.messages().components(MessageKeys.GUI_DETAIL_EVIDENCE_LORE, Map.of(







 "state", plugin.messages().plain(plugin.config().evidence().enabled()









 ? MessageKeys.GENERAL_ENABLED : MessageKeys.GENERAL_DISABLED)





 ))));



 inventory.setItem(14, item(Material.PAPER,





 plugin.messages().component(MessageKeys.GUI_DETAIL_NOTES_NAME),





 plugin.messages().components(MessageKeys.GUI_DETAIL_NOTES_LORE, Map.of(







 "count", String.valueOf(report.notes().size())





 ))));



 inventory.setItem(16, item(Material.NAME_TAG,





 plugin.messages().component(MessageKeys.GUI_DETAIL_CLOSE_NOTE_NAME),





 plugin.messages().components(MessageKeys.GUI_DETAIL_CLOSE_NOTE_LORE, Map.of(







 "note", report.closeNote().isBlank()









 ? plugin.messages().plain(MessageKeys.GENERAL_NONE) : report.closeNote()





 ))));




if (!report.chatSelections().isEmpty()) {





 inventory.setItem(24, item(Material.WRITTEN_BOOK,







 plugin.messages().component(MessageKeys.ADMIN_GUI_CHAT_REVIEW_NAME, Map.of(









 "count", String.valueOf(report.chatSelections().size())







 )),







 plugin.messages().components(MessageKeys.ADMIN_GUI_CHAT_REVIEW_LORE, Map.of(









 "count", String.valueOf(report.chatSelections().size())







 ))));



 }




inventory.setItem(28, item(Material.LIME_DYE,





 plugin.messages().component(MessageKeys.GUI_DETAIL_CLAIM_NAME),





 plugin.messages().components(MessageKeys.GUI_DETAIL_CLAIM_LORE)));



 inventory.setItem(29, item(Material.GRAY_DYE,





 plugin.messages().component(MessageKeys.GUI_DETAIL_RELEASE_NAME),





 plugin.messages().components(MessageKeys.GUI_DETAIL_RELEASE_LORE)));



 inventory.setItem(30, item(Material.ENDER_PEARL,





 plugin.messages().component(MessageKeys.GUI_DETAIL_TELEPORT_NAME),





 plugin.messages().components(MessageKeys.GUI_DETAIL_TELEPORT_LORE)));



 inventory.setItem(32, item(Material.REDSTONE,





 plugin.messages().component(MessageKeys.GUI_DETAIL_CLOSE_NAME),





 plugin.messages().components(MessageKeys.GUI_DETAIL_CLOSE_LORE)));



 inventory.setItem(34, item(Material.GREEN_DYE,





 plugin.messages().component(MessageKeys.GUI_DETAIL_REOPEN_NAME),





 plugin.messages().components(MessageKeys.GUI_DETAIL_REOPEN_LORE)));



 inventory.setItem(36, item(Material.SPECTRAL_ARROW,





 plugin.messages().component(MessageKeys.GUI_DETAIL_NEXT_NAME),





 plugin.messages().components(MessageKeys.GUI_DETAIL_NEXT_LORE, Map.of(







 "count", String.valueOf(plugin.repository().openReports().size())





 ))));



 inventory.setItem(40, item(Material.BARRIER,





 plugin.messages().component(MessageKeys.GUI_DETAIL_DELETE_NAME),





 plugin.messages().components(MessageKeys.GUI_DETAIL_DELETE_LORE)));



 inventory.setItem(REPLIES_SLOT, item(Material.WRITABLE_BOOK,





 plugin.messages().component(MessageKeys.GUI_DETAIL_REPLIES_NAME, placeholders),





 plugin.messages().components(MessageKeys.GUI_DETAIL_REPLIES_LORE, placeholders)));



 inventory.setItem(REFRESH_SLOT, item(Material.SUNFLOWER,





 component(MessageKeys.ADMIN_GUI_REFRESH_NAME),





 components(MessageKeys.ADMIN_GUI_REFRESH_LORE)));



 backButton(inventory);



 closeButton(inventory);



 fill(inventory, holder);



 player.openInventory(inventory);

 }


public void openVerdict(Player player, long reportId) {



 Report report = plugin.repository().get(reportId).orElse(null);



 if (report == null) {





 send(player, MessageKeys.GENERAL_REPORT_NOT_FOUND, Map.of("id", String.valueOf(reportId)));





 return;



 }



 ReportDeskHolder holder = ReportDeskHolder.of(ReportDeskHolder.Type.VERDICT, report.id());



 Inventory inventory = create(holder, MessageKeys.GUI_VERDICT_TITLE,





 Map.of("id", String.valueOf(report.id())));



 inventory.setItem(11, item(Material.LIME_CONCRETE,





 component(MessageKeys.GUI_VERDICT_RESOLVED_NAME),





 components(MessageKeys.GUI_VERDICT_RESOLVED_LORE)));



 inventory.setItem(13, item(Material.RED_CONCRETE,





 component(MessageKeys.GUI_VERDICT_REJECTED_NAME),





 components(MessageKeys.GUI_VERDICT_REJECTED_LORE)));



 inventory.setItem(15, item(Material.GRAY_CONCRETE,





 component(MessageKeys.GUI_VERDICT_DUPLICATE_NAME),





 components(MessageKeys.GUI_VERDICT_DUPLICATE_LORE)));



 inventory.setItem(17, item(Material.YELLOW_CONCRETE,





 component(MessageKeys.GUI_VERDICT_INSUFFICIENT_NAME),





 components(MessageKeys.GUI_VERDICT_INSUFFICIENT_LORE)));



 backButton(inventory);



 closeButton(inventory);



 fill(inventory, holder);



 player.openInventory(inventory);

 }


public void openDelete(Player player, long reportId) {



 Report report = plugin.repository().get(reportId).orElse(null);



 if (report == null) {





 send(player, MessageKeys.GENERAL_REPORT_NOT_FOUND, Map.of("id", String.valueOf(reportId)));





 return;



 }



 ReportDeskHolder holder = ReportDeskHolder.of(ReportDeskHolder.Type.DELETE, report.id());



 Inventory inventory = create(holder, MessageKeys.GUI_DELETE_TITLE,





 Map.of("id", String.valueOf(report.id())));



 inventory.setItem(11, item(Material.BARRIER,





 component(MessageKeys.GUI_DELETE_YES_NAME), components(MessageKeys.GUI_DELETE_YES_LORE)));



 inventory.setItem(15, item(Material.LIME_DYE,





 component(MessageKeys.GUI_DELETE_NO_NAME), components(MessageKeys.GUI_DELETE_NO_LORE)));



 backButton(inventory);



 closeButton(inventory);



 fill(inventory, holder);



 player.openInventory(inventory);

 }


public void openNotes(Player player, long reportId, int requestedPage) {



 Report report = plugin.repository().get(reportId).orElse(null);



 if (report == null) {





 send(player, MessageKeys.GENERAL_REPORT_NOT_FOUND, Map.of("id", String.valueOf(reportId)));





 return;



 }



 int pageSize = plugin.config().reports().guiPageSize();



 Page<ReportNote> page = Page.of(report.notes(), requestedPage, pageSize);



 ReportDeskHolder holder = ReportDeskHolder.of(ReportDeskHolder.Type.NOTES, report.id(), page.page());



 Inventory inventory = create(holder, MessageKeys.GUI_NOTES_TITLE,





 Map.of("id", String.valueOf(report.id())));



 int slot = 0;



 for (ReportNote note : page.items()) {





 if (slot >= pageSize) {







 break;





 }





 Map<String, String> placeholders = Map.of(







 "author", note.authorName(),







 "time", TimeText.absolute(note.createdAt(), plugin.messages()),







 "text", note.text()





 );





 inventory.setItem(slot, item(Material.PAPER,







 plugin.messages().component(MessageKeys.GUI_NOTES_ITEM_NAME, placeholders),







 plugin.messages().components(MessageKeys.GUI_NOTES_ITEM_LORE, placeholders)));





 slot++;



 }



 if (page.items().isEmpty()) {





 inventory.setItem(EMPTY_SLOT, item(Material.PAPER,







 plugin.messages().component(MessageKeys.GUI_NOTES_EMPTY), List.of()));



 }



 inventory.setItem(ACTION_MIDDLE_SLOT, item(Material.EMERALD,





 component(MessageKeys.GUI_NOTES_ADD_NAME), components(MessageKeys.GUI_NOTES_ADD_LORE)));



 backButton(inventory);



 pageNavigation(inventory, page);



 closeButton(inventory);



 fill(inventory, holder);



 player.openInventory(inventory);

 }


public void openReplies(Player player, long reportId, int requestedPage) {



 Report report = plugin.repository().get(reportId).orElse(null);



 if (report == null) {





 send(player, MessageKeys.GENERAL_REPORT_NOT_FOUND, Map.of("id", String.valueOf(reportId)));





 return;



 }



 int pageSize = plugin.config().reports().guiPageSize();



 Page<ReportReply> page = Page.of(report.replies(), requestedPage, pageSize);



 ReportDeskHolder holder = ReportDeskHolder.of(ReportDeskHolder.Type.REPLIES,





 report.id(), page.page());



 Map<String, String> title = Map.of(





 "id", String.valueOf(report.id()),





 "target", report.targetName()



 );



 Inventory inventory = create(holder, MessageKeys.GUI_REPLIES_TITLE, title);



 int slot = 0;



 for (ReportReply reply : page.items()) {





 if (slot >= pageSize) {







 break;





 }





 Map<String, String> placeholders = Map.of(







 "author", reply.authorName(),







 "time", TimeText.absolute(reply.createdAt(), plugin.messages()),







 "text", reply.text(),







 "role", plugin.messages().plain(reply.staff()









 ? MessageKeys.GENERAL_STAFF : MessageKeys.GENERAL_REPORTER)





 );





 inventory.setItem(slot, item(reply.staff() ? Material.WRITTEN_BOOK : Material.PAPER,







 plugin.messages().component(MessageKeys.GUI_REPLIES_ITEM_NAME, placeholders),







 plugin.messages().components(MessageKeys.GUI_REPLIES_ITEM_LORE, placeholders)));





 slot++;



 }



 if (page.items().isEmpty()) {





 inventory.setItem(EMPTY_SLOT, item(Material.PAPER,







 plugin.messages().component(MessageKeys.GUI_REPLIES_EMPTY), List.of()));



 }



 inventory.setItem(ACTION_MIDDLE_SLOT, item(Material.EMERALD,





 component(MessageKeys.GUI_REPLIES_ADD_NAME), components(MessageKeys.GUI_REPLIES_ADD_LORE)));



 backButton(inventory);



 pageNavigation(inventory, page);



 closeButton(inventory);



 fill(inventory, holder);



 player.openInventory(inventory);

 }


public void openEvidence(Player player, Report report, Evidence evidence) {



 ReportDeskHolder holder = ReportDeskHolder.of(ReportDeskHolder.Type.EVIDENCE,





 report == null ? 0L : report.id());



 Map<String, String> titlePlaceholders = report == null ? Map.of("id", "?", "target", "")





 : plugin.notification().placeholders(report);



 Inventory inventory = create(holder, MessageKeys.GUI_DETAIL_TITLE, titlePlaceholders);



 if (evidence == null) {





 inventory.setItem(EMPTY_SLOT, item(Material.PAPER,







 plugin.messages().component(MessageKeys.EVIDENCE_NOT_CAPTURED), List.of()));





 closeButton(inventory);





 fill(inventory, holder);





 player.openInventory(inventory);





 return;



 }




inventory.setItem(4, item(Material.PLAYER_HEAD,





 plugin.messages().component(MessageKeys.GENERAL_EVIDENCE),





 plugin.view().evidenceSummary(evidence)));




if (!evidence.world().isBlank()) {





 inventory.setItem(10, item(Material.GRASS_BLOCK,







 plugin.messages().component(MessageKeys.EVIDENCE_LOCATION, Map.of(









 "world", evidence.world(),









 "x", format(evidence.x()),









 "y", format(evidence.y()),









 "z", format(evidence.z())







 )), List.of()));



 }



 if (!evidence.gameMode().isBlank()) {





 inventory.setItem(12, item(Material.COMMAND_BLOCK,







 plugin.messages().component(MessageKeys.EVIDENCE_GAME_MODE,









 Map.of("mode", evidence.gameMode())), List.of()));



 }



 inventory.setItem(14, item(Material.GOLDEN_APPLE,





 plugin.messages().component(MessageKeys.EVIDENCE_HEALTH, Map.of(







 "health", format(evidence.health()),







 "food", String.valueOf(evidence.foodLevel()),







 "level", String.valueOf(evidence.expLevel()),







 "ping", String.valueOf(evidence.ping())





 )), List.of()));




List<Component> inventoryLore = new ArrayList<>();



 for (String line : evidence.inventory()) {





 inventoryLore.add(Component.text(line));



 }



 if (inventoryLore.isEmpty()) {





 inventoryLore.add(plugin.messages().component(MessageKeys.EVIDENCE_INVENTORY_EMPTY));



 }



 inventory.setItem(20, item(Material.CHEST,





 plugin.messages().component(MessageKeys.EVIDENCE_INVENTORY, Map.of(







 "count", String.valueOf(evidence.inventory().size()),







 "items", ""





 )), inventoryLore));



 if (!evidence.armor().isEmpty()) {





 List<Component> armorLore = new ArrayList<>();





 for (String line : evidence.armor()) {







 armorLore.add(Component.text(line));





 }





 inventory.setItem(22, item(Material.IRON_CHESTPLATE,







 plugin.messages().component(MessageKeys.EVIDENCE_ARMOR, Map.of("items", "")), armorLore));



 }




List<Component> chatLore = new ArrayList<>();



 for (ChatSample sample : evidence.chat()) {





 chatLore.add(plugin.messages().component(MessageKeys.EVIDENCE_CHAT_LINE, Map.of(







 "time", TimeText.absolute(sample.createdAt(), plugin.messages()),







 "player", sample.playerName(),







 "message", sample.message()





 )));



 }



 if (chatLore.isEmpty()) {





 chatLore.add(plugin.messages().component(MessageKeys.EVIDENCE_CHAT_EMPTY));



 }



 inventory.setItem(24, item(Material.WRITABLE_BOOK,





 plugin.messages().component(MessageKeys.EVIDENCE_CHAT), chatLore));




List<Component> breakLore = new ArrayList<>();



 for (BreakSample sample : evidence.blockBreaks()) {





 breakLore.add(plugin.messages().component(MessageKeys.EVIDENCE_BLOCK_BREAK_LINE, Map.of(







 "time", TimeText.absolute(sample.createdAt(), plugin.messages()),







 "world", sample.world(),







 "x", String.valueOf(sample.x()),







 "y", String.valueOf(sample.y()),







 "z", String.valueOf(sample.z()),







 "material", sample.material()





 )));



 }



 inventory.setItem(30, item(Material.IRON_PICKAXE,





 plugin.messages().component(MessageKeys.EVIDENCE_BLOCK_BREAK, Map.of(







 "count", String.valueOf(evidence.blockBreaks().size())





 )), breakLore));




List<Component> moveLore = new ArrayList<>();



 for (MoveSample sample : evidence.movement()) {





 moveLore.add(plugin.messages().component(MessageKeys.EVIDENCE_MOVEMENT_LINE, Map.of(







 "time", TimeText.absolute(sample.createdAt(), plugin.messages()),







 "world", sample.world(),







 "x", format(sample.x()),







 "y", format(sample.y()),







 "z", format(sample.z()),







 "ground", plugin.messages().plain(sample.onGround()









 ? MessageKeys.GENERAL_YES : MessageKeys.GENERAL_NO)





 )));



 }



 inventory.setItem(32, item(Material.FEATHER,





 plugin.messages().component(MessageKeys.EVIDENCE_MOVEMENT, Map.of(







 "count", String.valueOf(evidence.movement().size())





 )), moveLore));




backButton(inventory);



 closeButton(inventory);



 fill(inventory, holder);



 player.openInventory(inventory);

 }


/**


* Reads evidence off the main thread and only opens the inventory once the


* snapshot has been loaded.


*/

 public void loadEvidence(Player player, Report report) {



 if (!plugin.config().evidence().enabled()) {





 send(player, MessageKeys.ADMIN_EVIDENCE_DISABLED, Map.of());





 return;



 }



 if (report == null) {





 send(player, MessageKeys.GENERAL_REPORT_NOT_FOUND, Map.of("id", "?"));





 return;



 }



 if (!report.evidence().present()) {





 send(player, MessageKeys.ADMIN_EVIDENCE_EMPTY, Map.of("id", String.valueOf(report.id())));





 return;



 }



 send(player, MessageKeys.GUI_EVIDENCE_LOADING, Map.of());



 plugin.evidence().load(report.id()).whenComplete((loaded, throwable) -> runSync(() -> {





 if (!player.isOnline()) {







 return;





 }





 if (throwable != null) {







 plugin.getLogger().warning("Failed to load evidence #" + report.id() + ": "









 + throwable.getMessage());







 send(player, MessageKeys.ADMIN_EVIDENCE_EMPTY,









 Map.of("id", String.valueOf(report.id())));







 return;





 }





 if (loaded.isEmpty()) {







 send(player, MessageKeys.ADMIN_EVIDENCE_EMPTY,









 Map.of("id", String.valueOf(report.id())));







 return;





 }





 openEvidence(player, report, loaded.get());



 }));

 }


/**


* Loads the persisted evidence snapshot off the main thread, then shows the


* full target chat history with the reporter's selections highlighted.


*/

 public void loadChatReview(Player player, Report report) {



 if (!plugin.config().evidence().enabled()) {





 send(player, MessageKeys.ADMIN_EVIDENCE_DISABLED, Map.of());





 return;



 }



 if (report == null) {





 send(player, MessageKeys.GENERAL_REPORT_NOT_FOUND, Map.of("id", "?"));





 return;



 }



 if (report.chatSelections().isEmpty()) {





 send(player, MessageKeys.ADMIN_GUI_CHAT_EMPTY, Map.of());





 return;



 }



 if (!report.evidence().present()) {





 send(player, MessageKeys.ADMIN_EVIDENCE_EMPTY, Map.of("id", String.valueOf(report.id())));





 return;



 }



 send(player, MessageKeys.GUI_EVIDENCE_LOADING, Map.of());



 plugin.evidence().load(report.id()).whenComplete((loaded, throwable) -> runSync(() -> {





 if (!player.isOnline()) {







 return;





 }





 if (throwable != null) {







 plugin.getLogger().warning("Failed to load chat evidence #" + report.id() + ": "









 + throwable.getMessage());







 send(player, MessageKeys.ADMIN_EVIDENCE_EMPTY,









 Map.of("id", String.valueOf(report.id())));







 return;





 }





 if (loaded.isEmpty()) {







 send(player, MessageKeys.ADMIN_EVIDENCE_EMPTY,









 Map.of("id", String.valueOf(report.id())));







 return;





 }





 openChatReview(player, report, loaded.get(), 1);



 }));

 }


public void openChatReview(Player player, Report report, Evidence evidence, int requestedPage) {



 if (report == null || evidence == null) {





 send(player, MessageKeys.ADMIN_EVIDENCE_EMPTY, Map.of("id", "?"));





 return;



 }



 List<ChatSample> history = ChatSelectionSupport.chronological(





 evidence.chat(), report.targetName());



 ChatReviewState state = chatReviews.computeIfAbsent(player.getUniqueId(),





 ignored -> new ChatReviewState());



 state.reportId = report.id();



 state.samples = history;



 renderChatReview(player, report, state, requestedPage);

 }


private void renderChatReview(Player player, Report report, ChatReviewState state,
















 int requestedPage) {



 int pageSize = plugin.config().reports().guiPageSize();



 Page<ChatSample> page = Page.of(state.samples, requestedPage, pageSize);



 ReportDeskHolder holder = ReportDeskHolder.of(ReportDeskHolder.Type.ADMIN_CHAT,





 report.id(), page.page());



 Map<String, String> title = Map.of(





 "target", report.targetName(),





 "page", String.valueOf(page.page()),





 "pages", String.valueOf(page.totalPages())



 );



 Inventory inventory = create(holder, MessageKeys.ADMIN_GUI_CHAT_TITLE, title);




Map<String, String> summary = Map.of(





 "total", String.valueOf(state.samples.size()),





 "selected", String.valueOf(report.chatSelections().size()),





 "page", String.valueOf(page.page()),





 "pages", String.valueOf(page.totalPages())



 );



 inventory.setItem(4, item(Material.WRITTEN_BOOK,





 plugin.messages().component(MessageKeys.ADMIN_GUI_CHAT_SUMMARY_NAME, summary),





 plugin.messages().components(MessageKeys.ADMIN_GUI_CHAT_SUMMARY_LORE, summary)));




int slot = 0;



 for (ChatSample sample : page.items()) {





 if (slot >= pageSize) {







 break;





 }





 boolean selected = ChatSelectionSupport.isSelected(report.chatSelections(), sample);





 Map<String, String> placeholders = Map.of(







 "player", sample.playerName(),







 "time", TimeText.absolute(sample.createdAt(), plugin.messages()),







 "message", sample.message()





 );





 inventory.setItem(slot, item(selected ? Material.WRITTEN_BOOK : Material.PAPER,







 plugin.messages().component(selected











 ? MessageKeys.ADMIN_GUI_CHAT_SELECTED_NAME











 : MessageKeys.ADMIN_GUI_CHAT_ITEM_NAME,









 placeholders),







 plugin.messages().components(selected











 ? MessageKeys.ADMIN_GUI_CHAT_SELECTED_LORE











 : MessageKeys.ADMIN_GUI_CHAT_ITEM_LORE,









 placeholders)));





 slot++;



 }



 if (page.items().isEmpty()) {





 inventory.setItem(EMPTY_SLOT, item(Material.PAPER,







 plugin.messages().component(MessageKeys.ADMIN_GUI_CHAT_EMPTY), List.of()));



 }




pageNavigation(inventory, page);



 backButton(inventory);



 closeButton(inventory);



 fill(inventory, holder);



 player.openInventory(inventory);

 }


private void clickAdminChat(Player player, ReportDeskHolder holder, int slot) {



 if (slot == CLOSE_SLOT) {





 player.closeInventory();





 return;



 }



 if (slot == BACK_SLOT) {





 play(player, plugin.config().gui().soundClick());





 openDetail(player, holder.reportId());





 return;



 }



 if (slot != PREVIOUS_SLOT && slot != NEXT_SLOT) {





 return;



 }



 Report report = plugin.repository().get(holder.reportId()).orElse(null);



 if (report == null) {





 player.closeInventory();





 send(player, MessageKeys.GENERAL_REPORT_NOT_FOUND,







 Map.of("id", String.valueOf(holder.reportId())));





 return;



 }



 ChatReviewState state = chatReviews.get(player.getUniqueId());



 if (state == null || state.reportId != report.id() || state.samples.isEmpty()) {





 loadChatReview(player, report);





 return;



 }



 int target = slot == PREVIOUS_SLOT ? holder.page() - 1 : holder.page() + 1;



 renderChatReview(player, report, state, target);

 }


public void showStats(Player player) {



 if (player == null) {





 return;



 }



 player.closeInventory();



 plugin.view().sendStats(player);

 }


// ---------------------------------------------------------------------

 // Click handling

 // ---------------------------------------------------------------------


public void handleClick(InventoryClickEvent event) {



 if (!(event.getView().getTopInventory().getHolder() instanceof ReportDeskHolder holder)) {





 return;



 }



 event.setCancelled(true);



 if (!(event.getWhoClicked() instanceof Player player)) {





 return;



 }



 if (event.getClickedInventory() == null





 || !event.getClickedInventory().equals(event.getView().getTopInventory())) {





 return;



 }



 int slot = event.getRawSlot();



 if (slot < 0 || slot >= SIZE) {





 return;



 }



 if (isAdminType(holder.type()) && !player.hasPermission("reportdesk.admin")) {





 player.closeInventory();





 send(player, MessageKeys.GENERAL_NO_PERMISSION, Map.of());





 return;



 }



 switch (holder.type()) {





 case PLAYER_SELECT -> clickPlayerSelect(player, holder, slot);





 case CATEGORY_SELECT -> clickCategorySelect(player, holder, slot);





 case CHAT_SELECT -> clickChatSelect(player, holder, slot);





 case CONFIRM -> clickConfirm(player, slot);





 case ADMIN_LIST -> clickAdminList(player, holder, slot);





 case ADMIN_FILTER -> clickAdminFilter(player, holder, slot);





 case ADMIN_DETAIL -> clickAdminDetail(player, holder, slot);





 case VERDICT -> clickVerdict(player, holder, slot);





 case DELETE -> clickDelete(player, holder, slot);





 case NOTES -> clickNotes(player, holder, slot);





 case EVIDENCE -> clickEvidence(player, holder, slot);





 case REPLIES -> clickReplies(player, holder, slot);





 case ADMIN_CHAT -> clickAdminChat(player, holder, slot);





 case ADMIN_HUB, ADMIN_PLAYERS, ADMIN_PLAYER, ADMIN_PLAYER_REPORTS, ADMIN_STATS,







 ADMIN_WATCHLIST, ADMIN_CONFIG_SECTIONS, ADMIN_CONFIG_KEYS, ADMIN_CONFIG_KEY,







 ADMIN_MISC, ADMIN_AUDIT, ADMIN_CONFIRM ->







 adminGui.handleClick(player, holder, slot, event.isRightClick(),









 event.isShiftClick());



 }

 }


private void clickPlayerSelect(Player player, ReportDeskHolder holder, int slot) {



 if (slot == CLOSE_SLOT) {





 player.closeInventory();





 return;



 }



 if (slot == BACK_SLOT) {





 player.closeInventory();





 send(player, MessageKeys.GENERAL_CANCELLED, Map.of());





 return;



 }



 FlowState flow = flow(player);



 if (slot == ACTION_RIGHT_SLOT) {





 player.closeInventory();





 plugin.chatInput().request(player, MessageKeys.REPORT_SEARCH_PROMPT, Map.of(), text -> {







 if (!player.isOnline()) {









 return;







 }







 if (!plugin.validator().validateQuery(text).valid()) {









 send(player, MessageKeys.GENERAL_INVALID_INPUT,











 Map.of("reason", "illegal_characters"));









 return;







 }







 flow.query = text.trim();







 flow.page = 1;







 openPlayerSelect(player, 1);





 });





 return;



 }



 if (slot == PREVIOUS_SLOT) {





 openPlayerSelect(player, holder.page() - 1);





 return;



 }



 if (slot == NEXT_SLOT) {





 openPlayerSelect(player, holder.page() + 1);





 return;



 }



 String target = holder.value(slot);



 if (target == null) {





 return;



 }



 play(player, plugin.config().gui().soundClick());



 flow.target = target;



 flow.category = plugin.config().defaultCategory();



 flow.detail = "";



 flow.resetChat();



 openCategorySelect(player);

 }


private void clickCategorySelect(Player player, ReportDeskHolder holder, int slot) {



 if (slot == CLOSE_SLOT) {





 player.closeInventory();





 return;



 }



 if (slot == BACK_SLOT) {





 openPlayerSelect(player, flow(player).page);





 return;



 }



 String category = holder.value(slot);



 if (category == null) {





 return;



 }



 play(player, plugin.config().gui().soundClick());



 FlowState flow = flow(player);



 flow.category = category;



 flow.resetChat();



 if (chatPickerEnabled(flow)) {





 beginChatSelect(player);



 } else {





 openConfirm(player);



 }

 }


private void clickConfirm(Player player, int slot) {



 if (slot == CLOSE_SLOT) {





 player.closeInventory();





 return;



 }



 FlowState flow = flow(player);



 switch (slot) {





 case 27 -> {







 if (flow.chatFlow) {









 play(player, plugin.config().gui().soundClick());









 renderChatSelect(player, 1);







 }





 }





 case 29 -> submitFlow(player, flow);





 case 31 -> {







 player.closeInventory();







 plugin.chatInput().request(player, MessageKeys.REPORT_DETAIL_PROMPT, Map.of(), text -> {









 if (!player.isOnline()) {











 return;









 }









 FlowState current = flow(player);









 current.detail = text;









 openConfirm(player);







 });





 }





 case 33 -> {







 flows.remove(player.getUniqueId());







 player.closeInventory();







 send(player, MessageKeys.GENERAL_CANCELLED, Map.of());





 }





 default -> {







 // Decorative slot.





 }



 }

 }


private void submitFlow(Player player, FlowState flow) {



 if (flow.target.isBlank()) {





 send(player, MessageKeys.REPORT_TARGET_REQUIRED, Map.of());





 return;



 }



 boolean bypass = player.hasPermission(plugin.config().limits().bypassPermission());



 List<ReportChatSelection> selections = flow.chatFlow





 ? flow.selectedChatSelections() : List.of();



 var result = plugin.reportService().submit(player, flow.target, flow.category, flow.detail,





 bypass, selections);



 String key = ReportService.messageKey(result.reason());



 Map<String, String> placeholders = new LinkedHashMap<>(result.placeholders());



 placeholders.put("player", flow.target);



 placeholders.put("target", flow.target);



 placeholders.put("category", plugin.categoryDisplay(flow.category));



 if (!result.success() && MessageKeys.GENERAL_INVALID_INPUT.equals(key)) {





 placeholders.putIfAbsent("reason", "illegal_characters");



 }



 if (result.success()) {





 flows.remove(player.getUniqueId());





 play(player, plugin.config().gui().soundSuccess());





 player.closeInventory();



 } else {





 play(player, plugin.config().gui().soundError());



 }



 send(player, key, placeholders);

 }


private void clickAdminList(Player player, ReportDeskHolder holder, int slot) {



 AdminState state = adminStates.computeIfAbsent(player.getUniqueId(), ignored -> new AdminState());



 state.filter = holder.filter();



 state.page = holder.page();



 switch (slot) {





 case CLOSE_SLOT -> player.closeInventory();





 case TOP_LEFT_SLOT -> {







 play(player, plugin.config().gui().soundClick());







 openFilter(player, state.filter, state.page);





 }





 case TOP_MIDDLE_SLOT -> showStats(player);





 case TOP_RIGHT_SLOT -> {







 player.closeInventory();







 plugin.chatInput().request(player, MessageKeys.GUI_SEARCH_PROMPT, Map.of(), text -> {









 if (!player.isOnline()) {











 return;









 }









 if (!plugin.validator().validateQuery(text).valid()) {











 send(player, MessageKeys.GENERAL_INVALID_INPUT,













 Map.of("reason", "illegal_characters"));











 return;









 }









 state.filter = state.filter.withQuery(text.trim());









 state.page = 1;









 renderAdminList(player, state);







 });





 }





 case TOP_FAR_SLOT -> {







 play(player, plugin.config().gui().soundClick());







 Report next = plugin.repository()









 .filter(ReportFilter.ofStatus(ReportStatus.OPEN)











 .withSort(ReportFilter.SortOrder.OLDEST))









 .stream().findFirst()









 .orElseGet(() -> plugin.repository()











 .filter(ReportFilter.ofStatus(ReportStatus.CLAIMED)













 .withSort(ReportFilter.SortOrder.OLDEST))











 .stream().findFirst().orElse(null));







 if (next == null) {









 send(player, MessageKeys.ADMIN_GUI_NO_OPEN, Map.of());







 } else {









 openDetail(player, next.id());







 }





 }





 case BACK_SLOT, HUB_SLOT -> {







 play(player, plugin.config().gui().soundClick());







 adminGui.openHub(player);





 }





 case REFRESH_SLOT -> {







 play(player, plugin.config().gui().soundClick());







 renderAdminList(player, state);





 }





 case PREVIOUS_SLOT -> {







 state.page = Math.max(1, state.page - 1);







 renderAdminList(player, state);





 }





 case NEXT_SLOT -> {







 state.page = state.page + 1;







 renderAdminList(player, state);





 }





 default -> {







 Long reportId = holder.reportId(slot);







 if (reportId != null) {









 play(player, plugin.config().gui().soundClick());









 openDetail(player, reportId);







 }





 }



 }

 }


private void clickAdminFilter(Player player, ReportDeskHolder holder, int slot) {



 AdminState state = adminStates.computeIfAbsent(player.getUniqueId(), ignored -> new AdminState());



 state.filter = holder.filter();



 state.page = holder.page();



 if (slot == CLOSE_SLOT) {





 player.closeInventory();





 return;



 }



 if (slot == BACK_SLOT) {





 play(player, plugin.config().gui().soundClick());





 openAdminList(player, state.filter, state.page);





 return;



 }



 if (slot == FILTER_CATEGORY_SLOT) {





 List<String> ids = enabledCategories().stream().map(ReportCategory::id).toList();





 int current = ids.indexOf(state.filter.category());





 String next;





 if (current < 0) {







 next = ids.isEmpty() ? "" : ids.get(0);





 } else if (current + 1 >= ids.size()) {







 next = "";





 } else {







 next = ids.get(current + 1);





 }





 state.filter = state.filter.withCategory(next);





 state.page = 1;





 play(player, plugin.config().gui().soundClick());





 renderFilter(player, state);





 return;



 }



 if (slot == FILTER_MINE_SLOT) {





 state.filter = state.filter.claimedBy() == null







 ? state.filter.withClaimedBy(player.getUniqueId())







 : state.filter.withClaimedBy(null);





 state.page = 1;





 play(player, plugin.config().gui().soundClick());





 renderFilter(player, state);





 return;



 }



 if (slot == FILTER_SORT_SLOT) {





 state.filter = state.filter.withSort(switch (state.filter.sort()) {







 case NEWEST -> ReportFilter.SortOrder.PRIORITY;







 case PRIORITY -> ReportFilter.SortOrder.OLDEST;







 default -> ReportFilter.SortOrder.NEWEST;





 });





 state.page = 1;





 play(player, plugin.config().gui().soundClick());





 renderFilter(player, state);





 return;



 }



 if (slot == FILTER_PRIORITY_SLOT) {





 state.filter = state.filter.cyclePriority();





 state.page = 1;





 play(player, plugin.config().gui().soundClick());





 renderFilter(player, state);





 return;



 }



 if (slot == FILTER_OVERDUE_SLOT) {





 state.filter = state.filter.withOverdueOnly(!state.filter.overdueOnly());





 state.page = 1;





 play(player, plugin.config().gui().soundClick());





 renderFilter(player, state);





 return;



 }



 if (slot == FILTER_RESET_SLOT) {





 state.filter = ReportFilter.all();





 state.page = 1;





 play(player, plugin.config().gui().soundClick());





 renderFilter(player, state);





 return;



 }



 int index = slot - FILTER_STATUS_START;



 if (index < 0 || index >= FILTER_STATUSES.length) {





 return;



 }



 state.filter = state.filter.withStatus(FILTER_STATUSES[index]);



 state.page = 1;



 play(player, plugin.config().gui().soundClick());



 renderFilter(player, state);

 }


private void clickAdminDetail(Player player, ReportDeskHolder holder, int slot) {



 Report report = plugin.repository().get(holder.reportId()).orElse(null);



 if (report == null) {





 player.closeInventory();





 send(player, MessageKeys.GENERAL_REPORT_NOT_FOUND,







 Map.of("id", String.valueOf(holder.reportId())));





 return;



 }



 switch (slot) {





 case CLOSE_SLOT -> player.closeInventory();





 case BACK_SLOT -> openAdminList(player);





 case 28 -> {







 if (!adminActionAllowed(player)) {









 return;







 }







 ReportService.OpResult result = plugin.reportService().claim(report, player);







 notifyResult(player, result);







 afterDetailAction(player, result);





 }





 case 29 -> {







 if (!adminActionAllowed(player)) {









 return;







 }







 ReportService.OpResult result = plugin.reportService().release(report, player);







 notifyResult(player, result);







 afterDetailAction(player, result);





 }





 case 30 -> {







 if (!requirePermission(player, "reportdesk.admin")) {









 return;







 }







 if (!adminActionAllowed(player)) {









 return;







 }







 Player target = Bukkit.getPlayerExact(report.targetName());







 if (target == null) {









 play(player, plugin.config().gui().soundError());









 send(player, MessageKeys.ADMIN_TARGET_NOT_ONLINE, Map.of());









 return;







 }







 player.teleport(target.getLocation());







 play(player, plugin.config().gui().soundSuccess());







 send(player, MessageKeys.ADMIN_TELEPORTED, Map.of("target", target.getName()));







 plugin.audit("TELEPORT", player, report, target.getName());





 }





 case 32 -> {







 if (!requirePermission(player, "reportdesk.admin.close")) {









 return;







 }







 play(player, plugin.config().gui().soundClick());







 openVerdict(player, report.id());





 }





 case 34 -> {







 if (!requirePermission(player, "reportdesk.admin.close")) {









 return;







 }







 if (!adminActionAllowed(player)) {









 return;







 }







 ReportService.OpResult result = plugin.reportService().reopen(report, player);







 notifyResult(player, result);







 afterDetailAction(player, result);





 }





 case 40 -> {







 if (!requirePermission(player, "reportdesk.admin.delete")) {









 return;







 }







 play(player, plugin.config().gui().soundClick());







 openDelete(player, report.id());





 }





 case 12 -> {







 if (!requirePermission(player, "reportdesk.evidence")) {









 return;







 }







 if (!adminActionAllowed(player)) {









 return;







 }







 plugin.audit("VIEW_EVIDENCE", player, report, "");







 loadEvidence(player, report);





 }





 case 14 -> {







 play(player, plugin.config().gui().soundClick());







 openNotes(player, report.id(), 1);





 }





 case 24 -> {







 if (!requirePermission(player, "reportdesk.evidence")) {









 return;







 }







 if (!adminActionAllowed(player)) {









 return;







 }







 plugin.audit("VIEW_CHAT", player, report, "");







 loadChatReview(player, report);





 }





 case 36 -> {







 play(player, plugin.config().gui().soundClick());







 openNextOpenReport(player, report.id());





 }





 case REPLIES_SLOT -> {







 play(player, plugin.config().gui().soundClick());







 openReplies(player, report.id(), 1);





 }





 case REFRESH_SLOT -> {







 play(player, plugin.config().gui().soundClick());







 openDetail(player, report.id());





 }





 default -> {







 // Decorative slot.





 }



 }

 }


private void openNextOpenReport(Player player, long currentId) {



 Report next = firstOpenReport(ReportStatus.OPEN, currentId);



 if (next == null) {





 next = firstOpenReport(ReportStatus.CLAIMED, currentId);



 }



 if (next == null) {





 send(player, MessageKeys.GENERAL_NO_OPEN_REPORTS, Map.of());





 return;



 }



 openDetail(player, next);

 }


private Report firstOpenReport(ReportStatus status, long excludeId) {



 return plugin.repository().filter(ReportFilter.ofStatus(status)







 .withSort(ReportFilter.SortOrder.OLDEST))





 .stream()





 .filter(report -> report.id() != excludeId)





 .findFirst()





 .orElse(null);

 }


private List<ReportCategory> enabledCategories() {



 return plugin.config().categories().stream()





 .filter(ReportCategory::enabled)





 .sorted(java.util.Comparator.comparingInt(ReportCategory::priority).reversed()







 .thenComparing(ReportCategory::id))





 .toList();

 }


private void clickVerdict(Player player, ReportDeskHolder holder, int slot) {



 if (slot == CLOSE_SLOT) {





 player.closeInventory();





 return;



 }



 if (slot == BACK_SLOT) {





 openDetail(player, holder.reportId());





 return;



 }



 Verdict verdict = switch (slot) {





 case 11 -> Verdict.RESOLVED;





 case 13 -> Verdict.REJECTED;





 case 15 -> Verdict.DUPLICATE;





 case 17 -> Verdict.INSUFFICIENT;





 default -> null;



 };



 if (verdict == null) {





 return;



 }



 if (!requirePermission(player, "reportdesk.admin.close")) {





 return;



 }



 if (!adminActionAllowed(player)) {





 return;



 }



 Report report = plugin.repository().get(holder.reportId()).orElse(null);



 ReportService.OpResult result = plugin.reportService().close(report, verdict, player, "");



 notifyResult(player, result);



 afterDetailAction(player, result);

 }


private void clickDelete(Player player, ReportDeskHolder holder, int slot) {



 if (slot == CLOSE_SLOT) {





 player.closeInventory();





 return;



 }



 if (slot == BACK_SLOT || slot == 15) {





 openDetail(player, holder.reportId());





 return;



 }



 if (slot != 11) {





 return;



 }



 if (!requirePermission(player, "reportdesk.admin.delete")) {





 return;



 }



 if (!adminActionAllowed(player)) {





 return;



 }



 Report report = plugin.repository().get(holder.reportId()).orElse(null);



 ReportService.OpResult result = plugin.reportService().delete(report, player);



 notifyResult(player, result);



 if (result.success()) {





 player.closeInventory();



 }

 }


private void clickNotes(Player player, ReportDeskHolder holder, int slot) {



 if (slot == CLOSE_SLOT) {





 player.closeInventory();





 return;



 }



 if (slot == BACK_SLOT) {





 openDetail(player, holder.reportId());





 return;



 }



 if (slot == ACTION_MIDDLE_SLOT) {





 player.closeInventory();





 plugin.chatInput().request(player, MessageKeys.GUI_NOTES_ADD_PROMPT, Map.of(), text -> {







 if (!player.isOnline()) {









 return;







 }







 if (!adminActionAllowed(player)) {









 return;







 }







 Report report = plugin.repository().get(holder.reportId()).orElse(null);







 ReportService.OpResult result = plugin.reportService().addNote(report, player, text);







 notifyResult(player, result);







 if (result.success()) {









 openNotes(player, holder.reportId(), holder.page());







 }





 });





 return;



 }



 if (slot == PREVIOUS_SLOT) {





 openNotes(player, holder.reportId(), holder.page() - 1);





 return;



 }



 if (slot == NEXT_SLOT) {





 openNotes(player, holder.reportId(), holder.page() + 1);



 }

 }


private void clickReplies(Player player, ReportDeskHolder holder, int slot) {



 if (slot == CLOSE_SLOT) {





 player.closeInventory();





 return;



 }



 if (slot == BACK_SLOT) {





 openDetail(player, holder.reportId());





 return;



 }



 if (slot == ACTION_MIDDLE_SLOT) {





 player.closeInventory();





 plugin.chatInput().request(player, MessageKeys.GUI_REPLIES_ADD_PROMPT, Map.of(), text -> {







 if (!player.isOnline() || !adminActionAllowed(player)) {









 return;







 }







 Report report = plugin.repository().get(holder.reportId()).orElse(null);







 ReportService.OpResult result = plugin.reportService().addReply(report, player, text, true);







 notifyResult(player, result);







 if (result.success()) {









 openReplies(player, holder.reportId(), holder.page());







 }





 });





 return;



 }



 if (slot == PREVIOUS_SLOT) {





 openReplies(player, holder.reportId(), holder.page() - 1);





 return;



 }



 if (slot == NEXT_SLOT) {





 openReplies(player, holder.reportId(), holder.page() + 1);



 }

 }


private void clickEvidence(Player player, ReportDeskHolder holder, int slot) {



 if (slot == CLOSE_SLOT) {





 player.closeInventory();





 return;



 }



 if (slot == BACK_SLOT) {





 openDetail(player, holder.reportId());



 }

 }


private void afterDetailAction(Player player, ReportService.OpResult result) {



 if (!result.success() || result.report() == null) {





 return;



 }



 if (plugin.config().gui().closeOnAction()) {





 player.closeInventory();



 } else {





 openDetail(player, result.report());



 }

 }


private void notifyResult(Player player, ReportService.OpResult result) {



 play(player, result.success()





 ? plugin.config().gui().soundSuccess() : plugin.config().gui().soundError());



 send(player, result.messageKey(), result.placeholders());

 }


boolean adminActionAllowed(Player player) {



 if (!player.hasPermission("reportdesk.admin")) {





 send(player, MessageKeys.GENERAL_NO_PERMISSION, Map.of());





 return false;



 }



 long cooldown = plugin.config().security().adminCooldownMillis();



 long now = System.currentTimeMillis();



 Long last = lastAdminAction.get(player.getUniqueId());



 if (cooldown > 0 && last != null && now - last < cooldown) {





 send(player, MessageKeys.GENERAL_ACTION_COOLDOWN, Map.of());





 return false;



 }



 lastAdminAction.put(player.getUniqueId(), now);



 return true;

 }


private boolean requirePermission(Player player, String permission) {



 if (player.hasPermission(permission)) {





 return true;



 }



 send(player, MessageKeys.GENERAL_NO_PERMISSION, Map.of());



 return false;

 }


private boolean isAdminType(ReportDeskHolder.Type type) {



 return switch (type) {





 case ADMIN_LIST, ADMIN_FILTER, ADMIN_DETAIL, VERDICT, DELETE, NOTES, EVIDENCE, REPLIES,







 ADMIN_HUB, ADMIN_PLAYERS, ADMIN_PLAYER, ADMIN_PLAYER_REPORTS, ADMIN_STATS,







 ADMIN_WATCHLIST, ADMIN_CONFIG_SECTIONS, ADMIN_CONFIG_KEYS, ADMIN_CONFIG_KEY,







 ADMIN_MISC, ADMIN_AUDIT, ADMIN_CONFIRM, ADMIN_CHAT -> true;





 default -> false;



 };

 }


// ---------------------------------------------------------------------

 // Inventory building helpers

 // ---------------------------------------------------------------------


Inventory create(ReportDeskHolder holder, String titleKey, Map<String, String> placeholders) {



 Inventory inventory = Bukkit.createInventory(holder, SIZE,





 plugin.messages().component(titleKey, placeholders));



 holder.setInventory(inventory);



 return inventory;

 }


void pageNavigation(Inventory inventory, Page<?> page) {



 pageNavigation(inventory, page.page(), page.totalPages());

 }


/**


* Bottom-row page arrows: previous on slot 45, next on slot 53. The page


* counter lives in the arrow lore so the middle of the bar stays free for


* back/refresh/close.


*/

 void pageNavigation(Inventory inventory, int page, int totalPages) {



 Map<String, String> placeholders = Map.of(





 "page", String.valueOf(page),





 "pages", String.valueOf(totalPages)



 );



 if (page > 1) {





 List<Component> lore = new ArrayList<>(







 plugin.messages().components(MessageKeys.GENERAL_PREVIOUS_LORE));





 lore.add(plugin.messages().component(MessageKeys.GUI_PAGE_NAME, placeholders));





 inventory.setItem(PREVIOUS_SLOT, item(Material.ARROW,







 component(MessageKeys.GENERAL_PREVIOUS), lore));



 }



 if (page < totalPages) {





 List<Component> lore = new ArrayList<>(







 plugin.messages().components(MessageKeys.GENERAL_NEXT_LORE));





 lore.add(plugin.messages().component(MessageKeys.GUI_PAGE_NAME, placeholders));





 inventory.setItem(NEXT_SLOT, item(Material.ARROW,







 component(MessageKeys.GENERAL_NEXT), lore));



 }

 }


void closeButton(Inventory inventory) {



 inventory.setItem(CLOSE_SLOT, item(Material.BARRIER,





 component(MessageKeys.GENERAL_CLOSE), List.of()));

 }


/** Capacity of {@link #LIST_GRID}; all paged lists share the same page size. */

 static int listCapacity() {



 return LIST_GRID.length;

 }


static int listSlot(int index) {



 return LIST_GRID[Math.max(0, Math.min(index, LIST_GRID.length - 1))];

 }


void backButton(Inventory inventory) {



 backButton(inventory, MessageKeys.GENERAL_BACK);

 }


void backButton(Inventory inventory, String messageKey) {



 inventory.setItem(BACK_SLOT, item(Material.OAK_DOOR,





 component(messageKey), List.of()));

 }


void refreshButton(Inventory inventory) {



 inventory.setItem(REFRESH_SLOT, item(Material.SUNFLOWER,





 component(MessageKeys.ADMIN_GUI_REFRESH_NAME),





 components(MessageKeys.ADMIN_GUI_REFRESH_LORE)));

 }


void hubButton(Inventory inventory) {



 inventory.setItem(HUB_SLOT, item(Material.COMPASS,





 component(MessageKeys.ADMIN_GUI_HUB_BACK), List.of()));

 }


void header(Inventory inventory, Material material, String nameKey, List<Component> lore) {



 inventory.setItem(HEADER_SLOT, item(material, component(nameKey), lore));

 }


void header(Inventory inventory, Material material, String nameKey,







 List<Component> lore, Map<String, String> placeholders) {



 inventory.setItem(HEADER_SLOT, item(material,





 plugin.messages().component(nameKey, placeholders), lore));

 }


void fill(Inventory inventory, ReportDeskHolder holder) {



 if (!plugin.config().gui().fillEmptySlots()) {





 return;



 }



 ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, Component.text(" "), List.of());



 for (int slot = 0; slot < inventory.getSize(); slot++) {





 if (slot == CLOSE_SLOT) {







 continue;





 }





 if (inventory.getItem(slot) == null) {







 inventory.setItem(slot, filler);





 }



 }

 }


ItemStack item(Material material, Component name, List<Component> lore) {



 ItemStack stack = new ItemStack(material == null ? Material.PAPER : material);



 ItemMeta meta = stack.getItemMeta();



 if (meta != null) {





 meta.displayName(name == null ? Component.empty() : name);





 if (lore != null && !lore.isEmpty()) {







 meta.lore(lore);





 }





 stack.setItemMeta(meta);



 }



 return stack;

 }


private Component component(String key) {



 return plugin.messages().component(key);

 }


private List<Component> components(String key) {



 return plugin.messages().components(key, Map.of());

 }


private List<Component> components(String key, Map<String, String> placeholders) {



 return plugin.messages().components(key, placeholders);

 }


Material statusMaterial(ReportStatus status) {



 return switch (status) {





 case OPEN -> Material.LIME_DYE;





 case CLAIMED -> Material.CYAN_DYE;





 case RESOLVED -> Material.GREEN_DYE;





 case REJECTED -> Material.RED_DYE;





 case DUPLICATE -> Material.GRAY_DYE;





 case AUTO_CLOSED -> Material.BLACK_DYE;



 };

 }


private Material material(String name, Material fallback) {



 if (name == null || name.isBlank()) {





 return fallback;



 }



 try {





 return Material.valueOf(name.trim().toUpperCase(Locale.ROOT));



 } catch (IllegalArgumentException exception) {





 return fallback;



 }

 }


private void send(Player player, String key, Map<String, String> placeholders) {



 if (player != null && player.isOnline()) {





 player.sendMessage(plugin.messages().prefixed(key, placeholders));



 }

 }


void play(Player player, String sound) {



 if (player == null || sound == null || sound.isBlank()





 || !plugin.config().gui().soundsEnabled()) {





 return;



 }



 try {





 player.playSound(player.getLocation(), sound, 0.7f, 1.0f);



 } catch (RuntimeException ignored) {





 // Invalid configured sound should never break the GUI.



 }

 }


void runSync(Runnable task) {



 if (!plugin.isEnabled()) {





 return;



 }



 try {





 Bukkit.getScheduler().runTask(plugin, task);



 } catch (RuntimeException exception) {





 plugin.getLogger().warning("Could not run GUI task: " + exception.getMessage());



 }

 }


private static String format(double value) {



 return String.format(Locale.ROOT, "%.1f", value);

 } }
