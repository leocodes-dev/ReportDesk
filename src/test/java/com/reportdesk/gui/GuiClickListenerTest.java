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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Method;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.junit.jupiter.api.Test;

/**
 * Regression guard for the spectator-mode bug: the server pre-cancels inventory clicks for
 * spectators, so the GUI click handler must not use {@code ignoreCancelled = true}.
 */
class GuiClickListenerTest {

  @Test
  void clickHandlerStillRunsWhenTheServerPreCancelledTheEvent() throws Exception {

    Method method = GuiClickListener.class.getDeclaredMethod("onClick", InventoryClickEvent.class);

    EventHandler handler = method.getAnnotation(EventHandler.class);

    assertNotNull(handler, "onClick must be an event handler");

    assertFalse(
        handler.ignoreCancelled(), "ignoreCancelled=true breaks every GUI button for spectators");
  }
}
