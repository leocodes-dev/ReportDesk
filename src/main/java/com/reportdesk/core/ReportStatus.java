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

package com.reportdesk.core;

import java.util.Locale;

/** Lifecycle state of a report. */
public enum ReportStatus {
  OPEN,

  CLAIMED,

  RESOLVED,

  REJECTED,

  DUPLICATE,

  AUTO_CLOSED;

  public boolean isOpen() {

    return this == OPEN || this == CLAIMED;
  }

  public boolean isTerminal() {

    return !isOpen();
  }

  public static ReportStatus parse(String value, ReportStatus fallback) {

    if (value == null) {

      return fallback;
    }

    String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');

    for (ReportStatus status : values()) {

      if (status.name().equals(normalized)) {

        return status;
      }
    }

    return fallback;
  }
}
