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

package com.reportdesk.listener;
import com.reportdesk.ReportDeskPlugin; import io.papermc.paper.event.player.AsyncChatEvent; import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer; import org.bukkit.Bukkit; import org.bukkit.entity.Player; import org.bukkit.event.EventHandler; import org.bukkit.event.EventPriority; import org.bukkit.event.Listener;
/**
* Routes a pending GUI/command chat prompt back to the main thread. The chat
* event itself is async, so the callback (which may open inventories or update
* the repository) is always scheduled synchronously.
*/ public final class ChatInputListener implements Listener {


private final ReportDeskPlugin plugin;


public ChatInputListener(ReportDeskPlugin plugin) {



 this.plugin = plugin;

 }


@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)

 public void onChat(AsyncChatEvent event) {



 Player player = event.getPlayer();



 if (player == null || !plugin.chatInput().hasPending(player)) {





 return;



 }



 event.setCancelled(true);



 String text = PlainTextComponentSerializer.plainText().serialize(event.message());



 Bukkit.getScheduler().runTask(plugin, () -> plugin.chatInput().consume(player, text));

 } }
