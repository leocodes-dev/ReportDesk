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

package com.reportdesk.config;

import com.reportdesk.core.CategoryRegistry;
import com.reportdesk.core.InputValidator;
import com.reportdesk.core.RateLimitPolicy;
import com.reportdesk.core.ReportCategory;
import com.reportdesk.core.ReportSubmissionService;
import java.util.List;
import java.util.Optional;

/**
 * Immutable, fully validated plugin configuration. All thresholds come from config.yml (or the safe
 * defaults in {@link ConfigLoader}).
 */
public record PluginConfig(
    String serverName,
    String language,
    Storage storage,
    Limits limits,
    Reports reports,
    Gui gui,
    Notifications notifications,
    Evidence evidence,
    Punishments punishments,
    Audit audit,
    ConfigEditor configEditor,
    Security security,
    List<ReportCategory> categories,
    String defaultCategory,
    SmartPriority smartPriority,
    BedrockForms bedrockForms) {

  public PluginConfig {

    serverName = blankTo(serverName, "ReportDesk Server");

    language = blankTo(language, "zh_CN");

    categories = categories == null ? List.of() : List.copyOf(categories);

    defaultCategory = blankTo(defaultCategory, "other");

    smartPriority = smartPriority == null ? defaultSmartPriority() : smartPriority;

    bedrockForms = bedrockForms == null ? defaultBedrockForms() : bedrockForms;
  }

  private static BedrockForms defaultBedrockForms() {

    return new BedrockForms(false);
  }

  private static SmartPriority defaultSmartPriority() {

    return new SmartPriority(
        false,
        30,
        20,
        25,
        10,
        15,
        90,
        70,
        30,
        SmartPriority.GarbageAction.FLAG,
        2,
        200,
        5);
  }

  public CategoryRegistry categoryRegistry() {

    return CategoryRegistry.of(categories);
  }

  public Optional<ReportCategory> category(String idOrAlias) {

    return categoryRegistry().find(idOrAlias);
  }

  public RateLimitPolicy rateLimitPolicy() {

    return new RateLimitPolicy(
        limits.cooldownSeconds(),
        limits.dailyLimit(),
        limits.sameTargetIntervalSeconds(),
        limits.duplicateWindowSeconds(),
        limits.maxOpenPerTarget());
  }

  public InputValidator.Settings validatorSettings() {

    return new InputValidator.Settings(
        security.detailMinLength(),
        security.detailMaxLength(),
        security.noteMaxLength(),
        security.detailRequired(),
        security.allowColorCodes(),
        security.blockedWords(),
        security.playerNamePattern());
  }

  public com.reportdesk.core.PriorityPolicy priorityPolicy() {

    com.reportdesk.core.ReportPriority[] levels = com.reportdesk.core.ReportPriority.values();

    java.util.Map<com.reportdesk.core.ReportPriority, Integer> slaHours =
        new java.util.EnumMap<>(com.reportdesk.core.ReportPriority.class);

    for (com.reportdesk.core.ReportPriority level : levels) {

      slaHours.put(level, reports.priority().slaHours(level));
    }

    return new com.reportdesk.core.PriorityPolicy(
        reports.priority().enabled(),
        reports.priority().repeatOffenderThreshold(),
        reports.priority().repeatOffenderLevel(),
        slaHours);
  }

  /** Convenience accessor for the report conversation settings. */
  public Replies replies() {

    return reports.replies();
  }

  /**
   * True only when the chat picker is switched on, the evidence layer can
   *
   * <p>capture chat, and the configured category exists and is enabled.
   */
  public boolean chatReportAvailable() {

    ChatReport settings = reports.chatReport();

    if (!settings.enabled()) {

      return false;
    }

    if (!evidence.enabled() || !evidence.captureChatLog() || !evidence.captureOnReport()) {

      return false;
    }

    return category(settings.category()).filter(ReportCategory::enabled).isPresent();
  }

  public ReportSubmissionService.Settings submissionSettings() {

    return new ReportSubmissionService.Settings(
        serverName,
        limits.blockSelfReport(),
        limits.allowOfflineTarget(),
        limits.requireTargetOnline(),
        limits.minAccountAgeMinutes());
  }

  public record Storage(
      String directory,
      boolean backupOnSave,
      int maxBackups,
      long backupIntervalMinutes,
      int shutdownTimeoutSeconds,
      int compactAfterEntries,
      long compactIntervalMinutes) {}

  public record Limits(
      long commandCooldownMillis,
      long cooldownSeconds,
      int dailyLimit,
      long sameTargetIntervalSeconds,
      long duplicateWindowSeconds,
      int maxOpenPerTarget,
      int minAccountAgeMinutes,
      boolean blockSelfReport,
      boolean allowOfflineTarget,
      boolean requireTargetOnline,
      String bypassPermission) {}

  public record Reports(
      int textPageSize,
      int guiPageSize,
      boolean autoCloseEnabled,
      int autoCloseDays,
      int retentionDays,
      int maintenanceIntervalMinutes,
      int statsTopTargets,
      int staffStatsTop,
      String defaultCategory,
      Priority priority,
      Replies replies,
      ChatReport chatReport) {}

  public record Priority(
      boolean enabled,
      int repeatOffenderThreshold,
      com.reportdesk.core.ReportPriority repeatOffenderLevel,
      int overdueCheckMinutes,
      boolean overdueAlertEnabled,
      int slaUrgentHours,
      int slaHighHours,
      int slaNormalHours,
      int slaLowHours) {

    public int slaHours(com.reportdesk.core.ReportPriority priority) {

      if (priority == null) {

        return slaNormalHours;
      }

      return switch (priority) {
        case URGENT -> slaUrgentHours;

        case HIGH -> slaHighHours;

        case LOW -> slaLowHours;

        default -> slaNormalHours;
      };
    }
  }

  public record Replies(
      boolean enabled,
      boolean allowPlayerReplies,
      int maxLength,
      int maxPerReport,
      boolean notifyOnReply) {}

  /**
   * Reporter-side chat violation picker. Disabled by default; it also needs
   *
   * <p>the evidence layer with chat capture and capture-on-report enabled,
   *
   * <p>because the picker only lists lines that were actually captured and
   *
   * <p>persisted with the report.
   */
  public record ChatReport(boolean enabled, String category, int maxSelected) {

    public ChatReport {

      category = category == null || category.isBlank() ? "chat" : category.trim();

      maxSelected = Math.max(1, maxSelected);
    }
  }

  public record Gui(
      boolean enabled,
      boolean fillEmptySlots,
      boolean closeOnAction,
      boolean soundsEnabled,
      String soundClick,
      String soundSuccess,
      String soundError,
      Admin admin) {}

  /**
   * Admin management menu settings. The menu itself reuses the regular GUI
   *
   * <p>page size and audit page size, so only its own switches live here.
   */
  public record Admin(boolean enabled, boolean confirmDestructive) {}

  public record Notifications(
      boolean staffEnabled,
      String staffPermission,
      boolean staffSoundEnabled,
      String staffSound,
      boolean reporterEnabled,
      boolean reporterQueueOffline,
      int reporterMaxQueued,
      int reporterQueueRetentionDays,
      boolean watchlistEnabled,
      int watchlistMaxTargets,
      boolean watchlistNotifyOnReport,
      boolean broadcastEnabled,
      Webhook webhook,
      Sla sla) {}

  public record Webhook(
      boolean enabled,
      String url,
      int timeoutSeconds,
      String username,
      String avatarUrl,
      boolean eventCreated,
      boolean eventClaimed,
      boolean eventClosed,
      int retryCount,
      int retryBaseSeconds,
      boolean failureAlertEnabled) {}

  public record Evidence(
      boolean enabled,
      boolean captureOnReport,
      int retentionDays,
      int chatLogSize,
      int chatMaxLength,
      boolean captureLocation,
      boolean captureGameMode,
      boolean captureHealth,
      boolean captureInventory,
      int inventoryMaxItems,
      int displayNameMaxLength,
      boolean captureChatLog,
      boolean capturePing,
      BlockBreak blockBreak,
      Movement movement,
      AutoDetect autoDetect) {}

  public record BlockBreak(boolean enabled, int maxPerPlayer) {}

  public record Movement(boolean enabled, int minIntervalTicks, int maxSamples) {}

  public record AutoDetect(
      boolean enabled,
      int checkIntervalSeconds,
      int windowSeconds,
      int blockBreakThreshold,
      int cooldownSeconds,
      boolean createReport,
      boolean notifyStaff) {}

  public record Punishments(
      boolean enabled, List<String> resolvedCommands, List<String> rejectedCommands) {}

  public record Audit(boolean enabled, int retentionDays, int pageSize, int maxFileSizeMb) {}

  /** Optional weekly SLA summary pushed to the webhook (enhanced layer, off by default). */
  public record Sla(boolean weeklyEnabled, int weeklyIntervalHours) {}

  /**
   * Settings for the in-game config editor itself. These are read from the
   *
   * <p>live config on every edit, so changing them takes effect immediately.
   */
  public record ConfigEditor(
      boolean enabled, boolean backupOnSave, int maxBackups, int maxValueLength) {}

  public record Security(
      long adminCooldownMillis,
      int pendingTimeoutSeconds,
      int detailMinLength,
      int detailMaxLength,
      int noteMaxLength,
      int maxNotesPerReport,
      boolean detailRequired,
      boolean allowColorCodes,
      String playerNamePattern,
      List<String> blockedWords,
      boolean ipHashEnabled,
      String ipHashSalt) {}

  /**
   * Optional smart-priority scoring (enhanced layer, disabled by default).
   *
   * <p>When enabled, every submission is scored 0-100 from five weighted
   *
   * <p>factors (reporter credibility, target history, detail quality, chat
   *
   * <p>evidence and category severity). The score can upgrade the effective
   *
   * <p>priority and, below {@code garbageScore}, trigger the configured
   *
   * <p>garbage action.
   */
  public record SmartPriority(
      boolean enabled,
      int reporterCredibilityWeight,
      int targetHistoryWeight,
      int detailQualityWeight,
      int chatEvidenceWeight,
      int categorySeverityWeight,
      int urgentScore,
      int highScore,
      int garbageScore,
      GarbageAction garbageAction,
      int detailMinLength,
      int detailMaxLength,
      int chatMaxSelected) {

    public SmartPriority {

      garbageAction = garbageAction == null ? GarbageAction.FLAG : garbageAction;
    }

    public enum GarbageAction {

      /** Keep the report; the score is visible in the queue and detail views. */
      FLAG,

      /** Force the priority down to {@link com.reportdesk.core.ReportPriority#LOW}. */
      DEMOTE,

      /** Close the report automatically with the INSUFFICIENT verdict. */
      AUTO_CLOSE
    }
  }

  /**
   * Bedrock Edition form adaptation (enhancement layer, disabled by default).
   *
   * <p>When enabled, Bedrock clients that join through Geyser + Floodgate can use native Bedrock
   * forms (via the GeyserMC Cumulus library) for the report submission and history flows instead
   * of the Java Inventory GUI. Java Edition players are unaffected and continue to use the
   * standard GUI.
   *
   * <p>Requires GeyserMC (and Floodgate, for Bedrock login) to be installed on the server. The
   * plugin detects the backend reflectively at startup and degrades gracefully when it is absent.
   */
  public record BedrockForms(boolean enabled) {}

  private static String blankTo(String value, String fallback) {

    return value == null || value.isBlank() ? fallback : value;
  }
}
