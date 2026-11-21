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

import java.util.UUID;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired on the main thread, just before a player-initiated report is handed to the submission
 * pipeline. Third-party plugins may cancel the event to veto the submission; when cancelled the
 * command / GUI / form layer reports a cancellation to the player instead of creating the report.
 *
 * <p>System-generated reports (auto-detection, watchdog) do not fire this event.
 */
public final class ReportPreSubmitEvent extends Event implements Cancellable {

  private static final HandlerList HANDLERS = new HandlerList();

  private final UUID reporterId;

  private final String reporterName;

  private final String targetName;

  private final String category;

  private final String detail;

  private boolean cancelled = false;

  public ReportPreSubmitEvent(
      UUID reporterId, String reporterName, String targetName, String category, String detail) {

    this.reporterId = reporterId == null ? new UUID(0L, 0L) : reporterId;

    this.reporterName = reporterName == null ? "" : reporterName;

    this.targetName = targetName == null ? "" : targetName;

    this.category = category == null ? "" : category;

    this.detail = detail == null ? "" : detail;
  }

  @Override
  public HandlerList getHandlers() {

    return HANDLERS;
  }

  public static HandlerList getHandlerList() {

    return HANDLERS;
  }

  public UUID getReporterId() {

    return reporterId;
  }

  public String getReporterName() {

    return reporterName;
  }

  public String getTargetName() {

    return targetName;
  }

  public String getCategory() {

    return category;
  }

  public String getDetail() {

    return detail;
  }

  @Override
  public boolean isCancelled() {

    return cancelled;
  }

  @Override
  public void setCancelled(boolean cancelled) {

    this.cancelled = cancelled;
  }
}
