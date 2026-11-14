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
 * Chat-spam detection policy (roadmap P2 #16). Counts chat messages captured by the evidence
 * layer within the detection window and reports players who exceed the configured threshold.
 *
 * <p>Disabled by default via {@code evidence.auto-detect.chat-spam-enabled}; when the switch is
 * off, {@link #enabled(PluginConfig)} returns false and {@link #check} is never called, so no
 * allocation or IO occurs in the disabled path.
 */
public final class ChatSpamDetectPolicy implements DetectPolicy {

  @Override
  public String id() {

    return "chat-spam";
  }

  @Override
  public boolean enabled(PluginConfig config) {

    return config.evidence().enabled()
        && config.evidence().autoDetect().enabled()
        && config.evidence().autoDetect().chatSpamEnabled();
  }

  @Override
  public List<Detection> check(EvidenceService evidence, PluginConfig config, int windowSeconds) {

    if (!enabled(config)) {

      return List.of();
    }

    int threshold = config.evidence().autoDetect().chatSpamThreshold();

    return evidence
        .recentChatStats(windowSeconds, threshold)
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

    return MessageKeys.EVIDENCE_CHAT_SPAM_DETAIL;
  }

  @Override
  public String staffKey() {

    return MessageKeys.EVIDENCE_CHAT_SPAM_STAFF;
  }

  @Override
  public String targetCategory() {

    return "spam";
  }
}
