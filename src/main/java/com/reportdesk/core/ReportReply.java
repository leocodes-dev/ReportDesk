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

import java.util.UUID;

/**
 * One message in a report's conversation thread. Staff replies are visible to the reporter; player
 * replies are visible to staff. Internal staff notes stay in {@link ReportNote} and are never shown
 * to players.
 */
public record ReportReply(
    UUID authorId, String authorName, boolean staff, String text, long createdAt) {

  public ReportReply {

    authorId = authorId == null ? new UUID(0L, 0L) : authorId;

    authorName = authorName == null ? "" : authorName;

    text = text == null ? "" : text.trim();

    createdAt = Math.max(0L, createdAt);
  }
}
