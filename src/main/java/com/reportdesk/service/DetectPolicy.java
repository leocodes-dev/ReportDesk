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
import java.util.List;
import java.util.UUID;

/**
 * Strategy interface for auto-detection policies (roadmap P2 #16).
 *
 * <p>Each policy is independently enabled/disabled via config and must
 * short-circuit to an empty list when its switch is off — no allocation,
 * no IO, no further config reads.
 *
 * <p>Built-in strategies: {@link BlockBreakDetectPolicy} and
 * {@link ChatSpamDetectPolicy}. Custom strategies can be registered at
 * plugin startup without modifying the detection task.
 */
public interface DetectPolicy {

  /** Stable identifier for log messages and audit entries. */
  String id();

  /**
   * True only when this policy's specific switch is on in the live config.
   *
   * <p>Called once at the top of each detection tick; when false the
   * {@link #check} method is not called and no resources are allocated.
   */
  boolean enabled(PluginConfig config);

  /**
   * Returns players whose metric exceeded the configured threshold within
   * the given window.
   *
   * <p>Must short-circuit to {@code List.of()} when the policy is disabled;
   * no allocation in the disabled path.
   *
   * @param evidence the in-memory evidence layer (bounded deques, no IO)
   * @param config live plugin configuration
   * @param windowSeconds the detection window
   * @return players exceeding the threshold; empty when disabled or no matches
   */
  List<Detection> check(EvidenceService evidence, PluginConfig config, int windowSeconds);

  /** The i18n message key for the report detail line. */
  String detailKey();

  /** The i18n message key for the staff alert line. */
  String staffKey();

  /** The report category id to use when this policy creates a system report. */
  String targetCategory();

  /** One detection: a player whose metric exceeded the threshold within the window. */
  record Detection(UUID playerId, String playerName, int count, int windowSeconds) {}
}
