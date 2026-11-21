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

package com.reportdesk.util;

import java.lang.reflect.Method;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Central scheduler adapter that runs identically on Paper and Folia.
 *
 * <p>On plain Paper every task runs on the single main thread via
 * {@link org.bukkit.BukkitScheduler}. Folia (a region-threaded fork) keeps that legacy API
 * working, but entity/world state must be touched from the owning region thread. This adapter
 * routes "act on a specific player" tasks to the player's region when running on Folia, and falls
 * back to the legacy scheduler otherwise.
 *
 * <p>Folia is detected and driven <b>entirely through reflection</b> (no compile-time Folia
 * dependency, so the project still targets plain Paper). Detection looks for a server method
 * named {@code getRegionScheduler}; if present, an entity-task method is resolved by signature
 * (a 2-arg method whose first parameter accepts the entity and whose second is a
 * {@link Runnable}/{@link java.util.concurrent.Callable}). If anything fails to resolve, every
 * call degrades to the legacy {@code Bukkit.getScheduler()} path, which Folia still supports.
 *
 * <p><b>Threading note</b>: tasks submitted here are delivered on the appropriate thread
 * (region thread on Folia, main thread on Paper). Callers that need Bukkit state still defer
 * their own work accordingly.
 */
public final class TaskScheduler {

  private final Plugin plugin;

  /** Present only on Folia (or Folia-compatible) servers; null on plain Paper. */
  private final boolean folia;

  private final Object regionScheduler;

  private final Method entityTaskMethod;

  public TaskScheduler(Plugin plugin) {

    this.plugin = plugin;

    Method regionSchedulerGetter = null;

    Object regionSchedulerInstance = null;

    Method entityTask = null;

    boolean isFolia = false;

    try {
      Object server = Bukkit.getServer();

      if (server != null) {

        Method getRegionScheduler = findMethod(server.getClass(), "getRegionScheduler");

        if (getRegionScheduler != null) {

          isFolia = true;

          regionSchedulerInstance = getRegionScheduler.invoke(server);

          if (regionSchedulerInstance != null) {

            entityTask = findEntityTaskMethod(regionSchedulerInstance);
          }
        }
      }
    } catch (Throwable ignored) {
      // Not on Folia, or the reflection surface is unavailable: stay on the legacy scheduler.
    }

    this.folia = isFolia;

    this.regionScheduler = regionSchedulerInstance;

    this.entityTaskMethod = entityTask;
  }

  private static Method findMethod(Class<?> clazz, String name) {

    try {

      return clazz.getMethod(name);

    } catch (Throwable ignored) {

      return null;
    }
  }

  /** Resolves the region scheduler method that schedules a task on an entity's region. */
  private static Method findEntityTaskMethod(Object regionScheduler) {

    Class<?> clazz = regionScheduler.getClass();

    String[] names = {"runTaskForEntity", "scheduleTaskForEntity", "runTask"};

    for (String name : names) {

      for (Method method : clazz.getMethods()) {

        if (!method.getName().equals(name) || method.getParameterCount() != 2) {

          continue;
        }

        Class<?> first = method.getParameterTypes()[0];

        Class<?> second = method.getParameterTypes()[1];

        boolean firstAcceptsEntity =
            org.bukkit.entity.Entity.class.isAssignableFrom(first) || first == Object.class;

        boolean secondIsTask =
            Runnable.class.isAssignableFrom(second)
                || java.util.concurrent.Callable.class.isAssignableFrom(second);

        if (firstAcceptsEntity && secondIsTask) {

          return method;
        }
      }
    }

    return null;
  }

  // -------------------------------------------------------------------
  // Public API
  // -------------------------------------------------------------------

  /**
   * Schedules a task that will act on a specific player. On Folia this runs on the player's
   * region thread; on Paper (or when the player is offline) it runs on the main thread.
   */
  public void forPlayer(UUID playerUuid, Runnable task) {

    Player player = playerUuid == null ? null : Bukkit.getPlayer(playerUuid);

    if (player != null) {

      forEntity(player, task);

      return;
    }

    mainTask(task);
  }

  /** Schedules a task bound to a specific entity (region-aware on Folia). */
  public void forEntity(org.bukkit.entity.Entity entity, Runnable task) {

    if (folia && entityTaskMethod != null && regionScheduler != null) {

      try {

        entityTaskMethod.invoke(regionScheduler, entity, task);

        return;
      } catch (Throwable ignored) {

        // Fall through to the legacy scheduler.
      }
    }

    mainTask(task);
  }

  /** Schedules a task on the main thread (region-agnostic). */
  public void mainTask(Runnable task) {

    Bukkit.getScheduler().runTask(plugin, task);
  }

  /** Schedules an async task (no entity/world access; safe on both Paper and Folia). */
  public void asyncTask(Runnable task) {

    Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
  }

  /**
   * Whether the server is Folia (or Folia-compatible with a region scheduler).
   */
  public boolean isFolia() {

    return folia;
  }
}
