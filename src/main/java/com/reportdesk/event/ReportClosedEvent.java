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
import com.reportdesk.core.Verdict;
import java.util.UUID;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired on the main thread, immediately after an admin closes a report with a verdict. Not
 * cancellable: the closure has already taken effect when this fires.
 *
 * <p>This is the natural hook for ecosystem integrations - for example, running a
 * LuckPerms {@code lp temp remove <target> ...} punishment command for a confirmed
 * ({@link Verdict#RESOLVED}) report, or clearing a flag on a {@link Verdict#REJECTED} one.
 *
 * <p>Thread-safety contract for listeners: events are delivered synchronously on the server's
 * main thread. Listeners must be thread-safe and must not block; schedule heavy work off the
 * main thread.
 */
public final class ReportClosedEvent extends Event {

  private static final HandlerList HANDLERS = new HandlerList();

  private final Report report;

  private final Verdict verdict;

  private final UUID staffId;

  private final String staffName;

  public ReportClosedEvent(Report report, Verdict verdict, UUID staffId, String staffName) {

    this.report = report;

    this.verdict = verdict == null ? Verdict.RESOLVED : verdict;

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

  /** The report in its closed (terminal) state. */
  public Report getReport() {

    return report;
  }

  public Verdict getVerdict() {

    return verdict;
  }

  /** The admin who closed the report (zero UUID when the actor is the console or system). */
  public UUID getStaffId() {

    return staffId;
  }

  public String getStaffName() {

    return staffName;
  }
}
