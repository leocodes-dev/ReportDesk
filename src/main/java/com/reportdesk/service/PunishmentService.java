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
import com.reportdesk.core.Report;
import com.reportdesk.core.Verdict;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;

/**
 * Optional linkage-punishment layer. Disabled by default; when disabled the only work done is a
 * single boolean check.
 */
public final class PunishmentService {

  private final ReportDeskPlugin plugin;

  public PunishmentService(ReportDeskPlugin plugin) {

    this.plugin = plugin;
  }

  public void apply(Report report, Verdict verdict, String staffName) {

    PluginConfig.Punishments settings = plugin.config().punishments();

    if (!settings.enabled()) {

      return;
    }

    List<String> commands =
        switch (verdict) {
          case RESOLVED -> settings.resolvedCommands();

          case REJECTED, INSUFFICIENT -> settings.rejectedCommands();

          default -> List.of();
        };

    if (commands.isEmpty()) {

      return;
    }

    Runnable dispatch =
        () -> {
          for (String raw : commands) {

            if (raw == null || raw.isBlank()) {

              continue;
            }

            String command = raw.trim();

            if (command.startsWith("/")) {

              command = command.substring(1);
            }

            String resolved =
                command
                    .replace("%player%", safe(report.targetName()))
                    .replace("%reason%", safe(report.detail()))
                    .replace("%id%", String.valueOf(report.id()))
                    .replace("%staff%", safe(staffName))
                    .replace("%verdict%", verdict.name().toLowerCase(Locale.ROOT))
                    .replace("%category%", safe(report.category()));

            try {

              Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved);

              plugin.audit("PUNISH", null, report.id(), resolved);

            } catch (RuntimeException exception) {

              plugin.getLogger().warning("Punishment command failed: " + exception.getMessage());
            }
          }
        };

    if (Bukkit.isPrimaryThread()) {

      dispatch.run();

    } else {

      Bukkit.getScheduler().runTask(plugin, dispatch);
    }
  }

  private String safe(String value) {

    if (value == null) {

      return "";
    }

    return value.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ');
  }
}
