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

import com.reportdesk.config.PluginConfig;
import com.reportdesk.i18n.MessageKeys;
import java.util.List;

/**
 * Block-break detection policy. Maps the existing
 * {@code evidence.auto-detect.block-break-threshold} config onto the
 * {@link DetectPolicy} interface so it can be discovered alongside other
 * strategies without changing its behaviour.
 *
 * <p>When the evidence layer or the block-break switch is off, {@link
 * #enabled(PluginConfig)} returns false and the check method is not called.
 */
public final class BlockBreakDetectPolicy implements DetectPolicy {

  @Override
  public String id() {

    return "block-break";
  }

  @Override
  public boolean enabled(PluginConfig config) {

    return config.evidence().enabled()
        && config.evidence().autoDetect().enabled()
        && config.evidence().blockBreak().enabled();
  }

  @Override
  public List<Detection> check(EvidenceService evidence, PluginConfig config, int windowSeconds) {

    if (!enabled(config)) {

      return List.of();
    }

    var autoDetect = config.evidence().autoDetect();

    return evidence
        .recentBreakStats(windowSeconds, autoDetect.blockBreakThreshold())
        .stream()
        .map(
            stats ->
                new Detection(
                    stats.playerId(),
                    stats.playerName(),
                    stats.count(),
                    windowSeconds))
        .toList();
  }

  @Override
  public String detailKey() {

    return MessageKeys.EVIDENCE_AUTO_REPORT_DETAIL;
  }

  @Override
  public String staffKey() {

    return MessageKeys.EVIDENCE_AUTO_REPORT_STAFF;
  }

  @Override
  public String targetCategory() {

    return "cheating";
  }
}
