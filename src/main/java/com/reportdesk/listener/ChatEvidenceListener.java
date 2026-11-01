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

package com.reportdesk.listener;
import com.reportdesk.ReportDeskPlugin; import io.papermc.paper.event.player.AsyncChatEvent; import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer; import org.bukkit.entity.Player; import org.bukkit.event.EventHandler; import org.bukkit.event.EventPriority; import org.bukkit.event.Listener;
/**
* Optional chat evidence layer. Registered only when evidence and chat capture
* are both enabled. The handler stores a bounded plain-text sample and never
* performs IO.
*/ public final class ChatEvidenceListener implements Listener {


private final ReportDeskPlugin plugin;


public ChatEvidenceListener(ReportDeskPlugin plugin) {



 this.plugin = plugin;

 }


@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)

 public void onChat(AsyncChatEvent event) {



 if (!plugin.config().evidence().enabled()) {





 return;



 }



 if (!plugin.config().evidence().captureChatLog()) {





 return;



 }



 Player player = event.getPlayer();



 if (player == null) {





 return;



 }



 String message = PlainTextComponentSerializer.plainText().serialize(event.message());



 plugin.evidence().recordChat(player, message);

 } }
