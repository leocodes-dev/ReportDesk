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
import org.bukkit.event.EventHandler; import org.bukkit.event.EventPriority; import org.bukkit.event.Listener; import org.bukkit.event.inventory.InventoryClickEvent; import org.bukkit.event.inventory.InventoryDragEvent;
/**
* Dispatches clicks in ReportDesk inventories. Item movement is always
* cancelled. The actual actions live in {@link GuiService}.
*/ public final class GuiClickListener implements Listener {


private final GuiService gui;


public GuiClickListener(GuiService gui) {



 this.gui = gui;

 }


/**


* Clicks inside ReportDesk inventories must always reach the GUI service.


* <p>


* Do <strong>not</strong> use {@code ignoreCancelled = true} here: the server


* pre-cancels inventory clicks for spectators (and other plugins may cancel


* clicks as well), which would silently disable every button for those


* players. The event is cancelled again below, so no item movement is


* possible either way.


*/

 @EventHandler(priority = EventPriority.NORMAL)

 public void onClick(InventoryClickEvent event) {



 if (!(event.getView().getTopInventory().getHolder() instanceof ReportDeskHolder)) {





 return;



 }



 // Re-cancel even when a previous handler (or spectator handling) already did.



 event.setCancelled(true);



 gui.handleClick(event);

 }


@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)

 public void onDrag(InventoryDragEvent event) {



 if (event.getView().getTopInventory().getHolder() instanceof ReportDeskHolder) {





 event.setCancelled(true);



 }

 } }
