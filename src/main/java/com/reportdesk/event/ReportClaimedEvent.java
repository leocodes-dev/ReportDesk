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

package com.reportdesk.event;

import com.reportdesk.core.Report;
import java.util.UUID;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired on the main thread, immediately after an admin claims a report. Not cancellable: the
 * claim has already taken effect when this fires.
 *
 * <p>Thread-safety contract for listeners: events are delivered synchronously on the server's
 * main thread. Listeners must be thread-safe and must not block; heavy work (file or database IO)
 * should be scheduled off the main thread by the listener itself.
 */
public final class ReportClaimedEvent extends Event {

  private static final HandlerList HANDLERS = new HandlerList();

  private final Report report;

  private final UUID staffId;

  private final String staffName;

  public ReportClaimedEvent(Report report, UUID staffId, String staffName) {

    this.report = report;

    this.staffId = staffId == null ? new UUID(0L, 0L) : staffId;

    this.staffName = staffName == null ? "" : staffName;
  }

  @Override
  public HandlerList getHandlers() {

    return HANDLERS;
  }

  public static HandlerList getHandlerList() {

    return HANDLERS;
  }

  /** The report now carrying this claim. */
  public Report getReport() {

    return report;
  }

  /** The admin who claimed the report (zero UUID when the actor is the console). */
  public UUID getStaffId() {

    return staffId;
  }

  public String getStaffName() {

    return staffName;
  }
}
