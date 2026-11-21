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

package com.reportdesk.api;

import com.reportdesk.core.Report;
import com.reportdesk.core.Verdict;

/**
 * SPI for third-party punishment executors (economy fines, jail plugins, custom
 * demotion chains, etc.).
 *
 * <p>Implement this interface in your plugin and register it with
 * {@link com.reportdesk.ReportDeskPlugin#punishmentChannelRegistry()}:
 *
 * <pre>{@code
 * public class EconomyFine implements PunishmentChannel {
 *   public String id() { return "economy-fine"; }
 *   public void onPunished(Report report, Verdict verdict, String staffName) {
 *     // deduct coins when verdict == RESOLVED
 *   }
 * }
 *
 * // In your plugin's onEnable:
 * ReportDeskPlugin.reportDesk().punishmentChannelRegistry()
 *     .register(new EconomyFine());
 * }</pre>
 *
 * <p>Channels run <em>after</em> the built-in command template execution. A single
 * failing channel never blocks other channels or the core close flow (the registry
 * isolates exceptions).
 *
 * <p>Registration is explicit (Vault-style): each channel is registered by the plugin
 * that provides it, avoiding cross-classloader class-scanning issues.
 */
public interface PunishmentChannel {

  /** Stable identifier for this channel (used in logs and to deduplicate registrations). */
  String id();

  /**
   * Called on the main thread after a report is closed and the built-in punishment
   * commands have been dispatched.
   *
   * @param report    the closed report
   * @param verdict   how the report was resolved
   * @param staffName the staff member who closed it (may be "system" for auto-close)
   */
  void onPunished(Report report, Verdict verdict, String staffName);
}
