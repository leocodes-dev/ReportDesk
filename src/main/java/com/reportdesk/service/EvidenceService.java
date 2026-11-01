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
import com.reportdesk.ReportDeskPlugin; import com.reportdesk.core.BreakSample; import com.reportdesk.core.ChatSample; import com.reportdesk.core.ChatSelectionSupport; import com.reportdesk.core.Evidence; import com.reportdesk.core.MoveSample; import com.reportdesk.core.Report; import org.bukkit.GameMode; import org.bukkit.Location; import org.bukkit.Material; import org.bukkit.entity.Player; import org.bukkit.inventory.ItemStack; import org.bukkit.inventory.meta.ItemMeta;
import java.util.ArrayDeque; import java.util.ArrayList; import java.util.Deque; import java.util.List; import java.util.Map; import java.util.UUID; import java.util.concurrent.CompletableFuture; import java.util.concurrent.ConcurrentHashMap;
/**
* Optional evidence layer.
*
* <p>Event handlers only append small immutable samples to bounded in-memory
* deques. No file IO, database IO or heavy world scans happen in an event.
* Capture for a report happens on the main thread and only reads player state;
* the resulting JSON is written by {@link com.reportdesk.storage.FileStorage}.</p>
*/ public final class EvidenceService {


public record BreakStats(UUID playerId, String playerName, int count) {

 }


private final ReportDeskPlugin plugin;

 private final Map<UUID, Deque<BreakSample>> breaks = new ConcurrentHashMap<>();

 private final Map<UUID, Deque<MoveSample>> movement = new ConcurrentHashMap<>();

 private final Map<UUID, Deque<ChatSample>> chat = new ConcurrentHashMap<>();

 private final Map<UUID, Long> lastMoveSample = new ConcurrentHashMap<>();

 private final Map<UUID, String> lastKnownNames = new ConcurrentHashMap<>();


public EvidenceService(ReportDeskPlugin plugin) {



 this.plugin = plugin;

 }


public void recordBreak(Player player, org.bukkit.block.Block block) {



 if (!plugin.config().evidence().enabled()) {





 return;



 }



 if (!plugin.config().evidence().blockBreak().enabled()) {





 return;



 }



 if (player == null || block == null || player.hasPermission("reportdesk.bypass.evidence")) {





 return;



 }



 int max = plugin.config().evidence().blockBreak().maxPerPlayer();



 BreakSample sample = new BreakSample(System.currentTimeMillis(), block.getWorld().getName(),





 block.getX(), block.getY(), block.getZ(), block.getType().name());



 Deque<BreakSample> deque = breaks.computeIfAbsent(player.getUniqueId(), ignored -> new ArrayDeque<>());



 synchronized (deque) {





 deque.addFirst(sample);





 while (deque.size() > max) {







 deque.removeLast();





 }



 }



 lastKnownNames.put(player.getUniqueId(), player.getName());

 }


public void recordMove(Player player, Location to) {



 if (!plugin.config().evidence().enabled()) {





 return;



 }



 if (!plugin.config().evidence().movement().enabled()) {





 return;



 }



 if (player == null || to == null || player.hasPermission("reportdesk.bypass.evidence")) {





 return;



 }



 long now = System.currentTimeMillis();



 long interval = plugin.config().evidence().movement().minIntervalTicks() * 50L;



 Long last = lastMoveSample.get(player.getUniqueId());



 if (last != null && now - last < interval) {





 return;



 }



 lastMoveSample.put(player.getUniqueId(), now);



 int max = plugin.config().evidence().movement().maxSamples();



 MoveSample sample = new MoveSample(now, to.getWorld().getName(), to.getX(), to.getY(), to.getZ(),





 player.isOnGround());



 Deque<MoveSample> deque = movement.computeIfAbsent(player.getUniqueId(), ignored -> new ArrayDeque<>());



 synchronized (deque) {





 deque.addFirst(sample);





 while (deque.size() > max) {







 deque.removeLast();





 }



 }



 lastKnownNames.put(player.getUniqueId(), player.getName());

 }


public void recordChat(Player player, String message) {



 if (!plugin.config().evidence().enabled() || !plugin.config().evidence().captureChatLog()) {





 return;



 }



 if (player == null || message == null || message.isBlank()





 || player.hasPermission("reportdesk.bypass.evidence")) {





 return;



 }



 int max = plugin.config().evidence().chatLogSize();



 int maxLength = plugin.config().evidence().chatMaxLength();



 String cleaned = message.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ');



 if (cleaned.length() > maxLength) {





 cleaned = cleaned.substring(0, maxLength);



 }



 ChatSample sample = new ChatSample(System.currentTimeMillis(), player.getName(), cleaned);



 Deque<ChatSample> deque = chat.computeIfAbsent(player.getUniqueId(), ignored -> new ArrayDeque<>());



 synchronized (deque) {





 deque.addFirst(sample);





 while (deque.size() > max) {







 deque.removeLast();





 }



 }



 lastKnownNames.put(player.getUniqueId(), player.getName());

 }


/**


* Snapshot of the captured chat for one player, oldest first. The optional


* chat-report picker uses this bounded, in-memory view only; nothing is


* read from or written to disk here.


*/

 public List<ChatSample> chatHistory(String targetName) {



 if (!plugin.config().evidence().enabled() || !plugin.config().evidence().captureChatLog()) {





 return List.of();



 }



 if (targetName == null || targetName.isBlank()) {





 return List.of();



 }



 List<ChatSample> collected = new ArrayList<>();



 for (Deque<ChatSample> deque : chat.values()) {





 synchronized (deque) {







 collected.addAll(deque);





 }



 }



 return ChatSelectionSupport.chronological(collected, targetName);

 }


public void forget(UUID playerId) {



 if (!plugin.config().evidence().enabled()) {





 return;



 }



 if (playerId == null) {





 return;



 }



 breaks.remove(playerId);



 movement.remove(playerId);



 chat.remove(playerId);



 lastMoveSample.remove(playerId);



 lastKnownNames.remove(playerId);

 }


public Evidence capture(Player target, Report report) {



 if (target == null || report == null) {





 return null;



 }



 var settings = plugin.config().evidence();



 if (!settings.enabled() || !settings.captureOnReport()) {





 return null;



 }



 Location location = target.getLocation();



 List<String> inventory = new ArrayList<>();



 List<String> armor = new ArrayList<>();



 if (settings.captureInventory()) {





 int max = settings.inventoryMaxItems();





 ItemStack[] contents = target.getInventory().getContents();





 for (ItemStack item : contents) {







 if (inventory.size() >= max) {









 break;







 }







 String description = describe(item);







 if (description != null) {









 inventory.add(description);







 }





 }





 if (inventory.size() < max) {







 ItemStack offhand = target.getInventory().getItemInOffHand();







 String description = describe(offhand);







 if (description != null) {









 inventory.add(description);







 }





 }





 for (ItemStack item : target.getInventory().getArmorContents()) {







 String description = describe(item);







 if (description != null) {









 armor.add(description);







 }





 }



 }




return new Evidence(





 1,





 report.id(),





 System.currentTimeMillis(),





 target.getUniqueId(),





 target.getName(),





 true,





 settings.captureLocation() ? location.getWorld().getName() : "",





 settings.captureLocation() ? location.getX() : 0,





 settings.captureLocation() ? location.getY() : 0,





 settings.captureLocation() ? location.getZ() : 0,





 settings.captureLocation() ? location.getYaw() : 0,





 settings.captureLocation() ? location.getPitch() : 0,





 settings.captureGameMode() ? gameMode(target.getGameMode()) : "",





 settings.captureHealth() ? target.getHealth() : 0,





 settings.captureHealth() ? target.getFoodLevel() : 0,





 settings.captureHealth() ? target.getLevel() : 0,





 settings.capturePing() ? target.getPing() : -1,





 inventory,





 armor,





 copyChat(target.getUniqueId()),





 copyBreaks(target.getUniqueId()),





 copyMovement(target.getUniqueId()),





 plugin.config().serverName()



 );

 }


public CompletableFuture<java.util.Optional<Evidence>> load(long reportId) {



 return plugin.storage().loadEvidence(reportId);

 }


/**


* Snapshot for the optional auto-detection task. Runs off the main thread,


* reading only bounded in-memory queues.


*/

 public List<BreakStats> recentBreakStats(int windowSeconds, int threshold) {



 if (!plugin.config().evidence().enabled()) {





 return List.of();



 }



 if (!plugin.config().evidence().autoDetect().enabled()





 || !plugin.config().evidence().blockBreak().enabled()) {





 return List.of();



 }



 long cutoff = System.currentTimeMillis() - Math.max(1, windowSeconds) * 1000L;



 List<BreakStats> result = new ArrayList<>();



 for (Map.Entry<UUID, Deque<BreakSample>> entry : breaks.entrySet()) {





 Deque<BreakSample> deque = entry.getValue();





 int count = 0;





 synchronized (deque) {







 for (BreakSample sample : deque) {









 if (sample.createdAt() >= cutoff) {











 count++;









 }







 }





 }





 if (count >= Math.max(1, threshold)) {







 String name = lastKnownNames.getOrDefault(entry.getKey(), "");







 result.add(new BreakStats(entry.getKey(), name, count));





 }



 }



 return result;

 }


public void shutdown() {



 breaks.clear();



 movement.clear();



 chat.clear();



 lastMoveSample.clear();



 lastKnownNames.clear();

 }


private String describe(ItemStack item) {



 if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) {





 return null;



 }



 StringBuilder builder = new StringBuilder(item.getType().name());



 if (item.getAmount() > 1) {





 builder.append(" x").append(item.getAmount());



 }



 ItemMeta meta = item.getItemMeta();



 if (meta != null) {





 if (meta.hasDisplayName() && meta.displayName() != null) {







 String name = plugin.messages().plain(meta.displayName());







 if (!name.isBlank()) {









 int max = plugin.config().evidence().displayNameMaxLength();









 builder.append(" \"").append(name.length() > max ? name.substring(0, max) : name).append('"');







 }





 }





 if (meta.hasEnchants()) {







 builder.append(" ench=").append(meta.getEnchants().size());





 }



 }



 return builder.toString();

 }


private String gameMode(GameMode mode) {



 return mode == null ? "" : mode.name();

 }


private List<ChatSample> copyChat(UUID playerId) {



 Deque<ChatSample> deque = chat.get(playerId);



 if (deque == null) {





 return List.of();



 }



 synchronized (deque) {





 return List.copyOf(deque);



 }

 }


private List<BreakSample> copyBreaks(UUID playerId) {



 Deque<BreakSample> deque = breaks.get(playerId);



 if (deque == null) {





 return List.of();



 }



 synchronized (deque) {





 return List.copyOf(deque);



 }

 }


private List<MoveSample> copyMovement(UUID playerId) {



 Deque<MoveSample> deque = movement.get(playerId);



 if (deque == null) {





 return List.of();



 }



 synchronized (deque) {





 return List.copyOf(deque);



 }

 } }
