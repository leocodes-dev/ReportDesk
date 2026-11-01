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
import com.reportdesk.ReportDeskPlugin; import org.bukkit.event.EventHandler; import org.bukkit.event.EventPriority; import org.bukkit.event.Listener; import org.bukkit.event.player.PlayerJoinEvent; import org.bukkit.event.player.PlayerQuitEvent;
import java.util.UUID;
/**
* Drops per-player in-memory state when a player leaves. This is a base
* listener: it does not touch storage and is not a high-frequency event.
*/ public final class PlayerSessionListener implements Listener {


private final ReportDeskPlugin plugin;


public PlayerSessionListener(ReportDeskPlugin plugin) {



 this.plugin = plugin;

 }


@EventHandler(priority = EventPriority.MONITOR)

 public void onJoin(PlayerJoinEvent event) {



 plugin.offlineNotices().flush(event.getPlayer());

 }


@EventHandler(priority = EventPriority.MONITOR)

 public void onQuit(PlayerQuitEvent event) {



 UUID playerId = event.getPlayer().getUniqueId();



 plugin.evidence().forget(playerId);



 plugin.chatInput().cancel(event.getPlayer(), false);



 plugin.gui().forget(playerId);

 } }
