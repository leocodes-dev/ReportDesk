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
import com.reportdesk.core.Report;
import com.reportdesk.core.ReportCategory;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Optional auto-detection. Disabled by default and never punishes: it either creates a system
 * report or alerts staff.
 *
 * <p>Detection is driven by a fixed list of {@link DetectPolicy} strategies (block-break and
 * chat-spam). Each policy carries its own switch; when a policy is off its check is skipped and no
 * resources are allocated. Adding a new policy does not change the block-break default behaviour.
 */
public final class AutoDetectService {

  private final ReportDeskPlugin plugin;

  private final EvidenceService evidence;

  private final Map<UUID, Long> cooldownUntil = new ConcurrentHashMap<>();

  private final List<DetectPolicy> policies;

  public AutoDetectService(ReportDeskPlugin plugin, EvidenceService evidence) {

    this.plugin = plugin;

    this.evidence = evidence;

    this.policies = List.of(new BlockBreakDetectPolicy(), new ChatSpamDetectPolicy());
  }

  /**
   * One detection tick. Short-circuits when the evidence layer or the auto-detect master switch
   * is off. Otherwise walks the registered policies; each policy short-circuits on its own switch.
   */
  public void check() {

    var auto = plugin.config().evidence().autoDetect();

    if (!auto.enabled()) {

      return;
    }

    if (!plugin.config().evidence().enabled()) {

      return;
    }

    long now = System.currentTimeMillis();

    int windowSeconds = auto.windowSeconds();

    long cooldownMillis = auto.cooldownSeconds() * 1000L;

    for (DetectPolicy policy : policies) {

      if (!policy.enabled(plugin.config())) {

        continue;
      }

      for (DetectPolicy.Detection detection :
          policy.check(evidence, plugin.config(), windowSeconds)) {

        Long until = cooldownUntil.get(detection.playerId());

        if (until != null && until > now) {

          continue;
        }

        cooldownUntil.put(detection.playerId(), now + cooldownMillis);

        DetectPolicy capturedPolicy = policy;

        Bukkit.getScheduler().runTask(plugin, () -> handle(capturedPolicy, detection));
      }
    }
  }

  private void handle(DetectPolicy policy, DetectPolicy.Detection detection) {

    Player target = Bukkit.getPlayer(detection.playerId());

    String targetName =
        target != null ? target.getName() : detection.playerName();

    if (targetName == null || targetName.isBlank()) {

      return;
    }

    var auto = plugin.config().evidence().autoDetect();

    var registry = plugin.config().categoryRegistry();

    ReportCategory category =
        registry
            .find(policy.targetCategory())
            .orElseGet(
                () ->
                    registry
                        .find(plugin.config().defaultCategory())
                        .orElseThrow());

    String detail =
        plugin
            .messages()
            .plain(
                policy.detailKey(),
                Map.of(
                    "count", String.valueOf(detection.count()),
                    "seconds", String.valueOf(detection.windowSeconds())));

    if (auto.createReport()) {

      Optional<Report> created =
          plugin.reportService().submitSystem(targetName, category.id(), detail);

      created.ifPresent(
          report ->
              plugin
                  .notification()
                  .onAutoDetected(
                      report, detection.count(), detection.windowSeconds()));

    } else if (auto.notifyStaff()) {

      Map<String, String> placeholders = new LinkedHashMap<>();

      placeholders.put("target", targetName);

      placeholders.put("count", String.valueOf(detection.count()));

      placeholders.put("seconds", String.valueOf(detection.windowSeconds()));

      placeholders.put("id", "-");

      plugin
          .notification()
          .staffAlert(plugin.messages().prefixed(policy.staffKey(), placeholders));
    }
  }

  public void clearCooldowns() {

    cooldownUntil.clear();
  }
}
