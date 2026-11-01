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

package com.reportdesk.service;

import com.reportdesk.ReportDeskPlugin;
import com.reportdesk.i18n.MessageKeys;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.bukkit.entity.Player;

/**
 * One pending chat input per player (report detail, search, staff note). Timeout tasks only touch
 * an in-memory map.
 */
public final class ChatInputService {

  private record Pending(
      String promptKey,
      java.util.Map<String, String> placeholders,
      long expiresAt,
      Consumer<String> callback) {}

  private final ReportDeskPlugin plugin;

  private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();

  public ChatInputService(ReportDeskPlugin plugin) {

    this.plugin = plugin;
  }

  public void request(
      Player player,
      String promptKey,
      java.util.Map<String, String> placeholders,
      Consumer<String> callback) {

    if (player == null || callback == null) {

      return;
    }

    long timeoutMillis = plugin.config().security().pendingTimeoutSeconds() * 1000L;

    pending.put(
        player.getUniqueId(),
        new Pending(
            promptKey,
            placeholders == null ? java.util.Map.of() : java.util.Map.copyOf(placeholders),
            System.currentTimeMillis() + timeoutMillis,
            callback));

    player.sendMessage(
        plugin
            .messages()
            .prefixed(promptKey, placeholders == null ? java.util.Map.of() : placeholders));
  }

  public boolean hasPending(Player player) {

    return player != null && pending.containsKey(player.getUniqueId());
  }

  public void consume(Player player, String text) {

    if (player == null) {

      return;
    }

    Pending current = pending.remove(player.getUniqueId());

    if (current == null) {

      return;
    }

    String trimmed = text == null ? "" : text.trim();

    if (trimmed.equalsIgnoreCase("cancel")) {

      player.sendMessage(
          plugin.messages().prefixed(MessageKeys.GENERAL_CANCELLED, java.util.Map.of()));

      return;
    }

    if (trimmed.isEmpty()) {

      player.sendMessage(
          plugin
              .messages()
              .prefixed(
                  MessageKeys.GENERAL_INVALID_INPUT,
                  java.util.Map.of("reason", plugin.messages().plain(MessageKeys.GENERAL_NONE))));

      return;
    }

    try {

      current.callback().accept(trimmed);

    } catch (RuntimeException exception) {

      plugin.getLogger().warning("Chat input callback failed: " + exception.getMessage());

      player.sendMessage(
          plugin.messages().prefixed(MessageKeys.GENERAL_UNKNOWN_ERROR, java.util.Map.of()));
    }
  }

  public void cancel(Player player, boolean notify) {

    if (player == null) {

      return;
    }

    Pending removed = pending.remove(player.getUniqueId());

    if (notify && removed != null) {

      player.sendMessage(
          plugin.messages().prefixed(MessageKeys.GENERAL_CANCELLED, java.util.Map.of()));
    }
  }

  public void expire() {

    long now = System.currentTimeMillis();

    Iterator<Map.Entry<UUID, Pending>> iterator = pending.entrySet().iterator();

    while (iterator.hasNext()) {

      Map.Entry<UUID, Pending> entry = iterator.next();

      if (entry.getValue().expiresAt() <= now) {

        iterator.remove();

        Player player = plugin.getServer().getPlayer(entry.getKey());

        if (player != null && player.isOnline()) {

          player.sendMessage(
              plugin.messages().prefixed(MessageKeys.GENERAL_INPUT_TIMEOUT, java.util.Map.of()));
        }
      }
    }
  }

  public void clear() {

    pending.clear();
  }

  public int size() {

    return pending.size();
  }
}
