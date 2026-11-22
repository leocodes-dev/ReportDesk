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
import org.bukkit.Bukkit;

/**
 * Cross-core "am I on the server main thread" check.
 *
 * <p>Neither core exposes this in the shared Bukkit API: Paper/Folia add {@code
 * Bukkit#isPrimaryThread()}, while plain Spigot exposes it on the implementation class {@code
 * org.bukkit.craftbukkit.sched.CraftScheduler}. Both surfaces are resolved reflectively once and
 * cached; when neither is present (unknown core or unit tests without a server) the answer is
 * {@code false}, so callers defer their work to the scheduler instead of running it in place.
 */
public final class PrimaryThread {

  private static final Method PAPER_METHOD = resolvePaper();

  private static final Method SPIGOT_METHOD = resolveSpigot();

  private PrimaryThread() {}

  public static boolean isPrimaryThread() {

    if (PAPER_METHOD != null) {

      try {

        return Boolean.TRUE.equals(PAPER_METHOD.invoke(null));
      } catch (Throwable ignored) {

        // Fall through to the Spigot surface (defensive; the two cores are mutually exclusive).
      }
    }

    if (SPIGOT_METHOD != null) {

      try {

        return Boolean.TRUE.equals(SPIGOT_METHOD.invoke(Bukkit.getScheduler()));
      } catch (Throwable ignored) {

        // Scheduler implementation changed; treat as off-main-thread.
      }
    }

    return false;
  }

  /** {@code Bukkit#isPrimaryThread()} exists only on Paper/Folia. */
  private static Method resolvePaper() {

    try {

      return Bukkit.class.getMethod("isPrimaryThread");

    } catch (ReflectiveOperationException | LinkageError ignored) {

      return null;
    }
  }

  /** {@code CraftScheduler#isPrimaryThread()} exists only on Spigot. */
  private static Method resolveSpigot() {

    try {

      Class<?> scheduler = Class.forName("org.bukkit.craftbukkit.sched.CraftScheduler");

      return scheduler.getMethod("isPrimaryThread");

    } catch (ReflectiveOperationException | LinkageError ignored) {

      return null;
    }
  }
}
