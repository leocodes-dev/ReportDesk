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
import com.reportdesk.config.PluginConfig;
import com.reportdesk.core.PendingNotice;
import com.reportdesk.core.Report;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Queues reporter notifications while the reporter is offline and delivers them on the next join.
 * Persistence is asynchronous and never touches the main thread's file system; the in-memory queue
 * is authoritative until saved.
 */
public final class PendingNoticeService {

  private final ReportDeskPlugin plugin;

  private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<PendingNotice>> pending =
      new ConcurrentHashMap<>();

  private final AtomicBoolean ready = new AtomicBoolean(false);

  public PendingNoticeService(ReportDeskPlugin plugin) {

    this.plugin = plugin;
  }

  public boolean enabled() {

    PluginConfig.Notifications settings = plugin.config().notifications();

    return settings.reporterEnabled() && settings.reporterQueueOffline();
  }

  /**
   * Loads persisted notices. Called once during startup; merges with any
   *
   * <p>notices queued in the short window before the load finished.
   */
  public void load() {

    plugin
        .storage()
        .loadPendingNotices()
        .whenComplete(
            (loaded, throwable) -> {
              if (throwable != null) {

                plugin.warning("Failed to load offline notices: " + throwable.getMessage());

                ready.set(true);

                onMain(this::flushOnline);

                return;
              }

              int max = plugin.config().notifications().reporterMaxQueued();

              long cutoff = cutoff();

              for (PendingNotice notice : loaded) {

                if (cutoff > 0L && notice.createdAt() < cutoff) {

                  continue;
                }

                CopyOnWriteArrayList<PendingNotice> queue =
                    pending.computeIfAbsent(
                        notice.playerId(), ignored -> new CopyOnWriteArrayList<>());

                queue.add(notice);

                trim(queue, max);
              }

              for (CopyOnWriteArrayList<PendingNotice> queue : pending.values()) {

                queue.sort(Comparator.comparingLong(PendingNotice::createdAt));
              }

              ready.set(true);

              save();

              onMain(this::flushOnline);
            });
  }

  /**
   * Queues a notification when the player is offline. The caller has already
   *
   * <p>checked that reporter notifications are enabled.
   */
  public void enqueue(Report report, String messageKey, Map<String, String> placeholders) {

    if (!enabled() || report == null || messageKey == null || messageKey.isBlank()) {

      return;
    }

    if (report.reporterId().equals(new UUID(0L, 0L))) {

      return;
    }

    PluginConfig.Notifications settings = plugin.config().notifications();

    PendingNotice notice =
        new PendingNotice(
            report.reporterId(), messageKey, placeholders, System.currentTimeMillis());

    CopyOnWriteArrayList<PendingNotice> queue =
        pending.computeIfAbsent(notice.playerId(), ignored -> new CopyOnWriteArrayList<>());

    queue.add(notice);

    trim(queue, settings.reporterMaxQueued());

    save();
  }

  /**
   * Delivers and removes queued notices for one player. Runs on the main
   *
   * <p>thread because it sends chat messages.
   */
  public void flush(Player player) {

    if (player == null || !ready.get()) {

      return;
    }

    List<PendingNotice> notices = pending.remove(player.getUniqueId());

    if (notices == null || notices.isEmpty()) {

      return;
    }

    long cutoff = cutoff();

    List<PendingNotice> deliver = new ArrayList<>(notices.size());

    for (PendingNotice notice : notices) {

      if (cutoff > 0L && notice.createdAt() < cutoff) {

        continue;
      }

      deliver.add(notice);
    }

    save();

    if (deliver.isEmpty()) {

      return;
    }

    deliver.sort(Comparator.comparingLong(PendingNotice::createdAt));

    for (PendingNotice notice : deliver) {

      player.sendMessage(plugin.messages().prefixed(notice.messageKey(), notice.placeholders()));
    }
  }

  /**
   * Queues are capped per player; the oldest notice is dropped first so the
   *
   * <p>newest updates always survive.
   */
  private void trim(CopyOnWriteArrayList<PendingNotice> queue, int max) {

    int limit = Math.max(1, max);

    while (queue.size() > limit) {

      queue.remove(0);
    }
  }

  private void flushOnline() {

    for (Player online : Bukkit.getOnlinePlayers()) {

      flush(online);
    }
  }

  private void onMain(Runnable task) {

    if (!plugin.isEnabled()) {

      return;
    }

    try {

      Bukkit.getScheduler().runTask(plugin, task);

    } catch (RuntimeException exception) {

      plugin.warning("Could not flush offline notices: " + exception.getMessage());
    }
  }

  private long cutoff() {

    int days = plugin.config().notifications().reporterQueueRetentionDays();

    if (days <= 0) {

      return 0L;
    }

    return System.currentTimeMillis() - days * 86_400_000L;
  }

  /**
   * Persists the current queue on the storage executor. Safe to call from the
   *
   * <p>main thread; never performs IO inline.
   */
  public void save() {

    if (!ready.get()) {

      return;
    }

    List<PendingNotice> snapshot = new ArrayList<>();

    for (CopyOnWriteArrayList<PendingNotice> queue : pending.values()) {

      snapshot.addAll(queue);
    }

    snapshot.sort(Comparator.comparingLong(PendingNotice::createdAt));

    try {

      plugin
          .storage()
          .savePendingNotices(snapshot)
          .whenComplete(
              (ignored, throwable) -> {
                if (throwable != null) {

                  plugin.warning("Failed to save offline notices: " + throwable.getMessage());
                }
              });

    } catch (RuntimeException exception) {

      // The storage executor may already be shut down during plugin disable.

    }
  }
}
