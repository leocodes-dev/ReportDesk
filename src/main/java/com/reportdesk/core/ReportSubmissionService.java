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

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Pure report creation pipeline. The Bukkit layer collects player state and then calls this class;
 * all anti-abuse checks and model creation live here so they can be unit tested without a server.
 */
public final class ReportSubmissionService {

  public enum SubmitReason {
    SUCCESS,

    NOT_READY,

    INVALID_TARGET,

    CATEGORY_UNKNOWN,

    CATEGORY_DISABLED,

    SELF_REPORT,

    REQUIRES_ONLINE,

    OFFLINE_DISABLED,

    ACCOUNT_TOO_YOUNG,

    DETAIL_EMPTY,

    DETAIL_TOO_SHORT,

    DETAIL_TOO_LONG,

    DETAIL_ILLEGAL,

    DETAIL_BLOCKED,

    COOLDOWN,

    DAILY_LIMIT,

    SAME_TARGET,

    DUPLICATE,

    MAX_OPEN
  }

  public record SubmitRequest(
      UUID reporterId,
      String reporterName,
      UUID targetId,
      String targetName,
      boolean targetOnline,
      long reporterFirstPlayed,
      String category,
      String detail,
      boolean bypassLimits,
      boolean systemGenerated,
      List<ReportChatSelection> chatSelections) {

    public SubmitRequest {

      chatSelections = chatSelections == null ? List.of() : List.copyOf(chatSelections);
    }

    /**
     * Backwards-compatible constructor for callers that do not attach chat
     *
     * <p>selections (system reports, commands and older integrations).
     */
    public SubmitRequest(
        UUID reporterId,
        String reporterName,
        UUID targetId,
        String targetName,
        boolean targetOnline,
        long reporterFirstPlayed,
        String category,
        String detail,
        boolean bypassLimits,
        boolean systemGenerated) {

      this(
          reporterId,
          reporterName,
          targetId,
          targetName,
          targetOnline,
          reporterFirstPlayed,
          category,
          detail,
          bypassLimits,
          systemGenerated,
          List.of());
    }
  }

  public record SubmitResult(SubmitReason reason, Report report, Map<String, String> placeholders) {

    public SubmitResult {

      placeholders = placeholders == null ? Map.of() : Map.copyOf(placeholders);
    }

    public boolean success() {

      return reason == SubmitReason.SUCCESS && report != null;
    }

    public static SubmitResult error(SubmitReason reason, Map<String, String> placeholders) {

      return new SubmitResult(reason, null, placeholders);
    }
  }

  public record Settings(
      String serverName,
      boolean blockSelfReport,
      boolean allowOfflineTarget,
      boolean requireTargetOnline,
      int minAccountAgeMinutes) {}

  private final ReportRepository repository;

  private final RateLimiter rateLimiter;

  private final InputValidator validator;

  private final CategoryRegistry categories;

  private final Settings settings;

  private final Clock clock;

  public ReportSubmissionService(
      ReportRepository repository,
      RateLimiter rateLimiter,
      InputValidator validator,
      CategoryRegistry categories,
      Settings settings,
      Clock clock) {

    this.repository = repository;

    this.rateLimiter = rateLimiter;

    this.validator = validator;

    this.categories = categories;

    this.settings = settings == null ? new Settings("", true, true, false, 0) : settings;

    this.clock = clock == null ? Clock.systemDefaultZone() : clock;
  }

  public synchronized SubmitResult submit(SubmitRequest request) {

    if (request == null) {

      return SubmitResult.error(SubmitReason.INVALID_TARGET, Map.of());
    }

    if (!repository.isReady() && !request.systemGenerated()) {

      return SubmitResult.error(SubmitReason.NOT_READY, Map.of());
    }

    String targetName = validator.normalizeName(request.targetName());

    InputValidator.Result nameResult = validator.validatePlayerName(targetName);

    if (!nameResult.valid()) {

      return SubmitResult.error(SubmitReason.INVALID_TARGET, Map.of("player", targetName));
    }

    String categoryId = request.category() == null ? "" : request.category().trim();

    var category = categories.find(categoryId);

    if (category.isEmpty()) {

      return SubmitResult.error(SubmitReason.CATEGORY_UNKNOWN, Map.of("category", categoryId));
    }

    if (!category.get().enabled()) {

      return SubmitResult.error(
          SubmitReason.CATEGORY_DISABLED, Map.of("category", category.get().display()));
    }

    boolean samePlayer =
        request.targetId() != null && request.targetId().equals(request.reporterId());

    boolean sameName =
        request.reporterName() != null && request.reporterName().equalsIgnoreCase(targetName);

    if (!request.systemGenerated() && settings.blockSelfReport() && (samePlayer || sameName)) {

      return SubmitResult.error(SubmitReason.SELF_REPORT, Map.of());
    }

    if (!request.systemGenerated() && !request.targetOnline()) {

      if (settings.requireTargetOnline()) {

        return SubmitResult.error(SubmitReason.REQUIRES_ONLINE, Map.of("player", targetName));
      }

      if (!settings.allowOfflineTarget()) {

        return SubmitResult.error(SubmitReason.OFFLINE_DISABLED, Map.of("player", targetName));
      }
    }

    long now = clock.millis();

    if (!request.systemGenerated()
        && !request.bypassLimits()
        && settings.minAccountAgeMinutes() > 0
        && request.reporterFirstPlayed() > 0) {

      long required = settings.minAccountAgeMinutes() * 60_000L;

      long age = now - request.reporterFirstPlayed();

      if (age >= 0 && age < required) {

        return SubmitResult.error(
            SubmitReason.ACCOUNT_TOO_YOUNG,
            Map.of("minutes", String.valueOf(settings.minAccountAgeMinutes())));
      }
    }

    InputValidator.Result detailResult = validator.validateDetail(request.detail());

    if (!detailResult.valid()) {

      boolean emptyButHasSelections =
          detailResult.reason() == InputValidator.Reason.EMPTY
              && !request.chatSelections().isEmpty();

      if (!emptyButHasSelections) {

        SubmitReason reason =
            switch (detailResult.reason()) {
              case EMPTY -> SubmitReason.DETAIL_EMPTY;

              case TOO_SHORT -> SubmitReason.DETAIL_TOO_SHORT;

              case TOO_LONG -> SubmitReason.DETAIL_TOO_LONG;

              case BLOCKED_WORD -> SubmitReason.DETAIL_BLOCKED;

              default -> SubmitReason.DETAIL_ILLEGAL;
            };

        Map<String, String> placeholders = new LinkedHashMap<>();

        placeholders.put("min", String.valueOf(validator.settings().detailMinLength()));

        placeholders.put("max", String.valueOf(validator.settings().detailMaxLength()));

        placeholders.put("word", detailResult.word());

        return SubmitResult.error(reason, placeholders);
      }
    }

    if (!request.systemGenerated() && !request.bypassLimits()) {

      RateLimiter.Decision decision =
          rateLimiter.check(repository, request.reporterId(), targetName);

      if (!decision.isAllowed()) {

        SubmitReason reason =
            switch (decision.reason()) {
              case COOLDOWN -> SubmitReason.COOLDOWN;

              case DAILY_LIMIT -> SubmitReason.DAILY_LIMIT;

              case SAME_TARGET -> SubmitReason.SAME_TARGET;

              case DUPLICATE -> SubmitReason.DUPLICATE;

              case MAX_OPEN -> SubmitReason.MAX_OPEN;

              case ALLOWED -> SubmitReason.SUCCESS;
            };

        Map<String, String> placeholders = new LinkedHashMap<>();

        placeholders.put("seconds", String.valueOf(decision.seconds()));

        placeholders.put("limit", String.valueOf(decision.limit()));

        placeholders.put("id", String.valueOf(decision.duplicateId()));

        return SubmitResult.error(reason, placeholders);
      }
    }

    long id = repository.nextId();

    Report report =
        new Report(
            id,
            request.reporterId() == null ? new UUID(0L, 0L) : request.reporterId(),
            request.reporterName() == null ? "" : request.reporterName(),
            request.targetId(),
            targetName,
            category.get().id(),
            request.detail() == null ? "" : request.detail().trim(),
            ReportStatus.OPEN,
            now,
            now,
            null,
            "",
            0L,
            null,
            "",
            0L,
            "",
            "",
            java.util.List.of(),
            EvidenceMeta.none(),
            request.systemGenerated(),
            settings.serverName(),
            ReportPriority.NORMAL,
            0L,
            0L,
            List.of(),
            0L,
            request.chatSelections());

    repository.add(report);

    if (!request.systemGenerated()) {

      rateLimiter.record(request.reporterId(), targetName);
    }

    return new SubmitResult(SubmitReason.SUCCESS, report, Map.of("id", String.valueOf(id)));
  }
}
