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

import com.reportdesk.api.PunishmentChannel;
import com.reportdesk.core.Report;
import com.reportdesk.core.Verdict;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * Dispatches closed-report punishment events to registered {@link PunishmentChannel}
 * instances.
 *
 * <p>Registration is explicit and order-preserving. Each channel's callback is wrapped so a
 * throwing channel is logged and skipped without affecting other channels or the caller
 * (AGENTS.md: a misbehaving extension must never break the core path).
 */
public final class PunishmentChannelRegistry {

  private final CopyOnWriteArrayList<PunishmentChannel> channels = new CopyOnWriteArrayList<>();

  private final Logger logger;

  public PunishmentChannelRegistry(Logger logger) {

    this.logger = logger;
  }

  /** Registers a channel; duplicate ids are ignored. Returns true when a new channel was added. */
  public boolean register(PunishmentChannel channel) {

    if (channel == null || channel.id() == null || channel.id().isBlank()) {

      return false;
    }

    for (PunishmentChannel existing : channels) {

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

  public void dispatch(Report report, Verdict verdict, String staffName) {

    for (PunishmentChannel channel : channels) {

      safeRun(channel, channel.id(), () -> channel.onPunished(report, verdict, staffName));
    }
  }

  private void safeRun(PunishmentChannel channel, String id, Runnable action) {

    try {

      action.run();

    } catch (Throwable throwable) {

      logger.warning("Punishment channel '" + id + "' failed: " + throwable.getMessage());
    }
  }
}
