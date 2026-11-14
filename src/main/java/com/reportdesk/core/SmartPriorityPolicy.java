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

import com.reportdesk.config.PluginConfig.SmartPriority;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Heuristic smart-priority scoring (optional, disabled by default).
 *
 * <p>Each submission is scored 0-100 from five weighted factors:
 *
 * <ul>
 *   <li>reporter credibility - share of the reporter's past reports that were confirmed;
 *       unknown reporters get a neutral 0.5 so new players are not penalised;
 *   <li>target history - confirmed reports against the target plus its current open count;
 *   <li>detail quality - length and character diversity of the description, which keeps
 *       one-word spam ("hack", "aaaa") at the bottom;
 *   <li>chat evidence - selected chat messages relative to the configured maximum;
 *   <li>category severity - the configured severity of the chosen category.
 * </ul>
 *
 * <p>All thresholds and weights come from the {@code smart-priority} config section; this class
 * holds no business constants of its own.
 */
public final class SmartPriorityPolicy {

  /** History counts the caller gathers from the repository in a single pass. */
  public record Factors(int reporterTotal, int reporterResolved, int targetOpen, int targetResolved) {}

  /** The 0-100 score and whether it falls into the configured garbage band. */
  public record Evaluation(int score, boolean garbage) {}

  public Evaluation evaluate(
      Report report, Factors factors, SmartPriority settings, ReportPriority categorySeverity) {

    double reporter =
        factors.reporterTotal() == 0
            ? 0.5
            : factors.reporterResolved() / (double) factors.reporterTotal();

    // Each confirmed report against the target adds a quarter point, each still-open
    // report a tenth; capped at 1.0 so a single hostile target cannot saturate the factor.
    double target = Math.min(1.0, 0.25 * factors.targetResolved() + 0.1 * factors.targetOpen());

    double detail = detailQuality(report.detail(), settings.detailMinLength(), settings.detailMaxLength());

    int maxSelected = Math.max(1, settings.chatMaxSelected());

    double chat = Math.min(1.0, report.chatSelections().size() / (double) maxSelected);

    double category =
        (categorySeverity == null ? 1 : categorySeverity.ordinal())
            / (double) (ReportPriority.values().length - 1);

    int totalWeight =
        settings.reporterCredibilityWeight()
            + settings.targetHistoryWeight()
            + settings.detailQualityWeight()
            + settings.chatEvidenceWeight()
            + settings.categorySeverityWeight();

    double weighted;

    if (totalWeight <= 0) {

      // No factor is weighted: fall back to the unweighted average.
      weighted =
          (reporter + target + detail + chat + category) / 5.0 * 100.0;

    } else {

      weighted =
          100.0
              * (settings.reporterCredibilityWeight() * reporter
                  + settings.targetHistoryWeight() * target
                  + settings.detailQualityWeight() * detail
                  + settings.chatEvidenceWeight() * chat
                  + settings.categorySeverityWeight() * category)
              / totalWeight;
    }

    int score = (int) Math.round(Math.max(0.0, Math.min(100.0, weighted)));

    return new Evaluation(score, score <= settings.garbageScore());
  }

  /**
   * 0..1 quality of the free-text detail: length up to the configured maximum (60% of the
   * factor) plus character diversity (40%); descriptions shorter than the configured minimum
   * are capped low regardless.
   */
  static double detailQuality(String detail, int minLength, int maxLength) {

    String text = detail == null ? "" : detail.trim();

    int length = text.length();

    if (length == 0) {

      return 0.0;
    }

    double lengthPart = Math.min(1.0, length / (double) Math.max(1, maxLength));

    Set<Character> unique = new HashSet<>();

    for (char character : text.toLowerCase(Locale.ROOT).toCharArray()) {

      unique.add(character);
    }

    double diversity = unique.size() / (double) length;

    double quality = 0.6 * lengthPart + 0.4 * diversity;

    if (length < Math.max(0, minLength)) {

      quality = Math.min(quality, 0.2);
    }

    return quality;
  }
}
