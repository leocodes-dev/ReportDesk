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
import com.reportdesk.ReportDeskPlugin; import org.bukkit.entity.Player; import org.bukkit.event.EventHandler; import org.bukkit.event.EventPriority; import org.bukkit.event.Listener; import org.bukkit.event.block.BlockBreakEvent;
/**
* Enhanced evidence layer only. This listener is registered solely when
* {@code evidence.enabled} and {@code evidence.block-break.enabled} are true.
* The handler only appends a small immutable sample to an in-memory deque; no
* IO, world scan or re-computation happens here.
*/ public final class BlockBreakEvidenceListener implements Listener {


private final ReportDeskPlugin plugin;


public BlockBreakEvidenceListener(ReportDeskPlugin plugin) {



 this.plugin = plugin;

 }


@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)

 public void onBlockBreak(BlockBreakEvent event) {



 if (!plugin.config().evidence().blockBreak().enabled()) {





 return;



 }



 if (!plugin.config().evidence().enabled()) {





 return;



 }



 Player player = event.getPlayer();



 if (player == null) {





 return;



 }



 plugin.evidence().recordBreak(player, event.getBlock());

 } }
