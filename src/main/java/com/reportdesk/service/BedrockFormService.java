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
import com.reportdesk.core.ReportStatus;
import com.reportdesk.core.Verdict;
import com.reportdesk.i18n.MessageKeys;
import com.reportdesk.i18n.Messages;
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
        plugin.messagesFor(player).plain(MessageKeys.BEDROCK_FORM_REPORT_TITLE, Map.of());

    String contentLabel =
        plugin
            .messages()
            .plain(
                MessageKeys.BEDROCK_FORM_REPORT_CONTENT,
                Map.of("player", presetTarget == null || presetTarget.isBlank() ? "" : presetTarget));

    String categoryLabel = plugin.messagesFor(player).plain(MessageKeys.GENERAL_CATEGORY, Map.of());
    String detailLabel = plugin.messagesFor(player).plain(MessageKeys.GENERAL_DETAIL, Map.of());
    String detailPlaceholder =
        plugin.messagesFor(player).plain(MessageKeys.BEDROCK_FORM_DETAIL_PLACEHOLDER, Map.of());

    String target = presetTarget;

    final List<String> idsForMapping = categoryIds;

    String targetLabel = plugin.messagesFor(player).plain(MessageKeys.GENERAL_TARGET, Map.of());
    String targetPlaceholder =
        plugin.messagesFor(player).plain(MessageKeys.BEDROCK_FORM_TARGET_PLACEHOLDER, Map.of());

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

    player.sendMessage(plugin.messagesFor(player).prefixed(key, placeholders));

    if (result.success()) {

      player.sendMessage(plugin.messagesFor(player).prefixed(MessageKeys.BEDROCK_FORM_REPORT_SUBMITTED, Map.of()));
    }
  }

  /**
   * Opens a Bedrock simple form listing the player's own reports. Tapping a button opens the
   * detail view (currently via text; full form-based detail views would require multiple round
   * trips and are left as future work).
   */
  public boolean openHistoryForm(Player player) {

    return openHistoryForm(player, null);
  }

  /**
   * Opens a Bedrock simple form listing the player's own reports, optionally
   * restricted by the given status filter. Java players, a disabled feature
   * or an unavailable bridge return false so the caller falls back to the
   * Java-side history path.
   */
  public boolean openHistoryForm(Player player, ReportFilter filter) {

    if (!enabled()) {

      return false;
    }

    UUID uuid = player.getUniqueId();

    if (!bridge.isBedrockPlayer(uuid)) {

      return false;
    }

    if (!plugin.repository().isReady()) {

      player.sendMessage(plugin.messagesFor(player).prefixed(MessageKeys.GENERAL_LOADING, Map.of()));

      return false;
    }

    // Force the reporter restriction so a tampered filter can never leak
    // other players' reports (mirrors the GUI history guard).
    ReportFilter effective =
        (filter == null ? ReportFilter.all() : filter).withReporter(uuid);

    List<Report> own = plugin.repository().filter(effective);

    // A simple form has no pagination; show only the most recent configured
    // page-size worth of reports.
    int shown = Math.min(own.size(), plugin.config().reports().guiPageSize());

    List<String> buttons = new ArrayList<>();

    for (int i = 0; i < shown; i++) {
      Report report = own.get(i);

      String statusLabel =
          plugin
              .messagesFor(player)
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

      player.sendMessage(plugin.messagesFor(player).prefixed(MessageKeys.REPORT_OWN_EMPTY, Map.of()));

      return false;
    }

    String title = plugin.messagesFor(player).plain(MessageKeys.BEDROCK_FORM_HISTORY_TITLE, Map.of());

    String content =
        plugin.messagesFor(player).plain(MessageKeys.BEDROCK_FORM_HISTORY_CONTENT, Map.of());

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

      player.sendMessage(plugin.messagesFor(player).prefixed(MessageKeys.GENERAL_LOADING, Map.of()));

      return false;
    }

    int pageSize = plugin.config().reports().guiPageSize();

    var page = plugin.repository().page(filter == null ? ReportFilter.all() : filter, 1, pageSize);

    List<String> buttons = new ArrayList<>();

    for (Report report : page.items()) {

      String statusLabel =
          plugin
              .messagesFor(player)
              .plain(
                  com.reportdesk.service.NotificationService.statusKey(report.status()), Map.of());

      String timeLabel = TimeText.absolute(report.createdAt(), plugin.messagesFor(player));

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

      buttons.add(plugin.messagesFor(player).plain(MessageKeys.GENERAL_NONE, Map.of()));
    }

    String title = plugin.messagesFor(player).plain(MessageKeys.BEDROCK_FORM_ADMIN_TITLE, Map.of());

    String content =
        plugin
            .messagesFor(player)
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
  // Admin-facing: Bedrock detail + action form (the mobile admin workbench)
  // -------------------------------------------------------------------

  /**
   * Opens a Bedrock simple form showing one report's detail plus context-aware admin actions,
   * so a Bedrock admin can act on a report without typing commands. The action buttons depend
   * on the report's current status:
   * <ul>
   *   <li>OPEN    - claim, close</li>
   *   <li>CLAIMED - release, close</li>
   *   <li>terminal- reopen</li>
   * </ul>
   *
   * <p>"Close" opens a second simple form to pick the verdict. Java admins keep the Inventory
   * detail GUI; this is a no-op for them and when the feature or the bridge is unavailable.
   *
   * @param player   the admin
   * @param reportId the report to show and act on
   * @return true when a form was sent or detail was shown; false when disabled / not Bedrock
   */
  public boolean openAdminDetailForm(Player player, long reportId) {

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

      player.sendMessage(plugin.messagesFor(player).prefixed(MessageKeys.GENERAL_LOADING, Map.of()));

      return false;
    }

    Report report = plugin.repository().get(reportId).orElse(null);

    if (report == null) {

      player.sendMessage(
          plugin.messagesFor(player)
              .prefixed(MessageKeys.GENERAL_REPORT_NOT_FOUND, Map.of("id", String.valueOf(reportId))));

      return false;
    }

    Messages m = plugin.messagesFor(player);

    String detailText = buildAdminDetailText(report, m);

    List<String> buttons = new ArrayList<>();

    List<String> actions = new ArrayList<>();

    if (report.status() == ReportStatus.OPEN) {

      actions.add("claim");

      buttons.add(m.plain(MessageKeys.BEDROCK_ADMIN_BTN_CLAIM, Map.of()));
    }

    if (report.status() == ReportStatus.CLAIMED) {

      actions.add("release");

      buttons.add(m.plain(MessageKeys.BEDROCK_ADMIN_BTN_RELEASE, Map.of()));
    }

    if (!report.isTerminal()) {

      actions.add("close");

      buttons.add(m.plain(MessageKeys.BEDROCK_ADMIN_BTN_CLOSE, Map.of()));
    }

    if (report.isTerminal()) {

      actions.add("reopen");

      buttons.add(m.plain(MessageKeys.BEDROCK_ADMIN_BTN_REOPEN, Map.of()));
    }

    if (actions.isEmpty()) {

      plugin.view().sendInfo(player, report);

      return true;
    }

    String title = m.plain(MessageKeys.BEDROCK_FORM_ADMIN_TITLE, Map.of());

    String content = String.format(Locale.ROOT, "#%d%n%s", reportId, detailText);

    final List<String> acts = actions;

    return bridge.sendSimpleForm(
        uuid, title, content, buttons,
        index -> {
          int idx = index;

          Bukkit.getScheduler()
              .runTask(
                  plugin,
                  () -> {
                    Player current = Bukkit.getPlayer(uuid);

                    if (current == null) {

                      return;
                    }

                    Report latest = plugin.repository().get(reportId).orElse(null);

                    if (latest == null) {

                      return;
                    }

                    if (idx < 0 || idx >= acts.size()) {

                      return;
                    }

                    handleAdminAction(current, latest, acts.get(idx));
                  });
        });
  }

  private void handleAdminAction(Player staff, Report report, String action) {

    String resultKey;

    boolean success = false;

    switch (action) {

      case "claim" -> {
        var result = plugin.reportService().claim(report, staff);

        success = result.success();

        resultKey =
            success ? MessageKeys.ADMIN_CLAIMED : MessageKeys.ADMIN_ALREADY_CLAIMED;
      }

      case "release" -> {
        var result = plugin.reportService().release(report, staff);

        success = result.success();

        resultKey = success ? MessageKeys.ADMIN_RELEASED : MessageKeys.ADMIN_NOT_CLAIMED;
      }

      case "reopen" -> {
        var result = plugin.reportService().reopen(report, staff);

        success = result.success();

        resultKey =
            success ? MessageKeys.ADMIN_REOPENED : MessageKeys.ADMIN_NOT_CLOSED;
      }

      case "close" -> {

        openAdminVerdictForm(staff, report);

        return;
      }

      default ->
          {
            return;
          }
    }

    if (!success) {

      staff.sendMessage(
          plugin
              .messagesFor(staff)
              .prefixed(resultKey, Map.of("id", String.valueOf(report.id()))));

      return;
    }

    // Success: show a "done" form with navigation buttons
    openAdminDoneForm(staff, report, resultKey);
  }

  /**
   * Shows a post-action "done" form with two navigation buttons: back to the queue and view
   * the current report. Called on the main thread after a successful admin action.
   */
  private void openAdminDoneForm(Player staff, Report report, String resultKey) {

    Messages m = plugin.messagesFor(staff);

    UUID uuid = staff.getUniqueId();

    String title = m.plain(MessageKeys.BEDROCK_ADMIN_DONE_TITLE, Map.of());

    String content = m.plain(resultKey, Map.of("id", String.valueOf(report.id())));

    String btnQueue = m.plain(MessageKeys.BEDROCK_ADMIN_BTN_BACK_QUEUE, Map.of());

    String btnView = m.plain(MessageKeys.BEDROCK_ADMIN_BTN_VIEW, Map.of());

    bridge.sendSimpleForm(
        uuid,
        title,
        content,
        List.of(btnQueue, btnView),
        index ->
            Bukkit.getScheduler()
                .runTask(
                    plugin,
                    () -> {
                      Player current = Bukkit.getPlayer(uuid);

                      if (current == null) {

                        return;
                      }

                      if (index == 0) {

                        openAdminQueueForm(current, ReportFilter.all());

                      } else if (index == 1) {

                        openAdminDetailForm(current, report.id());
                      }
                    }));
  }

  private void openAdminVerdictForm(Player player, Report report) {

    if (!player.hasPermission("reportdesk.admin.close")) {

      player.sendMessage(
          plugin.messagesFor(player).prefixed(MessageKeys.GENERAL_NO_PERMISSION, Map.of()));

      return;
    }

    UUID uuid = player.getUniqueId();

    Messages m = plugin.messagesFor(player);

    Verdict[] choices = {
      Verdict.RESOLVED,
      Verdict.REJECTED,
      Verdict.DUPLICATE,
      Verdict.INSUFFICIENT
    };

    List<String> options = new ArrayList<>();

    for (Verdict verdict : choices) {

      options.add(m.plain(NotificationService.verdictKey(verdict.name()), Map.of()));
    }

    String title = m.plain(MessageKeys.BEDROCK_ADMIN_VERDICT_TITLE, Map.of());

    String content = String.format(Locale.ROOT, "#%d", report.id());

    String noteLabel = m.plain(MessageKeys.GENERAL_CLOSE_NOTE, Map.of());

    String notePlaceholder = m.plain(MessageKeys.BEDROCK_FORM_DETAIL_PLACEHOLDER, Map.of());

    final Verdict[] verdicts = choices;

    bridge.sendCustomForm2(
        uuid,
        title,
        content,
        m.plain(MessageKeys.GENERAL_VERDICT, Map.of()),
        options,
        noteLabel,
        notePlaceholder,
        (verdictIdx, note) ->
            Bukkit.getScheduler()
                .runTask(
                    plugin,
                    () -> {
                      Player current = Bukkit.getPlayer(uuid);

                      if (current == null) {

                        return;
                      }

                      if (verdictIdx < 0 || verdictIdx >= verdicts.length) {

                        return;
                      }

                      Report latest =
                          plugin.repository().get(report.id()).orElse(report);

                      var result =
                          plugin.reportService()
                              .close(latest, verdicts[verdictIdx], current, note.trim());

                      current.sendMessage(
                          plugin.messagesFor(current)
                              .prefixed(
                                  result.success()
                                      ? MessageKeys.ADMIN_CLOSED
                                      : MessageKeys.ADMIN_ALREADY_CLOSED,
                                  Map.of(
                                      "id", String.valueOf(latest.id()),
                                      "verdict",
                                      plugin.messagesFor(current)
                                          .plain(
                                              NotificationService.verdictKey(
                                                  verdicts[verdictIdx].name()),
                                              Map.of()))));
                    }));
  }

  /** Compact detail block for the admin form content, reusing the existing general.* keys. */
  private String buildAdminDetailText(Report report, Messages m) {

    String statusLabel = m.plain(NotificationService.statusKey(report.status()), Map.of());

    String priorityLabel =
        m.plain(NotificationService.priorityKey(report.priority()), Map.of());

    StringBuilder sb = new StringBuilder();

    sb.append(String.format("%s: %s%n", m.plain(MessageKeys.GENERAL_TARGET, Map.of()), report.targetName()));

    sb.append(
        String.format(
            "%s: %s%n", m.plain(MessageKeys.GENERAL_REPORTER, Map.of()), report.reporterName()));

    sb.append(
        String.format(
            "%s: %s%n",
            m.plain(MessageKeys.GENERAL_CATEGORY, Map.of()),
            plugin.categoryDisplay(report.category())));

    sb.append(
        String.format("%s: %s%n", m.plain(MessageKeys.GENERAL_STATUS, Map.of()), statusLabel));

    sb.append(String.format("%s: %s%n", m.plain(MessageKeys.GENERAL_PRIORITY, Map.of()), priorityLabel));

    if (!report.verdict().isBlank()) {

      sb.append(
          String.format(
              "%s: %s%n",
              m.plain(MessageKeys.GENERAL_VERDICT, Map.of()),
              m.plain(NotificationService.verdictKey(report.verdict()), Map.of())));
    }

    sb.append(
        String.format(
            "%s: %s%n",
            m.plain(MessageKeys.GENERAL_CREATED, Map.of()),
            TimeText.absolute(report.createdAt(), m)));

    if (report.evidence().present()) {

      sb.append(
          String.format(
              "%s: %s%n",
              m.plain(MessageKeys.GENERAL_EVIDENCE, Map.of()),
              report.evidence().shortSummary()));
    }

    if (!report.detail().isBlank()) {

      sb.append(String.format("%s: %s", m.plain(MessageKeys.GENERAL_DETAIL, Map.of()), report.detail()));
    }

    return sb.toString();
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
