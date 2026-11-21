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
import com.reportdesk.core.ReportStatus;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired on the main thread after a report's lifecycle status changes (claim, close, reopen,
 * release). The event carries the report <em>before</em> and <em>after</em> the transition so
 * listeners can log or react to the change.
 */
public final class ReportStatusChangedEvent extends Event {

  private static final HandlerList HANDLERS = new HandlerList();

  private final Report report;

  private final ReportStatus from;

  private final ReportStatus to;

  public ReportStatusChangedEvent(Report report, ReportStatus from, ReportStatus to) {

    this.report = report;

    this.from = from == null ? ReportStatus.OPEN : from;

    this.to = to == null ? ReportStatus.OPEN : to;
  }

  @Override
  public HandlerList getHandlers() {

    return HANDLERS;
  }

  public static HandlerList getHandlerList() {

    return HANDLERS;
  }

  /** The report in its new (post-transition) state. */
  public Report getReport() {

    return report;
  }

  public ReportStatus getFrom() {

    return from;
  }

  public ReportStatus getTo() {

    return to;
  }
}
