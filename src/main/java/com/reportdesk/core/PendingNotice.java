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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A notification that could not be delivered because the player was offline. The message key and
 * raw placeholder values are stored (not the rendered component) so the notice uses the language
 * active when the player returns.
 */
public record PendingNotice(
    UUID playerId, String messageKey, Map<String, String> placeholders, long createdAt) {

  public PendingNotice {

    playerId = playerId == null ? new UUID(0L, 0L) : playerId;

    messageKey = messageKey == null ? "" : messageKey.trim();

    Map<String, String> copy = new LinkedHashMap<>();

    if (placeholders != null) {

      placeholders.forEach(
          (key, value) -> {
            if (key != null && !key.isBlank()) {

              copy.put(key, value == null ? "" : value);
            }
          });
    }

    placeholders = Collections.unmodifiableMap(copy);

    createdAt = Math.max(0L, createdAt);
  }

  public boolean valid() {

    return !playerId.equals(new UUID(0L, 0L)) && !messageKey.isBlank();
  }
}
