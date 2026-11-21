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
 * Fired on the main thread, immediately after a report has been created and persisted. Not
 * cancellable: by the time this fires the report already exists.
 */
public final class ReportSubmittedEvent extends Event {

  private static final HandlerList HANDLERS = new HandlerList();

  private final Report report;

  private final UUID reporterId;

  private final String source;

  public ReportSubmittedEvent(Report report, UUID reporterId, String source) {

    this.report = report;
    this.reporterId = reporterId == null ? new UUID(0L, 0L) : reporterId;
    this.source = source == null ? "" : source;
  }

  @Override
  public HandlerList getHandlers() {

    return HANDLERS;
  }

  public static HandlerList getHandlerList() {

    return HANDLERS;
  }

  public Report getReport() {

    return report;
  }

  public UUID getReporterId() {

    return reporterId;
  }

  /**
   * Where the submission came from: "command", "gui", "chat", "bedrock-form", "auto" or "system".
   */
  public String getSource() {

    return source;
  }
}
