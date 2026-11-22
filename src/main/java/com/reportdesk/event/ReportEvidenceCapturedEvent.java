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

import com.reportdesk.core.Evidence;
import com.reportdesk.core.Report;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired on the main thread, after the optional evidence layer captures a snapshot for a newly
 * submitted report. Not cancellable: the capture has already happened. Only fires when the
 * evidence layer is enabled and a live target was available; system reports with no captured
 * snapshot do not fire it.
 *
 * <p>Thread-safety contract for listeners: events are delivered synchronously on the server's
 * main thread. Listeners must be thread-safe and must not block; schedule heavy work off the
 * main thread.
 */
public final class ReportEvidenceCapturedEvent extends Event {

  private static final HandlerList HANDLERS = new HandlerList();

  private final Report report;

  private final Evidence evidence;

  public ReportEvidenceCapturedEvent(Report report, Evidence evidence) {

    this.report = report;

    this.evidence = evidence;
  }

  @Override
  public HandlerList getHandlers() {

    return HANDLERS;
  }

  public static HandlerList getHandlerList() {

    return HANDLERS;
  }

  /** The report the evidence snapshot belongs to. */
  public Report getReport() {

    return report;
  }

  /** The captured evidence; null-safe by construction but may be null if the capture returned none. */
  public Evidence getEvidence() {

    return evidence;
  }
}
