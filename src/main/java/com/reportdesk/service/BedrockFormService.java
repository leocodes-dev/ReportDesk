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
import com.reportdesk.config.PluginConfig;
import com.reportdesk.core.Report;
import com.reportdesk.core.ReportCategory;
import com.reportdesk.core.ReportFilter;
import com.reportdesk.i18n.MessageKeys;
import com.reportdesk.util.TimeText;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Bedrock form dispatch layer.
 *
 * <p>Routes ReportDesk's player and admin workflows to the GeyserMC Cumulus form stack when the
 * calling player is a Bedrock client. Java Edition players continue to use the standard Inventory
 * GUI; this service is a no-op for them.
 *
 * <p><b>Threading</b>: Cumulus form callbacks are delivered on the Geyser/Floodgate worker thread.
 * Every handler that touches Bukkit state (players, inventories, repository mutations) is
 * scheduled back to the main thread via {@link Bukkit#getScheduler()}.
 *
 * <p><b>Default-off enhancement</b>: all public entry points first check
 * {@code config.bedrockForms().enabled()}. When disabled the method returns immediately without
 * reading the bridge, touching the repository, or performing any other work (AGENTS.md §5).
 */
public final class BedrockFormService {

  private final ReportDeskPlugin plugin;
  private final BedrockFormBridge bridge;

  public BedrockFormService(ReportDeskPlugin plugin, BedrockFormBridge bridge) {

    this.plugin = plugin;
    this.bridge = bridge;
  }

  /** Exposes the underlying bridge for diagnostics. */
  public BedrockFormBridge bridge() {

    return bridge;
  }

  // -------------------------------------------------------------------
  // Player-facing: submit a report through a Bedrock modal form
  // -------------------------------------------------------------------

  /**
   * Opens a Bedrock custom form for the player to submit a report:
   * dropdown for category, input for details, submit/cancel buttons.
   *
   * <p>Returns true when the form was successfully dispatched to the player;
   * false when the bridge is unavailable, the player is not a Bedrock client,
   * the feature is disabled, or the player has no {@code reportdesk.report} permission.
   */
  public boolean openReportForm(Player player, String presetTarget) {

    if (!enabled()) {

      return false;
    }

    if (player == null || !player.hasPermission("reportdesk.report")) {

      return false;
    }

    UUID uuid = player.getUniqueId();

    if (!bridge.isBedrockPlayer(uuid)) {

      return false;
    }

    List<String> categoryIds = enabledCategoryIds();
    List<String> categoryOptions =
        categoryIds.stream().map(plugin::categoryDisplay).collect(java.util.stream.Collectors.toList());

    String title =
        plugin.messages().plain(MessageKeys.BEDROCK_FORM_REPORT_TITLE, Map.of());

    String contentLabel =
        plugin
            .messages()
            .plain(
                MessageKeys.BEDROCK_FORM_REPORT_CONTENT,
                Map.of("player", presetTarget == null || presetTarget.isBlank() ? "" : presetTarget));

    String categoryLabel = plugin.messages().plain(MessageKeys.GENERAL_CATEGORY, Map.of());
    String detailLabel = plugin.messages().plain(MessageKeys.GENERAL_DETAIL, Map.of());
    String detailPlaceholder =
        plugin.messages().plain(MessageKeys.BEDROCK_FORM_DETAIL_PLACEHOLDER, Map.of());

    String target = presetTarget;

    final List<String> idsForMapping = categoryIds;

    String targetLabel = plugin.messages().plain(MessageKeys.GENERAL_TARGET, Map.of());
    String targetPlaceholder =
        plugin.messages().plain(MessageKeys.BEDROCK_FORM_TARGET_PLACEHOLDER, Map.of());

    boolean ok =
        bridge.sendCustomForm(
            uuid,
            title,
            contentLabel,
            targetLabel,
            targetPlaceholder,
            target == null ? "" : target,
            categoryLabel,
            categoryOptions,
            detailLabel,
            detailPlaceholder,
            (reportTarget, categoryIndex, detail) -> {
              // The player may have typed a target directly into the form; prefer it over the
              // preset so /report (no arg) works end to end on Bedrock.
              String effectiveTarget =
                  reportTarget != null && !reportTarget.isBlank()
                      ? reportTarget.trim()
                      : (target == null ? "" : target);
              String categoryId = resolveCategory(idsForMapping, categoryIndex);
              String detailText = detail == null ? "" : detail;

              Bukkit.getScheduler()
                  .runTask(
                      plugin,
                      () -> {
                        Player current = Bukkit.getPlayer(uuid);

                        if (current == null) {

                          return;
                        }

                        handleReportSubmit(current, effectiveTarget, categoryId, detailText);
                      });
            },
            () ->
                Bukkit.getScheduler()
                    .runTask(
                        plugin,
                        () -> {
                          Player current = Bukkit.getPlayer(uuid);

                          if (current != null) {

                            current.sendMessage(
                                plugin
                                    .messages()
                                    .prefixed(MessageKeys.GENERAL_CANCELLED, Map.of()));
                          }
                        }));

    return ok;
  }

  private void handleReportSubmit(Player player, String targetName, String categoryId, String detail) {

    PluginConfig.Limits limits = plugin.config().limits();

    boolean bypass = player.hasPermission(limits.bypassPermission());

    var result =
        plugin
            .reportService()
            .submit(
                player,
                targetName,
                categoryId,
                detail,
                bypass,
                java.util.List.of(),
                "bedrock-form");

    String key = ReportService.messageKey(result.reason());

    Map<String, String> placeholders = new LinkedHashMap<>(result.placeholders());

    placeholders.put("player", targetName == null ? "" : targetName);
    placeholders.put("target", targetName == null ? "" : targetName);
    placeholders.put("category", plugin.categoryDisplay(categoryId));

    if (!result.success()) {

      placeholders.putIfAbsent("reason", "unknown");
    }

    player.sendMessage(plugin.messages().prefixed(key, placeholders));

    if (result.success()) {

      player.sendMessage(plugin.messages().prefixed(MessageKeys.BEDROCK_FORM_REPORT_SUBMITTED, Map.of()));
    }
  }

  /**
   * Opens a Bedrock simple form listing the player's own reports. Tapping a button opens the
   * detail view (currently via text; full form-based detail views would require multiple round
   * trips and are left as future work).
   */
  public boolean openHistoryForm(Player player) {

    if (!enabled()) {

      return false;
    }

    UUID uuid = player.getUniqueId();

    if (!bridge.isBedrockPlayer(uuid)) {

      return false;
    }

    if (!plugin.repository().isReady()) {

      player.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_LOADING, Map.of()));

      return false;
    }

    List<Report> own = plugin.repository().byReporter(uuid);

    List<String> buttons = new ArrayList<>();

    for (Report report : own) {

      String statusLabel =
          plugin
              .messages()
              .plain(
                  com.reportdesk.service.NotificationService.statusKey(report.status()), Map.of());

      buttons.add(
          String.format(
              Locale.ROOT,
              "#%d %s %s [%s]",
              report.id(),
              report.targetName(),
              plugin.categoryDisplay(report.category()),
              statusLabel));
    }

    if (buttons.isEmpty()) {

      player.sendMessage(plugin.messages().prefixed(MessageKeys.REPORT_OWN_EMPTY, Map.of()));

      return false;
    }

    String title = plugin.messages().plain(MessageKeys.BEDROCK_FORM_HISTORY_TITLE, Map.of());

    String content =
        plugin.messages().plain(MessageKeys.BEDROCK_FORM_HISTORY_CONTENT, Map.of());

    final int[] selectedIndex = {-1};

    return bridge.sendSimpleForm(
        uuid,
        title,
        content,
        buttons,
        index -> {
          // Schedule main-thread handling
          int idx = index;
          Bukkit.getScheduler()
              .runTask(
                  plugin,
                  () -> {
                    Player current = Bukkit.getPlayer(uuid);

                    if (current == null) {

                      return;
                    }

                    if (idx >= 0 && idx < own.size()) {

                      Report selected = own.get(idx);

                      plugin.view().sendOwnInfo(current, selected);
                    }
                  });
        });
  }

  // -------------------------------------------------------------------
  // Admin-facing: Bedrock simple form for the report queue
  // -------------------------------------------------------------------

  /**
   * Opens a Bedrock simple form listing open reports for an admin. Tapping a button opens the
   * report detail view (via text commands when the full admin GUI is not available on Bedrock).
   */
  public boolean openAdminQueueForm(Player player, ReportFilter filter) {

    if (!enabled()) {

      return false;
    }

    if (!player.hasPermission("reportdesk.admin")) {

      return false;
    }

    UUID uuid = player.getUniqueId();

    if (!bridge.isBedrockPlayer(uuid)) {

      return false;
    }

    if (!plugin.repository().isReady()) {

      player.sendMessage(plugin.messages().prefixed(MessageKeys.GENERAL_LOADING, Map.of()));

      return false;
    }

    int pageSize = plugin.config().reports().guiPageSize();

    var page = plugin.repository().page(filter == null ? ReportFilter.all() : filter, 1, pageSize);

    List<String> buttons = new ArrayList<>();

    for (Report report : page.items()) {

      String statusLabel =
          plugin
              .messages()
              .plain(
                  com.reportdesk.service.NotificationService.statusKey(report.status()), Map.of());

      String timeLabel = TimeText.absolute(report.createdAt(), plugin.messages());

      buttons.add(
          String.format(
              Locale.ROOT,
              "#%d %s [%s] %s",
              report.id(),
              report.targetName(),
              statusLabel,
              timeLabel));
    }

    if (buttons.isEmpty()) {

      buttons.add(plugin.messages().plain(MessageKeys.GENERAL_NONE, Map.of()));
    }

    String title = plugin.messages().plain(MessageKeys.BEDROCK_FORM_ADMIN_TITLE, Map.of());

    String content =
        plugin
            .messages()
            .plain(
                MessageKeys.BEDROCK_FORM_ADMIN_CONTENT,
                Map.of(
                    "page", String.valueOf(page.page()),
                    "total", String.valueOf(page.totalItems())));

    return bridge.sendSimpleForm(
        uuid,
        title,
        content,
        buttons,
        index ->
            Bukkit.getScheduler()
                .runTask(
                    plugin,
                    () -> {
                      Player current = Bukkit.getPlayer(uuid);

                      if (current == null) {

                        return;
                      }

                      if (index >= 0 && index < page.items().size()) {

                        Report selected = page.items().get(index);

                        plugin.view().sendInfo(current, selected);
                      }
                    }));
  }

  // -------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------

  /**
   * Single switch check: when {@code bedrock-forms.enabled=false} this returns immediately
   * without any further work (AGENTS.md §5 — "关闭时入口一次开关判断直接返回").
   */
  private boolean enabled() {

    return plugin.config() != null
        && plugin.config().bedrockForms() != null
        && plugin.config().bedrockForms().enabled();
  }

  /** Collects enabled category IDs, in priority order. */
  private List<String> enabledCategoryIds() {

    List<String> ids = new ArrayList<>();

    for (ReportCategory category :
        plugin.config().categories().stream()
            .filter(ReportCategory::enabled)
            .sorted(
                java.util.Comparator.comparingInt(ReportCategory::priority)
                    .reversed()
                    .thenComparing(ReportCategory::id))
            .toList()) {

      ids.add(category.id());
    }

    return ids;
  }

  /**
   * Resolves a dropdown index to a category ID. Returns the default category when the index is out
   * of range.
   */
  private String resolveCategory(List<String> categoryIds, int index) {

    if (index >= 0 && index < categoryIds.size()) {

      return categoryIds.get(index);
    }

    return plugin.config().defaultCategory();
  }
}
