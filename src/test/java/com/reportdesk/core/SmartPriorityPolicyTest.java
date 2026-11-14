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

package com.reportdesk.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.reportdesk.config.PluginConfig.SmartPriority;
import com.reportdesk.config.PluginConfig.SmartPriority.GarbageAction;
import com.reportdesk.test.TestFixtures;
import java.util.List;
import org.junit.jupiter.api.Test;

class SmartPriorityPolicyTest {

  private final SmartPriorityPolicy policy = new SmartPriorityPolicy();

  private static SmartPriority settings() {

    return new SmartPriority(
        true,
        30,
        20,
        25,
        10,
        15,
        90,
        70,
        30,
        GarbageAction.FLAG,
        2,
        200,
        5);
  }

  @Test
  void unknownReporterGetsNeutralCredibility() {

    Report report =
        TestFixtures.open(1L, TestFixtures.ALICE, "Anna", "Bob", "cheating", 100L)
            .withChatSelections(
                List.of(
                    new ReportChatSelection(91L, "Bob", "stop the duping"),
                    new ReportChatSelection(92L, "Bob", "he is using xray"),
                    new ReportChatSelection(93L, "Bob", "lag machine confirmed"),
                    new ReportChatSelection(94L, "Bob", "bypassing walls here"),
                    new ReportChatSelection(95L, "Bob", "watch him clip through")),
                100L);

    SmartPriorityPolicy.Evaluation evaluation =
        policy.evaluate(report, new SmartPriorityPolicy.Factors(0, 0, 0, 0), settings(), ReportPriority.NORMAL);

    // Unknown reporter gets the neutral 0.5 credibility, the target is clean,
    // chat evidence is full, detail "detail 1" scores 0.424 and NORMAL is 1/3:
    // (30*0.5 + 0 + 25*0.424 + 10*1.0 + 15*(1/3)) = 40.6 -> 41, mid band.
    assertEquals(41, evaluation.score());

    assertFalse(evaluation.garbage());
  }

  @Test
  void trustedReporterAndDirtyTargetScoreHigh() {

    Report report =
        TestFixtures.open(2L, TestFixtures.ALICE, "Anna", "Bob", "cheating", 100L)
            .withChatSelections(
                List.of(new ReportChatSelection(90L, "Bob", "hack him with netherite")),
                100L);

    SmartPriorityPolicy.Factors factors = new SmartPriorityPolicy.Factors(10, 10, 4, 6);

    SmartPriorityPolicy.Evaluation evaluation = policy.evaluate(report, factors, settings(), ReportPriority.URGENT);

    // A fully trusted reporter against a repeatedly reported target must at
    // least reach the configured HIGH band, even with a short detail.
    assertTrue(evaluation.score() >= settings().highScore(), "score was " + evaluation.score());

    assertFalse(evaluation.garbage());
  }

  @Test
  void spamDetailWithCleanHistoryIsGarbage() {

    Report report = TestFixtures.open(3L, TestFixtures.ALICE, "Anna", "Bob", "other", 100L);

    SmartPriorityPolicy.Factors factors = new SmartPriorityPolicy.Factors(5, 0, 0, 0);

    SmartPriority settings =
        new SmartPriority(
            true,
            30,
            20,
            25,
            10,
            15,
            90,
            70,
            30,
            GarbageAction.DEMOTE,
            8,
            200,
            5);

    SmartPriorityPolicy.Evaluation evaluation = policy.evaluate(report, factors, settings, ReportPriority.LOW);

    assertTrue(evaluation.garbage(), "score was " + evaluation.score());

    assertTrue(evaluation.score() <= settings.garbageScore());
  }

  @Test
  void zeroWeightsFallBackToUnweightedAverage() {

    Report report =
        TestFixtures.open(4L, TestFixtures.ALICE, "Anna", "Bob", "other", 100L)
            .withChatSelections(
                List.of(
                    new ReportChatSelection(91L, "Bob", "stop the duping"),
                    new ReportChatSelection(92L, "Bob", "he is using xray"),
                    new ReportChatSelection(93L, "Bob", "lag machine confirmed"),
                    new ReportChatSelection(94L, "Bob", "bypassing walls here"),
                    new ReportChatSelection(95L, "Bob", "watch him clip through")),
                100L);

    SmartPriority settings =
        new SmartPriority(
            true, 0, 0, 0, 0, 0, 90, 70, 30, GarbageAction.FLAG, 2, 200, 5);

    SmartPriorityPolicy.Evaluation evaluation =
        policy.evaluate(
            report, new SmartPriorityPolicy.Factors(10, 10, 0, 4), settings, ReportPriority.URGENT);

    // With a zero total weight the policy must fall back to the plain
    // unweighted average of the five factors instead of dividing by zero:
    // reporter 10/10 = 1.0, target min(1, 0.25*4 + 0.1*0) = 1.0, full chat
    // evidence = 1.0, URGENT severity = 1.0, plus the fixture detail.
    double detail = SmartPriorityPolicy.detailQuality(report.detail(), 2, 200);

    int expected = (int) Math.round(100.0 * (4.0 + detail) / 5.0);

    assertEquals(expected, evaluation.score());

    assertFalse(evaluation.garbage());
  }

  @Test
  void detailQualityRewardsDiversityAndLength() {

    double empty = SmartPriorityPolicy.detailQuality("", 2, 200);

    double repeated = SmartPriorityPolicy.detailQuality("aaaaaa", 2, 200);

    double rich = SmartPriorityPolicy.detailQuality("v01f1y with numbers 1234 and words", 2, 200);

    double underMinimum = SmartPriorityPolicy.detailQuality("ok", 8, 200);

    assertEquals(0.0, empty, 1e-9);

    assertTrue(repeated < 0.3, "repeated text scored " + repeated);

    assertTrue(rich > repeated, "rich text must beat repeated text");

    assertTrue(underMinimum <= 0.2, "below minimum length must stay capped");
  }

  @Test
  void garbageActionDefaultsToFlagWhenActionIsFlag() {

    Report report = TestFixtures.open(5L, TestFixtures.ALICE, "Anna", "Bob", "other", 100L);

    SmartPriorityPolicy.Evaluation evaluation =
        policy.evaluate(
            report, new SmartPriorityPolicy.Factors(5, 0, 0, 0), settings(), ReportPriority.LOW);

    // The action choice is consumed by the service, not the policy: with the
    // default threshold (30) a weak submission lands in the garbage band.
    assertEquals(settings().garbageAction(), GarbageAction.FLAG);

    assertTrue(evaluation.garbage());
  }
}
