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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.reportdesk.config.ConfigLoader;
import com.reportdesk.config.MapConfigSource;
import com.reportdesk.config.PluginConfig;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Verifies the roadmap P2 #16 detection-policy gates: every policy is an enhancement layer that
 * defaults to off and short-circuits on a single switch check. These tests exercise the pure
 * {@link DetectPolicy#enabled(PluginConfig)} decision for the two built-in strategies.
 */
class DetectPolicyTest {

  private static final BlockBreakDetectPolicy BLOCK_BREAK = new BlockBreakDetectPolicy();

  private static final ChatSpamDetectPolicy CHAT_SPAM = new ChatSpamDetectPolicy();

  @Test
  void bothPoliciesDefaultOff() {

    PluginConfig config = load(new LinkedHashMap<>());

    assertFalse(BLOCK_BREAK.enabled(config));

    assertFalse(CHAT_SPAM.enabled(config));
  }

  @Test
  void blockBreakPolicyNeedsEvidenceBlockBreakAndAutoDetect() {

    Map<String, Object> evidence = new LinkedHashMap<>();

    Map<String, Object> blockBreak = new LinkedHashMap<>();

    blockBreak.put("enabled", true);

    Map<String, Object> autoDetect = new LinkedHashMap<>();

    autoDetect.put("enabled", true);

    evidence.put("enabled", true);

    evidence.put("block-break", blockBreak);

    evidence.put("auto-detect", autoDetect);

    Map<String, Object> root = new LinkedHashMap<>();

    root.put("evidence", evidence);

    assertTrue(BLOCK_BREAK.enabled(load(root)));

    // Master auto-detect off -> policy off even with block-break on.
    autoDetect.put("enabled", false);

    assertFalse(BLOCK_BREAK.enabled(load(root)));

    // Evidence layer off -> policy off even with auto-detect on.
    autoDetect.put("enabled", true);

    evidence.put("enabled", false);

    assertFalse(BLOCK_BREAK.enabled(load(root)));
  }

  @Test
  void chatSpamPolicyNeedsEvidenceAutoDetectAndChatSpamSwitch() {

    Map<String, Object> autoDetect = new LinkedHashMap<>();

    autoDetect.put("enabled", true);

    autoDetect.put("chat-spam-enabled", true);

    Map<String, Object> evidence = new LinkedHashMap<>();

    evidence.put("enabled", true);

    evidence.put("auto-detect", autoDetect);

    Map<String, Object> root = new LinkedHashMap<>();

    root.put("evidence", evidence);

    assertTrue(CHAT_SPAM.enabled(load(root)));

    // Flipping the dedicated chat-spam switch off disables this policy only.
    autoDetect.put("chat-spam-enabled", false);

    assertFalse(CHAT_SPAM.enabled(load(root)));

    // The block-break policy is unaffected by the chat-spam switch.
    Map<String, Object> blockBreak = new LinkedHashMap<>();

    blockBreak.put("enabled", true);

    evidence.put("block-break", blockBreak);

    assertTrue(BLOCK_BREAK.enabled(load(root)));

    // chat-spam stays off regardless of block-break being on.
    assertFalse(CHAT_SPAM.enabled(load(root)));
  }

  @Test
  void policyIdentifiersAreStable() {

    assertEquals("block-break", BLOCK_BREAK.id());

    assertEquals("chat-spam", CHAT_SPAM.id());
  }

  private static PluginConfig load(Map<String, Object> root) {

    return new ConfigLoader(ignored -> {}).load(new MapConfigSource(root));
  }
}
