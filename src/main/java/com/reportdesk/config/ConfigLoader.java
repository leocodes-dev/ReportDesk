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

import com.reportdesk.core.ReportCategory;
import com.reportdesk.core.ReportPriority;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Reads and validates config.yml. Invalid values never crash the plugin: they are clamped to a safe
 * range (or replaced with the default) and reported through the warning consumer.
 */
public final class ConfigLoader {

  public static final int DEFAULT_TEXT_PAGE_SIZE = 10;

  public static final int DEFAULT_GUI_PAGE_SIZE = 28;

  public static final int DEFAULT_DETAIL_MIN = 2;

  public static final int DEFAULT_DETAIL_MAX = 200;

  public static final int DEFAULT_NOTE_MAX = 500;

  public static final int DEFAULT_MAX_NOTES = 20;

  public static final String DEFAULT_PLAYER_PATTERN = "^[A-Za-z0-9_]{3,16}$";

  private static final int MISSING = Integer.MIN_VALUE;

  private final Consumer<String> warning;

  public ConfigLoader(Consumer<String> warning) {

    this.warning = warning == null ? ignored -> {} : warning;
  }

  public PluginConfig load(ConfigSource root) {

    if (root == null) {

      root = new MapConfigSource(java.util.Map.of());

      warn("config.yml is empty; using all defaults");
    }

    ConfigSource storage = root.section("storage");

    ConfigSource limits = root.section("limits");

    ConfigSource reports = root.section("reports");

    ConfigSource priority = reports.section("priority");

    ConfigSource slaHours = priority.section("sla-hours");

    ConfigSource replies = reports.section("replies");

    ConfigSource chatReport = reports.section("chat-report");

    ConfigSource gui = root.section("gui");

    ConfigSource notifications = root.section("notifications");

    ConfigSource staff = notifications.section("staff");

    ConfigSource reporter = notifications.section("reporter");

    ConfigSource watchlist = notifications.section("watchlist");

    ConfigSource webhook = notifications.section("webhook");

    ConfigSource sla = notifications.section("sla");

    ConfigSource evidence = root.section("evidence");

    ConfigSource capture = evidence.section("capture");

    ConfigSource blockBreak = evidence.section("block-break");

    ConfigSource movement = evidence.section("movement");

    ConfigSource autoDetect = evidence.section("auto-detect");

    ConfigSource punishments = root.section("punishments");

    ConfigSource audit = root.section("audit");

    ConfigSource configEditor = root.section("config-editor");

    ConfigSource security = root.section("security");

    ConfigSource bedrockForms = root.section("bedrock-forms");

    ConfigSource smartRoot = root.section("smart-priority");

    ConfigSource smartWeights = smartRoot.section("weights");

    ConfigSource smartThresholds = smartRoot.section("thresholds");

    int smartUrgent = integer(smartThresholds, "urgent-score", 90, 0, 100);

    int smartHigh = Math.min(integer(smartThresholds, "high-score", 70, 0, 100), smartUrgent);

    int smartGarbage = Math.min(integer(smartThresholds, "garbage-score", 30, 0, 100), smartHigh);

    PluginConfig.SmartPriority smartSettings =
        new PluginConfig.SmartPriority(
            bool(smartRoot, "enabled", false),
            integer(smartWeights, "reporter-credibility", 30, 0, 100),
            integer(smartWeights, "target-history", 20, 0, 100),
            integer(smartWeights, "detail-quality", 25, 0, 100),
            integer(smartWeights, "chat-evidence", 10, 0, 100),
            integer(smartWeights, "category-severity", 15, 0, 100),
            smartUrgent,
            smartHigh,
            smartGarbage,
            parseSmartGarbageAction(string(smartRoot, "garbage-action", "flag")),
            integer(security, "detail-min-length", 2, 0, 10_000),
            integer(security, "detail-max-length", 200, 1, 100_000),
            integer(chatReport, "max-selected", 5, 1, 100));

    List<ReportCategory> categories = readCategories(reports);

    String defaultCategory = readDefaultCategory(reports, categories);

    PluginConfig.Storage storageSettings =
        new PluginConfig.Storage(
            readDirectory(storage, "directory", "data"),
            bool(storage, "backup-on-save", true),
            integer(storage, "max-backups", 5, 0, 100),
            integer(storage, "backup-interval-minutes", 60, 0, 10080),
            integer(storage, "shutdown-timeout-seconds", 5, 1, 60),
            integer(storage, "compact-after-entries", 50, 1, 100_000),
            integer(storage, "compact-interval-minutes", 360, 0, 100_800));

    PluginConfig.Limits limitSettings =
        new PluginConfig.Limits(
            integer(limits, "command-cooldown-millis", 1000, 0, 60_000),
            integer(limits, "cooldown-seconds", 60, 0, 604_800),
            integer(limits, "daily-limit", 10, 0, 10_000),
            integer(limits, "same-target-interval-seconds", 300, 0, 2_592_000),
            integer(limits, "duplicate-window-seconds", 600, 0, 2_592_000),
            integer(limits, "max-open-per-target", 3, 0, 1_000),
            integer(limits, "min-account-age-minutes", 0, 0, 525_600),
            bool(limits, "block-self-report", true),
            bool(limits, "allow-offline-target", true),
            bool(limits, "require-target-online", false),
            string(limits, "bypass-permission", "reportdesk.bypass.limits"));

    int detailMin = integer(security, "detail-min-length", DEFAULT_DETAIL_MIN, 0, 1_000);

    int detailMax = integer(security, "detail-max-length", DEFAULT_DETAIL_MAX, 1, 2_000);

    if (detailMax < detailMin) {

      warn(
          "security.detail-max-length ("
              + detailMax
              + ") is lower than detail-min-length ("
              + detailMin
              + "); raising it to "
              + detailMin);

      detailMax = detailMin;
    }

    int noteMax = integer(security, "note-max-length", DEFAULT_NOTE_MAX, 1, 4_000);

    String playerPattern = string(security, "player-name-pattern", DEFAULT_PLAYER_PATTERN);

    if (!validPattern(playerPattern)) {

      warn("security.player-name-pattern is not a valid regex; using default");

      playerPattern = DEFAULT_PLAYER_PATTERN;
    }

    PluginConfig.Security securitySettings =
        new PluginConfig.Security(
            integer(security, "admin-cooldown-millis", 300, 0, 60_000),
            integer(security, "pending-timeout-seconds", 120, 10, 3_600),
            detailMin,
            detailMax,
            noteMax,
            integer(security, "max-notes-per-report", DEFAULT_MAX_NOTES, 1, 200),
            bool(security, "detail-required", false),
            bool(security, "allow-color-codes", false),
            playerPattern,
            list(security, "blocked-words", List.of()),
            bool(security, "ip-hash-enabled", false),
            string(security, "ip-hash-salt", ""));

    PluginConfig.Reports reportSettings =
        new PluginConfig.Reports(
            integer(reports, "text-page-size", DEFAULT_TEXT_PAGE_SIZE, 1, 100),
            integer(reports, "gui-page-size", DEFAULT_GUI_PAGE_SIZE, 1, DEFAULT_GUI_PAGE_SIZE),
            bool(reports, "auto-close-enabled", true),
            integer(reports, "auto-close-days", 14, 1, 3_650),
            integer(reports, "retention-days", 90, 0, 36_500),
            integer(reports, "maintenance-interval-minutes", 30, 1, 1_440),
            integer(reports, "stats-top-targets", 3, 1, 20),
            integer(reports, "staff-stats-top", 5, 1, 20),
            defaultCategory,
            new PluginConfig.Priority(
                bool(priority, "enabled", true),
                integer(priority, "repeat-offender-threshold", 3, 0, 1_000),
                readPriority(priority, "repeat-offender-level", ReportPriority.HIGH),
                integer(priority, "overdue-check-minutes", 15, 1, 1_440),
                bool(priority, "overdue-alert-enabled", true),
                integer(slaHours, "urgent", 2, 0, 8_760),
                integer(slaHours, "high", 6, 0, 8_760),
                integer(slaHours, "normal", 24, 0, 8_760),
                integer(slaHours, "low", 72, 0, 8_760)),
            new PluginConfig.Replies(
                bool(replies, "enabled", true),
                bool(replies, "allow-player-replies", true),
                integer(replies, "max-length", 300, 16, 2_000),
                integer(replies, "max-per-report", 50, 1, 500),
                bool(replies, "notify", true)),
            readChatReport(chatReport, categories));

    ConfigSource guiSounds = gui.section("sounds");

    ConfigSource guiAdmin = gui.section("admin");

    PluginConfig.Admin adminGuiSettings =
        new PluginConfig.Admin(
            bool(guiAdmin, "enabled", true), bool(guiAdmin, "confirm-destructive", true));

    PluginConfig.Gui guiSettings =
        new PluginConfig.Gui(
            bool(gui, "enabled", true),
            bool(gui, "fill-empty-slots", true),
            bool(gui, "close-on-action", false),
            bool(guiSounds, "enabled", true),
            string(guiSounds, "click", "ui.button.click"),
            string(guiSounds, "success", "entity.player.levelup"),
            string(guiSounds, "error", "entity.villager.no"),
            adminGuiSettings);

    String webhookUrl = string(webhook, "url", "");

    boolean webhookEnabled = bool(webhook, "enabled", false);

    if (webhookEnabled && !isHttpUrl(webhookUrl)) {

      warn(
          "notifications.webhook.enabled=true but url is not a valid http(s) URL; webhook"
              + " disabled");

      webhookEnabled = false;
    }

    PluginConfig.Webhook webhookSettings =
        new PluginConfig.Webhook(
            webhookEnabled,
            webhookUrl,
            integer(webhook, "timeout-seconds", 5, 1, 30),
            string(webhook, "username", "ReportDesk"),
            string(webhook, "avatar-url", ""),
            bool(webhook, "events.created", true),
            bool(webhook, "events.claimed", true),
            bool(webhook, "events.closed", true),
            integer(webhook, "retry-count", 2, 0, 5),
            integer(webhook, "retry-base-seconds", 5, 1, 300),
            bool(webhook, "failure-alert-enabled", true),
            string(webhook, "secondary-url", ""),
            bool(webhook, "sla-events-enabled", true));

    PluginConfig.Notifications notificationSettings =
        new PluginConfig.Notifications(
            bool(staff, "enabled", true),
            string(staff, "permission", "reportdesk.notify"),
            bool(staff, "sound-enabled", true),
            string(staff, "sound", "block.note_block.pling"),
            bool(reporter, "enabled", true),
            bool(reporter, "queue-offline", true),
            integer(reporter, "max-queued", 20, 1, 500),
            integer(reporter, "queue-retention-days", 30, 0, 3_650),
            bool(watchlist, "enabled", true),
            integer(watchlist, "max-targets", 20, 1, 200),
            bool(watchlist, "notify-on-report", true),
            bool(notifications, "broadcast.enabled", false),
            webhookSettings,
            new PluginConfig.Sla(
                bool(sla, "weekly-enabled", false),
                integer(sla, "interval-hours", 168, 6, 8_760)));

    PluginConfig.Evidence evidenceSettings =
        new PluginConfig.Evidence(
            bool(evidence, "enabled", false),
            bool(evidence, "capture-on-report", true),
            integer(evidence, "retention-days", 30, 0, 36_500),
            integer(evidence, "chat-log-size", 20, 1, 500),
            integer(evidence, "chat-max-length", 256, 16, 2_000),
            bool(capture, "location", true),
            bool(capture, "game-mode", true),
            bool(capture, "health", true),
            bool(capture, "inventory", true),
            integer(capture, "inventory-max-items", 41, 0, 41),
            integer(capture, "display-name-max-length", 80, 8, 500),
            bool(capture, "chat-log", true),
            bool(capture, "ping", true),
            new PluginConfig.BlockBreak(
                bool(blockBreak, "enabled", false),
                integer(blockBreak, "max-per-player", 100, 1, 100_000)),
            new PluginConfig.Movement(
                bool(movement, "enabled", false),
                integer(movement, "min-interval-ticks", 20, 1, 200),
                integer(movement, "max-samples", 60, 1, 1_000)),
            new PluginConfig.AutoDetect(
                bool(autoDetect, "enabled", false),
                integer(autoDetect, "check-interval-seconds", 5, 1, 300),
                integer(autoDetect, "window-seconds", 10, 1, 600),
                integer(autoDetect, "block-break-threshold", 40, 1, 100_000),
                integer(autoDetect, "cooldown-seconds", 300, 0, 86_400),
                bool(autoDetect, "create-report", true),
                bool(autoDetect, "notify-staff", true),
                bool(autoDetect, "chat-spam-enabled", false),
                integer(autoDetect, "chat-spam-threshold", 60, 2, 1_000)));

    PluginConfig.Punishments punishmentSettings =
        new PluginConfig.Punishments(
            bool(punishments, "enabled", false),
            list(punishments, "resolved-commands", List.of()),
            list(punishments, "rejected-commands", List.of()));

    PluginConfig.Audit auditSettings =
        new PluginConfig.Audit(
            bool(audit, "enabled", true),
            integer(audit, "retention-days", 180, 0, 36_500),
            integer(audit, "page-size", DEFAULT_TEXT_PAGE_SIZE, 1, 100),
            integer(audit, "max-size-mb", 0, 0, 10_240));

    PluginConfig.ConfigEditor editorSettings =
        new PluginConfig.ConfigEditor(
            bool(configEditor, "enabled", true),
            bool(configEditor, "backup-on-save", true),
            integer(configEditor, "max-backups", 5, 0, 50),
            integer(configEditor, "max-value-length", 256, 1, 4_096));

    // Bedrock Forms enhancement layer: disabled by default. When enabled, the
    // plugin routes Bedrock clients (via Geyser + Floodgate) to the native
    // Cumulus form stack instead of the Java Inventory GUI.
    PluginConfig.BedrockForms bedrockFormsSettings =
        new PluginConfig.BedrockForms(bool(bedrockForms, "enabled", false));

    String language = string(root, "language", "zh_CN");

    if (!language.matches("[A-Za-z0-9_-]+")) {

      warn("language '" + language + "' is invalid; using zh_CN");

      language = "zh_CN";
    }

    String serverName = string(root, "server-name", "ReportDesk Server");

    if (serverName.isBlank()) {

      serverName = "ReportDesk Server";
    }

    return new PluginConfig(
        serverName,
        language,
        storageSettings,
        limitSettings,
        reportSettings,
        guiSettings,
        notificationSettings,
        evidenceSettings,
        punishmentSettings,
        auditSettings,
        editorSettings,
        securitySettings,
        categories,
        defaultCategory,
        smartSettings,
        bedrockFormsSettings);
  }

  private PluginConfig.SmartPriority.GarbageAction parseSmartGarbageAction(String raw) {

    String normalized = raw == null ? "" : raw.trim().toLowerCase(java.util.Locale.ROOT);

    return switch (normalized) {
      case "demote", "lower" -> PluginConfig.SmartPriority.GarbageAction.DEMOTE;

      case "auto-close", "autoclose", "close", "purge" ->
          PluginConfig.SmartPriority.GarbageAction.AUTO_CLOSE;

      default -> {
        if (!normalized.isEmpty() && !normalized.equals("flag")) {

          warn(
              "smart-priority.garbage-action '"
                  + raw
                  + "' is invalid; using 'flag' (flag/demote/auto-close)");
        }

        yield PluginConfig.SmartPriority.GarbageAction.FLAG;
      }
    };
  }

  private List<ReportCategory> readCategories(ConfigSource reports) {

    ConfigSource section = reports.section("categories");

    Set<String> keys = section == null ? Set.of() : section.keys("");

    List<ReportCategory> categories = new ArrayList<>();

    for (String key : keys) {

      if (!section.isSection(key)) {

        warn("reports.categories." + key + " is not a section; skipped");

        continue;
      }

      ConfigSource category = section.section(key);

      categories.add(
          new ReportCategory(
              key,
              string(category, "display", key),
              string(category, "icon", "PAPER"),
              list(category, "aliases", List.of()),
              bool(category, "enabled", true),
              integer(category, "priority", 0, -1_000_000, 1_000_000),
              list(category, "description", List.of()),
              readPriority(category, "severity", ReportPriority.NORMAL)));
    }

    if (categories.isEmpty()) {

      warn("no report categories configured; adding fallback 'other' category");

      categories.add(
          new ReportCategory(
              "other", "Other", "WRITABLE_BOOK", List.of("misc"), true, 0, List.of()));
    }

    if (categories.stream().noneMatch(ReportCategory::enabled)) {

      warn("all report categories are disabled; enabling 'other' fallback");

      ReportCategory fallback =
          categories.stream()
              .filter(category -> category.id().equals("other"))
              .findFirst()
              .orElseGet(
                  () ->
                      new ReportCategory(
                          "other", "Other", "WRITABLE_BOOK", List.of("misc"), true, 0, List.of()));

      categories.removeIf(category -> category.id().equals(fallback.id()));

      categories.add(
          new ReportCategory(
              fallback.id(),
              fallback.display(),
              fallback.icon(),
              fallback.aliases(),
              true,
              fallback.priority(),
              fallback.description()));
    }

    return List.copyOf(categories);
  }

  private PluginConfig.ChatReport readChatReport(
      ConfigSource section, List<ReportCategory> categories) {

    boolean enabled = bool(section, "enabled", false);

    String requested = string(section, "category", "chat");

    String normalized = requested == null ? "" : requested.trim().toLowerCase(Locale.ROOT);

    String categoryId = normalized.isBlank() ? "chat" : normalized;

    boolean found = false;

    for (ReportCategory category : categories) {

      boolean byId = category.id().equals(normalized);

      boolean byAlias =
          category.aliases().stream()
              .anyMatch(alias -> alias != null && alias.equalsIgnoreCase(normalized));

      if (byId || byAlias) {

        categoryId = category.id();

        found = true;

        if (!category.enabled()) {

          if (enabled) {

            warn(
                "reports.chat-report.category '"
                    + requested
                    + "' is disabled; chat picker disabled");
          }

          enabled = false;
        }

        break;
      }
    }

    if (!found && enabled) {

      warn(
          "reports.chat-report.category '"
              + requested
              + "' does not match any report category; chat picker disabled");

      enabled = false;
    }

    return new PluginConfig.ChatReport(
        enabled, categoryId, integer(section, "max-selected", 5, 1, 100));
  }

  private String readDefaultCategory(ConfigSource reports, List<ReportCategory> categories) {

    String requested = string(reports, "default-category", "other");

    String normalized = requested.trim().toLowerCase(Locale.ROOT);

    for (ReportCategory category : categories) {

      if (category.id().equals(normalized)
          || category.aliases().stream().anyMatch(alias -> alias.equalsIgnoreCase(normalized))) {

        if (!category.enabled()) {

          warn(
              "reports.default-category '"
                  + requested
                  + "' is disabled; using first enabled category");

          break;
        }

        return category.id();
      }
    }

    String fallback =
        categories.stream()
            .filter(ReportCategory::enabled)
            .map(ReportCategory::id)
            .findFirst()
            .orElse(categories.get(0).id());

    if (!fallback.equalsIgnoreCase(requested)) {

      warn("reports.default-category '" + requested + "' not found; using '" + fallback + "'");
    }

    return fallback;
  }

  private int integer(ConfigSource source, String path, int fallback, int min, int max) {

    int raw = source.getInt(path, MISSING);

    if (raw == MISSING) {

      warn("missing config value " + path + "; using default " + fallback);

      return clamp(fallback, min, max);
    }

    if (raw < min || raw > max) {

      int clamped = clamp(raw, min, max);

      warn(path + "=" + raw + " is outside [" + min + ", " + max + "]; using " + clamped);

      return clamped;
    }

    return raw;
  }

  private boolean bool(ConfigSource source, String path, boolean fallback) {

    return source.getBoolean(path, fallback);
  }

  private String string(ConfigSource source, String path, String fallback) {

    String value = source.getString(path, null);

    if (value == null) {

      return fallback;
    }

    return value;
  }

  private List<String> list(ConfigSource source, String path, List<String> fallback) {

    return source.getStringList(path, fallback);
  }

  private ReportPriority readPriority(ConfigSource source, String path, ReportPriority fallback) {

    String raw = string(source, path, fallback.id());

    ReportPriority parsed = ReportPriority.parse(raw, null);

    if (parsed == null) {

      warn(
          path
              + "='"
              + raw
              + "' is not a valid priority (low/normal/high/urgent); using "
              + fallback.id());

      return fallback;
    }

    return parsed;
  }

  private String readDirectory(ConfigSource storage, String path, String fallback) {

    String value = string(storage, path, fallback);

    if (value == null || value.isBlank()) {

      return fallback;
    }

    String normalized = value.replace('\\', '/');

    if (normalized.startsWith("/")
        || normalized.matches("^[A-Za-z]:.*")
        || normalized.contains("..")) {

      warn(path + " must be a relative directory without '..'; using " + fallback);

      return fallback;
    }

    return normalized;
  }

  private boolean validPattern(String pattern) {

    try {

      Pattern.compile(pattern);

      return true;

    } catch (PatternSyntaxException exception) {

      return false;
    }
  }

  private boolean isHttpUrl(String value) {

    if (value == null || value.isBlank()) {

      return false;
    }

    try {

      java.net.URI uri = java.net.URI.create(value.trim());

      return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
          && uri.getHost() != null;

    } catch (IllegalArgumentException exception) {

      return false;
    }
  }

  private static int clamp(int value, int min, int max) {

    return Math.max(min, Math.min(max, value));
  }

  private void warn(String message) {

    warning.accept("[config] " + message);
  }
}
