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
import com.reportdesk.ReportDeskPlugin; import com.reportdesk.config.PluginConfig; import com.reportdesk.core.Report; import com.reportdesk.core.WatchEntry; import org.bukkit.entity.Player;
import java.util.ArrayList; import java.util.Comparator; import java.util.HashSet; import java.util.List; import java.util.Locale; import java.util.Set; import java.util.UUID; import java.util.concurrent.ConcurrentHashMap; import java.util.concurrent.CopyOnWriteArrayList; import java.util.concurrent.atomic.AtomicBoolean;
/**
* Persistent staff watchlist. Watching a player name is opt-in per staff member
* and only produces notifications; it never changes report data or permissions.
*/ public final class WatchlistService {


public enum ToggleResult {



 ADDED,



 REMOVED,



 NOT_WATCHED,



 LIMIT_REACHED,



 DISABLED,



 INVALID

 }


private final ReportDeskPlugin plugin;

 private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<WatchEntry>> watches =



 new ConcurrentHashMap<>();

 private final AtomicBoolean ready = new AtomicBoolean(false);


public WatchlistService(ReportDeskPlugin plugin) {



 this.plugin = plugin;

 }


public boolean enabled() {



 return plugin.config().notifications().watchlistEnabled();

 }


/**


* Loads persisted watches once during startup. Duplicate names are merged


* and the configured per-staff cap is enforced defensively.


*/

 public void load() {



 plugin.storage().loadWatchlist().whenComplete((loaded, throwable) -> {





 if (throwable != null) {







 plugin.warning("Failed to load watchlist: " + throwable.getMessage());







 ready.set(true);







 return;





 }





 int max = plugin.config().notifications().watchlistMaxTargets();





 for (WatchEntry entry : loaded) {







 CopyOnWriteArrayList<WatchEntry> list =









 watches.computeIfAbsent(entry.staffId(), ignored -> new CopyOnWriteArrayList<>());







 if (contains(list, entry.targetKey()) || list.size() >= max) {









 continue;







 }







 list.add(entry);





 }





 for (CopyOnWriteArrayList<WatchEntry> list : watches.values()) {







 list.sort(Comparator.comparingLong(WatchEntry::createdAt));





 }





 ready.set(true);





 save();



 });

 }


public ToggleResult toggle(Player staff, String targetName) {



 if (!enabled()) {





 return ToggleResult.DISABLED;



 }



 if (staff == null || targetName == null || targetName.isBlank()) {





 return ToggleResult.INVALID;



 }



 String target = targetName.trim();



 String key = target.toLowerCase(Locale.ROOT);



 CopyOnWriteArrayList<WatchEntry> list =





 watches.computeIfAbsent(staff.getUniqueId(), ignored -> new CopyOnWriteArrayList<>());



 for (WatchEntry entry : list) {





 if (entry.targetKey().equals(key)) {







 list.remove(entry);







 if (list.isEmpty()) {









 watches.remove(staff.getUniqueId(), list);







 }







 save();







 return ToggleResult.REMOVED;





 }



 }



 if (list.size() >= plugin.config().notifications().watchlistMaxTargets()) {





 return ToggleResult.LIMIT_REACHED;



 }



 list.add(new WatchEntry(staff.getUniqueId(), target, System.currentTimeMillis()));



 save();



 return ToggleResult.ADDED;

 }


/**


* Removes an existing watch without accidentally adding one.


*/

 public ToggleResult remove(Player staff, String targetName) {



 if (!enabled()) {





 return ToggleResult.DISABLED;



 }



 if (staff == null || targetName == null || targetName.isBlank()) {





 return ToggleResult.INVALID;



 }



 String key = targetName.trim().toLowerCase(Locale.ROOT);



 CopyOnWriteArrayList<WatchEntry> list = watches.get(staff.getUniqueId());



 if (list == null) {





 return ToggleResult.NOT_WATCHED;



 }



 for (WatchEntry entry : list) {





 if (entry.targetKey().equals(key)) {







 list.remove(entry);







 if (list.isEmpty()) {









 watches.remove(staff.getUniqueId(), list);







 }







 save();







 return ToggleResult.REMOVED;





 }



 }



 return ToggleResult.NOT_WATCHED;

 }


public boolean isWatching(UUID staffId, String targetName) {



if (staffId == null || targetName == null) {





 return false;



 }



 List<WatchEntry> list = watches.get(staffId);



 return list != null && contains(list, targetName.trim().toLowerCase(Locale.ROOT));

 }


public List<WatchEntry> watchedBy(UUID staffId) {



 List<WatchEntry> list = staffId == null ? null : watches.get(staffId);



 if (list == null || list.isEmpty()) {





 return List.of();



 }



 List<WatchEntry> copy = new ArrayList<>(list);



 copy.sort(Comparator.comparingLong(WatchEntry::createdAt).reversed());



 return List.copyOf(copy);

 }


/** Online-independent set of staff ids following this target. */

 public Set<UUID> watchersOf(String targetName) {



 if (targetName == null || targetName.isBlank()) {





 return Set.of();



 }



 String key = targetName.trim().toLowerCase(Locale.ROOT);



 Set<UUID> result = new HashSet<>();



 for (var entry : watches.entrySet()) {





 if (contains(entry.getValue(), key)) {







 result.add(entry.getKey());





 }



 }



 return Set.copyOf(result);

 }


/**


* Sends a focused alert to online staff who follow this target. The normal


* staff notification still runs; this is an additional, opt-in signal.


*/

 public void onReportCreated(Report report) {



 PluginConfig.Notifications settings = plugin.config().notifications();



 if (!settings.watchlistEnabled() || !settings.watchlistNotifyOnReport() || report == null) {





 return;



 }



 Set<UUID> watchers = watchersOf(report.targetName());



 if (watchers.isEmpty()) {





 return;



 }



 plugin.notification().onWatchedReport(report, watchers);

 }


private boolean contains(List<WatchEntry> list, String key) {



 for (WatchEntry entry : list) {





 if (entry.targetKey().equals(key)) {







 return true;





 }



 }



 return false;

 }


/**


* Persists the watchlist on the storage executor. Never blocks the caller.


*/

 public void save() {



 if (!ready.get()) {





 return;



 }



 List<WatchEntry> snapshot = new ArrayList<>();



 for (CopyOnWriteArrayList<WatchEntry> list : watches.values()) {





 snapshot.addAll(list);



 }



 snapshot.sort(Comparator.comparingLong(WatchEntry::createdAt));



 try {





 plugin.storage().saveWatchlist(snapshot).whenComplete((ignored, throwable) -> {







 if (throwable != null) {









 plugin.warning("Failed to save watchlist: " + throwable.getMessage());







 }





 });



 } catch (RuntimeException exception) {





 // The storage executor may already be shut down during plugin disable.



 }

 } }
