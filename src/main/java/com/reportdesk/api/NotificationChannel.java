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
import java.util.Map;

/**
 * SPI for third-party notification channels (Discord, QQ, email, etc.).
 *
 * <p>Implement this interface in your plugin and register it with
 * {@link com.reportdesk.ReportDeskPlugin#notificationChannelRegistry()}:
 *
 * <pre>{@code
 * public class DiscordChannel implements NotificationChannel {
 *
 *   public String id() { return "discord"; }
 *
 *   public void onCreated(Report report, Map<String, String> placeholders) {
 *     // send to Discord webhook
 *   }
 *
 *   public void onClosed(Report report, String staffName, Map<String, String> placeholders) {
 *     // update Discord message
 *   }
 *
 *   public void onClaimed(Report report, String staffName, Map<String, String> placeholders) {}
 *   public void onReopened(Report report, String staffName, Map<String, String> placeholders) {}
 * }
 *
 * // In your plugin's onEnable:
 * ReportDeskPlugin.reportDesk().notificationChannelRegistry()
 *     .register(new DiscordChannel());
 * }</pre>
 *
 * <p>Registration is explicit (Vault-style): each channel is registered by the plugin that
 * provides it, avoiding cross-classloader class-scanning issues. A single failing channel
 * never breaks other channels or the core plugin (the registry isolates exceptions).
 *
 * <p>All methods have a default no-op implementation so channels only implement the events
 * they care about.
 */
public interface NotificationChannel {

  /** Stable identifier for this channel (used in logs and to deduplicate registrations). */
  String id();

  /** Called after a new report is created and persisted. */
  default void onCreated(Report report, Map<String, String> placeholders) {}

  /** Called after a report is claimed by a staff member. */
  default void onClaimed(Report report, String staffName, Map<String, String> placeholders) {}

  /** Called after a report is closed (resolved / rejected / duplicate / auto-closed). */
  default void onClosed(Report report, String staffName, Map<String, String> placeholders) {}

  /** Called after a report is reopened. */
  default void onReopened(Report report, String staffName, Map<String, String> placeholders) {}
}
