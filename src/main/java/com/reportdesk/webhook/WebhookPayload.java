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

package com.reportdesk.webhook;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Discord-compatible webhook payload. Kept free of Bukkit so JSON escaping can be unit tested. */
public record WebhookPayload(
    String username,
    String avatarUrl,
    String content,
    String title,
    int color,
    List<Field> fields) {

  public record Field(String name, String value, boolean inline) {}

  public WebhookPayload {

    username = username == null ? "ReportDesk" : username;

    avatarUrl = avatarUrl == null ? "" : avatarUrl;

    content = content == null ? "" : content;

    title = title == null ? "" : title;

    fields = fields == null ? List.of() : List.copyOf(fields);
  }

  public Map<String, Object> toMap() {

    Map<String, Object> root = new LinkedHashMap<>();

    root.put("username", truncate(username, 80));

    if (!avatarUrl.isBlank()) {

      root.put("avatar_url", avatarUrl);
    }

    root.put("content", truncate(content, 1900));

    Map<String, Object> embed = new LinkedHashMap<>();

    embed.put("title", truncate(title, 256));

    embed.put("color", color);

    List<Map<String, Object>> embedFields = new ArrayList<>();

    for (Field field : fields) {

      if (field == null || field.name() == null || field.name().isBlank()) {

        continue;
      }

      Map<String, Object> entry = new LinkedHashMap<>();

      entry.put("name", truncate(field.name(), 256));

      entry.put("value", truncate(field.value(), 1024));

      entry.put("inline", field.inline());

      embedFields.add(entry);
    }

    embed.put("fields", embedFields);

    root.put("embeds", List.of(embed));

    return root;
  }

  private static String truncate(String value, int max) {

    if (value == null) {

      return "";
    }

    return value.length() <= max ? value : value.substring(0, Math.max(0, max - 1)) + "…";
  }
}
