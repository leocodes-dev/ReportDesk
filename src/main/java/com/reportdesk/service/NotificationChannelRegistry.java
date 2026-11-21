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

import com.reportdesk.api.NotificationChannel;
import com.reportdesk.core.Report;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * Dispatches report lifecycle events to registered {@link NotificationChannel} instances.
 *
 * <p>Registration is explicit and order-preserving. Each channel's callback is wrapped so a
 * throwing channel is logged and skipped without affecting other channels or the caller
 * (AGENTS.md: a misbehaving extension must never break the core path).
 */
public final class NotificationChannelRegistry {

  private final CopyOnWriteArrayList<NotificationChannel> channels = new CopyOnWriteArrayList<>();

  private final Logger logger;

  public NotificationChannelRegistry(Logger logger) {

    this.logger = logger;
  }

  /** Registers a channel; duplicate ids are ignored. Returns true when a new channel was added. */
  public boolean register(NotificationChannel channel) {

    if (channel == null || channel.id() == null || channel.id().isBlank()) {

      return false;
    }

    for (NotificationChannel existing : channels) {

      if (existing.id().equals(channel.id())) {

        return false;
      }
    }

    channels.add(channel);

    return true;
  }

  /** Removes all channels with the given id. */
  public void unregister(String channelId) {

    channels.removeIf(channel -> channel.id().equals(channelId));
  }

  public int size() {

    return channels.size();
  }

  public void dispatchCreated(Report report, Map<String, String> placeholders) {

    for (NotificationChannel channel : channels) {

      safeRun(
          channel,
          () -> {
            channel.onCreated(report, placeholders);

            return null;
          });
    }
  }

  public void dispatchClaimed(Report report, String staffName, Map<String, String> placeholders) {

    for (NotificationChannel channel : channels) {

      safeRun(
          channel,
          () -> {
            channel.onClaimed(report, staffName, placeholders);

            return null;
          });
    }
  }

  public void dispatchClosed(Report report, String staffName, Map<String, String> placeholders) {

    for (NotificationChannel channel : channels) {

      safeRun(
          channel,
          () -> {
            channel.onClosed(report, staffName, placeholders);

            return null;
          });
    }
  }

  public void dispatchReopened(Report report, String staffName, Map<String, String> placeholders) {

    for (NotificationChannel channel : channels) {

      safeRun(
          channel,
          () -> {
            channel.onReopened(report, staffName, placeholders);

            return null;
          });
    }
  }

  /** Invokes one channel, isolating any exception so it cannot break the dispatch loop. */
  private void safeRun(NotificationChannel channel, java.util.function.Supplier<Void> action) {

    try {

      action.get();

    } catch (Throwable throwable) {

      logger.warning(
          "Notification channel '"
              + channel.id()
              + "' failed: "
              + throwable.getMessage());
    }
  }
}
