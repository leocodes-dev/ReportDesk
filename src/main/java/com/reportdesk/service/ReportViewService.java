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
import com.reportdesk.core.AuditEntry;
import com.reportdesk.core.ChatSample;
import com.reportdesk.core.Evidence;
import com.reportdesk.core.Page;
import com.reportdesk.core.Report;
import com.reportdesk.core.ReportFilter;
import com.reportdesk.core.ReportReply;
import com.reportdesk.core.ReportStats;
import com.reportdesk.i18n.MessageKeys;
import com.reportdesk.i18n.Messages;
import com.reportdesk.util.TimeText;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

/**
 * Text presentation shared by /reports and the GUI. Everything reads the in-memory repository or
 * pre-fetched values; this class never performs IO.
 */
public final class ReportViewService {

  private final ReportDeskPlugin plugin;

  public ReportViewService(ReportDeskPlugin plugin) {

    this.plugin = plugin;
  }

  public Map<String, String> placeholders(Report report) {

    return plugin.notification().placeholders(report);
  }

  public void sendInfo(CommandSender sender, Report report) {

    if (sender == null || report == null) {

      return;
    }

    Messages messages = plugin.messages();

    sender.sendMessage(
        messages.component(
            MessageKeys.ADMIN_INFO_HEADER, Map.of("id", String.valueOf(report.id()))));

    infoLine(sender, MessageKeys.GENERAL_TARGET, report.targetName());

    infoLine(sender, MessageKeys.GENERAL_REPORTER, report.reporterName());

    infoLine(sender, MessageKeys.GENERAL_CATEGORY, plugin.categoryDisplay(report.category()));

    infoLine(
        sender,
        MessageKeys.GENERAL_STATUS,
        messages.plain(NotificationService.statusKey(report.status())));

    infoLine(sender, MessageKeys.GENERAL_CREATED, TimeText.absolute(report.createdAt(), messages));

    infoLine(
        sender,
        MessageKeys.GENERAL_CLAIMED_BY,
        report.claimedByName().isBlank()
            ? messages.plain(MessageKeys.GENERAL_NONE)
            : report.claimedByName());

    infoLine(
        sender,
        MessageKeys.GENERAL_CLOSED_BY,
        report.closedByName().isBlank()
            ? messages.plain(MessageKeys.GENERAL_NONE)
            : report.closedByName());

    infoLine(
        sender,
        MessageKeys.GENERAL_VERDICT,
        report.verdict().isBlank()
            ? messages.plain(MessageKeys.GENERAL_NONE)
            : messages.plain(NotificationService.verdictKey(report.verdict())));

    infoLine(
        sender,
        MessageKeys.GENERAL_CLOSE_NOTE,
        report.closeNote().isBlank()
            ? messages.plain(MessageKeys.GENERAL_NONE)
            : report.closeNote());

    infoLine(
        sender,
        MessageKeys.GENERAL_TARGET_HISTORY,
        messages.plain(
            MessageKeys.GENERAL_TARGET_HISTORY_VALUE,
            Map.of(
                "open", String.valueOf(plugin.repository().countOpenForTarget(report.targetName())),
                "total", String.valueOf(plugin.repository().countForTarget(report.targetName())))));

    infoLine(
        sender,
        MessageKeys.GENERAL_PRIORITY,
        messages.plain(NotificationService.priorityKey(report.priority())));

    infoLine(sender, MessageKeys.GENERAL_SLA, slaText(report));

    infoLine(sender, MessageKeys.GENERAL_REPLIES, replySummary(report));

    infoLine(
        sender,
        MessageKeys.GENERAL_DETAIL,
        report.detail().isBlank() ? messages.plain(MessageKeys.GENERAL_NONE) : report.detail());

    infoLine(sender, MessageKeys.GENERAL_NOTES, String.valueOf(report.notes().size()));

    infoLine(
        sender,
        MessageKeys.GENERAL_EVIDENCE,
        report.evidence().present()
            ? messages.plain(MessageKeys.GENERAL_YES)
                + " ("
                + report.evidence().shortSummary()
                + ")"
            : messages.plain(MessageKeys.GENERAL_NO));
  }

  /**
   * Player-safe view of a report. It omits staff-only data such as internal
   *
   * <p>notes and evidence metadata while still showing the close note/verdict.
   */
  public void sendOwnInfo(CommandSender sender, Report report) {

    if (sender == null || report == null) {

      return;
    }

    Messages messages = plugin.messages();

    sender.sendMessage(
        messages.component(
            MessageKeys.REPORT_OWN_INFO_HEADER, Map.of("id", String.valueOf(report.id()))));

    ownInfoLine(sender, MessageKeys.GENERAL_TARGET, report.targetName());

    ownInfoLine(sender, MessageKeys.GENERAL_CATEGORY, plugin.categoryDisplay(report.category()));

    ownInfoLine(
        sender,
        MessageKeys.GENERAL_STATUS,
        messages.plain(NotificationService.statusKey(report.status())));

    ownInfoLine(
        sender, MessageKeys.GENERAL_CREATED, TimeText.absolute(report.createdAt(), messages));

    ownInfoLine(
        sender,
        MessageKeys.GENERAL_CLAIMED_BY,
        report.claimedByName().isBlank()
            ? messages.plain(MessageKeys.GENERAL_NONE)
            : report.claimedByName());

    if (!report.claimedByName().isBlank()) {

      ownInfoLine(sender, MessageKeys.GENERAL_CLAIMED_AT, TimeText.absolute(report.claimedAt(), messages));
    }

    ownInfoLine(
        sender,
        MessageKeys.GENERAL_CLOSED_BY,
        report.closedByName().isBlank()
            ? messages.plain(MessageKeys.GENERAL_NONE)
            : report.closedByName());

    if (!report.closedByName().isBlank()) {

      ownInfoLine(sender, MessageKeys.GENERAL_CLOSED_AT, TimeText.absolute(report.closedAt(), messages));
    }

    ownInfoLine(
        sender,
        MessageKeys.GENERAL_VERDICT,
        report.verdict().isBlank()
            ? messages.plain(MessageKeys.GENERAL_NONE)
            : messages.plain(NotificationService.verdictKey(report.verdict())));

    ownInfoLine(
        sender,
        MessageKeys.GENERAL_CLOSE_NOTE,
        report.closeNote().isBlank()
            ? messages.plain(MessageKeys.GENERAL_NONE)
            : report.closeNote());

    ownInfoLine(
        sender,
        MessageKeys.GENERAL_EVIDENCE,
        report.evidence().present()
            ? report.evidence().shortSummary()
            : messages.plain(MessageKeys.GENERAL_NONE));

    ownInfoLine(
        sender,
        MessageKeys.GENERAL_TARGET_HISTORY,
        messages.plain(
            MessageKeys.GENERAL_TARGET_HISTORY_VALUE,
            Map.of(
                "open",
                String.valueOf(plugin.repository().countOpenForTarget(report.targetName())),
                "total", String.valueOf(plugin.repository().countForTarget(report.targetName())))));

    ownInfoLine(
        sender,
        MessageKeys.GENERAL_PRIORITY,
        messages.plain(NotificationService.priorityKey(report.priority())));

    ownInfoLine(sender, MessageKeys.GENERAL_SLA, slaText(report));

    ownInfoLine(sender, MessageKeys.GENERAL_REPLIES, replySummary(report));

    report
        .lastReply()
        .ifPresent(
            reply ->
                ownInfoLine(
                    sender,
                    MessageKeys.GENERAL_LAST_REPLY,
                    messages.plain(
                        MessageKeys.REPORT_REPLY_LINE,
                        Map.of(
                            "author", reply.authorName(),
                            "time", TimeText.absolute(reply.createdAt(), messages),
                            "text", reply.text()))));

    ownInfoLine(
        sender,
        MessageKeys.GENERAL_DETAIL,
        report.detail().isBlank() ? messages.plain(MessageKeys.GENERAL_NONE) : report.detail());
  }

  public void sendStats(CommandSender sender) {

    if (sender == null) {

      return;
    }

    Messages messages = plugin.messages();

    // Unsorted snapshot: the statistics pass does not need the newest-first order.
    ReportStats stats = ReportStats.compute(plugin.repository().snapshot());

    sender.sendMessage(messages.component(MessageKeys.ADMIN_STATS_HEADER, Map.of()));

    sender.sendMessage(
        messages.component(
            MessageKeys.ADMIN_STATS_TOTAL, Map.of("total", String.valueOf(stats.total()))));

    sender.sendMessage(
        messages.component(
            MessageKeys.ADMIN_STATS_STATUS,
            Map.of(
                "open", String.valueOf(stats.open()),
                "claimed", String.valueOf(stats.claimed()),
                "closed", String.valueOf(stats.closed()))));

    sender.sendMessage(
        messages.component(
            MessageKeys.ADMIN_STATS_VERDICTS,
            Map.of(
                "resolved", String.valueOf(stats.resolved()),
                "rejected", String.valueOf(stats.rejected()),
                "duplicate", String.valueOf(stats.duplicate()))));

    sender.sendMessage(
        messages.component(
            MessageKeys.ADMIN_STATS_AVG,
            Map.of("duration", TimeText.duration(stats.averageHandlingMillis(), messages))));

    for (Map.Entry<String, Integer> entry : stats.byCategory().entrySet()) {

      sender.sendMessage(
          messages.component(
              MessageKeys.ADMIN_STATS_CATEGORY,
              Map.of(
                  "category", plugin.categoryDisplay(entry.getKey()),
                  "count", String.valueOf(entry.getValue()))));
    }

    for (Map.Entry<String, Integer> entry :
        stats.topTargets(plugin.config().reports().statsTopTargets())) {

      sender.sendMessage(
          messages.component(
              MessageKeys.ADMIN_STATS_TOP_TARGET,
              Map.of(
                  "target", entry.getKey(),
                  "count", String.valueOf(entry.getValue()))));
    }

    for (Map.Entry<String, Integer> entry :
        stats.topStaff(plugin.config().reports().staffStatsTop())) {

      sender.sendMessage(
          messages.component(
              MessageKeys.ADMIN_STATS_STAFF,
              Map.of(
                  "staff", entry.getKey(),
                  "count", String.valueOf(entry.getValue()))));
    }
  }

  /**
   * Renders a report conversation. {@code staffView} selects the admin
   *
   * <p>wording; both audiences see exactly the same replies.
   */
  public void sendReplies(
      CommandSender sender, Report report, Page<ReportReply> page, boolean staffView) {

    if (sender == null || report == null || page == null) {

      return;
    }

    Messages messages = plugin.messages();

    sender.sendMessage(
        messages.component(
            staffView ? MessageKeys.ADMIN_REPLIES_HEADER : MessageKeys.REPORT_REPLIES_HEADER,
            Map.of("id", String.valueOf(report.id()), "count", String.valueOf(page.totalItems()))));

    if (page.items().isEmpty()) {

      sender.sendMessage(
          messages.component(
              staffView ? MessageKeys.ADMIN_REPLIES_EMPTY : MessageKeys.REPORT_REPLIES_EMPTY,
              Map.of()));

      return;
    }

    for (ReportReply reply : page.items()) {

      sender.sendMessage(
          messages.component(
              staffView ? MessageKeys.ADMIN_REPLY_LINE : MessageKeys.REPORT_REPLY_LINE,
              Map.of(
                  "author", reply.authorName(),
                  "time", TimeText.absolute(reply.createdAt(), messages),
                  "text", reply.text())));
    }

    if (page.totalPages() > 1) {

      sender.sendMessage(
          messages.component(
              MessageKeys.GENERAL_PAGE,
              Map.of(
                  "page", String.valueOf(page.page()),
                  "pages", String.valueOf(page.totalPages()))));
    }
  }

  public void sendList(CommandSender sender, Page<Report> page, ReportFilter filter) {

    if (sender == null || page == null) {

      return;
    }

    Messages messages = plugin.messages();

    sender.sendMessage(
        messages.component(
            MessageKeys.ADMIN_LIST_HEADER,
            Map.of(
                "filter", filterLabel(filter),
                "count", String.valueOf(page.totalItems()))));

    if (page.items().isEmpty()) {

      sender.sendMessage(messages.component(MessageKeys.GUI_EMPTY, Map.of()));

      return;
    }

    for (Report report : page.items()) {

      sender.sendMessage(messages.component(MessageKeys.ADMIN_LIST_LINE, linePlaceholders(report)));
    }

    if (page.totalPages() > 1) {

      sender.sendMessage(
          messages.component(
              MessageKeys.ADMIN_LIST_FOOTER,
              Map.of(
                  "page", String.valueOf(page.page()),
                  "pages", String.valueOf(page.totalPages()),
                  "next", String.valueOf(Math.min(page.totalPages(), page.page() + 1)))));
    }
  }

  public void sendOwnReports(CommandSender sender, Page<Report> page) {

    if (sender == null || page == null) {

      return;
    }

    Messages messages = plugin.messages();

    sender.sendMessage(
        messages.component(
            MessageKeys.REPORT_OWN_HEADER, Map.of("count", String.valueOf(page.totalItems()))));

    if (page.items().isEmpty()) {

      sender.sendMessage(messages.component(MessageKeys.REPORT_OWN_EMPTY, Map.of()));

      return;
    }

    for (Report report : page.items()) {

      sender.sendMessage(messages.component(MessageKeys.REPORT_OWN_LINE, linePlaceholders(report)));
    }

    if (page.totalPages() > 1) {

      sender.sendMessage(
          messages.component(
              MessageKeys.GENERAL_PAGE,
              Map.of(
                  "page", String.valueOf(page.page()),
                  "pages", String.valueOf(page.totalPages()))));
    }
  }

  /**
   * Player report history with an optional status filter. The list is scoped to the sender's own
   * reports only; nothing on this path can reach another player's data.
   */
  public void sendHistory(CommandSender sender, UUID reporterId, ReportFilter filter, int page) {

    if (sender == null || reporterId == null) {

      return;
    }

    Messages messages = plugin.messages();

    ReportFilter effective = (filter == null ? ReportFilter.all() : filter).withReporter(reporterId);

    int pageSize = plugin.config().reports().textPageSize();

    Page<Report> result = plugin.repository().page(effective, page, pageSize);

    List<Report> own = plugin.repository().byReporter(reporterId);

    int open = 0;

    int unread = 0;

    for (Report report : own) {

      if (report.isOpen()) {

        open++;
      }

      unread += report.unreadReplies();
    }

    sender.sendMessage(
        messages.component(
            MessageKeys.REPORT_HISTORY_HEADER,
            Map.of(
                "filter", filterLabel(effective),
                "count", String.valueOf(result.totalItems()))));

    sender.sendMessage(
        messages.component(
            MessageKeys.REPORT_HISTORY_STATS,
            Map.of(
                "open", String.valueOf(open),
                "closed", String.valueOf(own.size() - open),
                "unread", String.valueOf(unread))));

    if (result.items().isEmpty()) {

      sender.sendMessage(messages.component(MessageKeys.REPORT_OWN_EMPTY, Map.of()));

    } else {

      for (Report report : result.items()) {

        sender.sendMessage(messages.component(MessageKeys.REPORT_OWN_LINE, linePlaceholders(report)));
      }
    }

    if (result.totalPages() > 1) {

      sender.sendMessage(
          messages.component(
              MessageKeys.GENERAL_PAGE,
              Map.of(
                  "page", String.valueOf(result.page()),
                  "pages", String.valueOf(result.totalPages()))));
    }

    sender.sendMessage(messages.component(MessageKeys.REPORT_HISTORY_HINT, Map.of()));
  }

  public void sendAudit(CommandSender sender, int page, List<String> lines) {

    if (sender == null) {

      return;
    }

    Messages messages = plugin.messages();

    sender.sendMessage(
        messages.component(
            MessageKeys.ADMIN_AUDIT_HEADER, Map.of("page", String.valueOf(Math.max(1, page)))));

    if (lines == null || lines.isEmpty()) {

      sender.sendMessage(messages.component(MessageKeys.ADMIN_AUDIT_EMPTY, Map.of()));

      return;
    }

    for (String line : lines) {

      AuditEntry entry = AuditEntry.fromLine(line);

      if (entry == null) {

        continue;
      }

      sender.sendMessage(
          messages.component(
              MessageKeys.ADMIN_AUDIT_LINE,
              Map.of(
                  "time", TimeText.absolute(entry.timestamp(), messages),
                  "actor", entry.actorName(),
                  "action", entry.action(),
                  "report", entry.reportId() <= 0 ? "-" : String.valueOf(entry.reportId()),
                  "detail", entry.detail())));
    }
  }

  public void sendEvidence(CommandSender sender, Report report, Evidence evidence) {

    if (sender == null) {

      return;
    }

    Messages messages = plugin.messages();

    long reportId = report == null ? 0L : report.id();

    sender.sendMessage(
        messages.component(MessageKeys.EVIDENCE_HEADER, Map.of("id", String.valueOf(reportId))));

    if (evidence == null) {

      sender.sendMessage(messages.component(MessageKeys.EVIDENCE_NOT_CAPTURED, Map.of()));

      return;
    }

    sender.sendMessage(
        messages.component(
            MessageKeys.EVIDENCE_TARGET,
            Map.of(
                "target", evidence.targetName(),
                "online",
                    messages.plain(
                        evidence.targetOnline()
                            ? MessageKeys.GENERAL_YES
                            : MessageKeys.GENERAL_NO))));

    if (!evidence.targetOnline()) {

      sender.sendMessage(messages.component(MessageKeys.EVIDENCE_OFFLINE, Map.of()));
    }

    sender.sendMessage(
        messages.component(
            MessageKeys.EVIDENCE_CAPTURED_AT,
            Map.of("time", TimeText.absolute(evidence.capturedAt(), messages))));

    if (!evidence.world().isBlank()) {

      sender.sendMessage(
          messages.component(
              MessageKeys.EVIDENCE_LOCATION,
              Map.of(
                  "world", evidence.world(),
                  "x", format(evidence.x()),
                  "y", format(evidence.y()),
                  "z", format(evidence.z()))));
    }

    if (!evidence.gameMode().isBlank()) {

      sender.sendMessage(
          messages.component(MessageKeys.EVIDENCE_GAME_MODE, Map.of("mode", evidence.gameMode())));
    }

    sender.sendMessage(
        messages.component(
            MessageKeys.EVIDENCE_HEALTH,
            Map.of(
                "health", format(evidence.health()),
                "food", String.valueOf(evidence.foodLevel()),
                "level", String.valueOf(evidence.expLevel()),
                "ping", String.valueOf(evidence.ping()))));

    sender.sendMessage(
        messages.component(
            MessageKeys.EVIDENCE_INVENTORY,
            Map.of(
                "count",
                String.valueOf(evidence.inventory().size()),
                "items",
                evidence.inventory().isEmpty()
                    ? messages.plain(MessageKeys.EVIDENCE_INVENTORY_EMPTY)
                    : String.join(", ", evidence.inventory()))));

    if (!evidence.armor().isEmpty()) {

      sender.sendMessage(
          messages.component(
              MessageKeys.EVIDENCE_ARMOR, Map.of("items", String.join(", ", evidence.armor()))));
    }

    sender.sendMessage(messages.component(MessageKeys.EVIDENCE_CHAT, Map.of()));

    if (evidence.chat().isEmpty()) {

      sender.sendMessage(messages.component(MessageKeys.EVIDENCE_CHAT_EMPTY, Map.of()));

    } else {

      for (ChatSample sample : evidence.chat()) {

        sender.sendMessage(
            messages.component(
                MessageKeys.EVIDENCE_CHAT_LINE,
                Map.of(
                    "time", TimeText.absolute(sample.createdAt(), messages),
                    "player", sample.playerName(),
                    "message", sample.message())));
      }
    }

    sender.sendMessage(
        messages.component(
            MessageKeys.EVIDENCE_BLOCK_BREAK,
            Map.of("count", String.valueOf(evidence.blockBreaks().size()))));

    for (var sample : evidence.blockBreaks()) {

      sender.sendMessage(
          messages.component(
              MessageKeys.EVIDENCE_BLOCK_BREAK_LINE,
              Map.of(
                  "time", TimeText.absolute(sample.createdAt(), messages),
                  "world", sample.world(),
                  "x", String.valueOf(sample.x()),
                  "y", String.valueOf(sample.y()),
                  "z", String.valueOf(sample.z()),
                  "material", sample.material())));
    }

    sender.sendMessage(
        messages.component(
            MessageKeys.EVIDENCE_MOVEMENT,
            Map.of("count", String.valueOf(evidence.movement().size()))));

    for (var sample : evidence.movement()) {

      sender.sendMessage(
          messages.component(
              MessageKeys.EVIDENCE_MOVEMENT_LINE,
              Map.of(
                  "time", TimeText.absolute(sample.createdAt(), messages),
                  "world", sample.world(),
                  "x", format(sample.x()),
                  "y", format(sample.y()),
                  "z", format(sample.z()),
                  "ground",
                      messages.plain(
                          sample.onGround() ? MessageKeys.GENERAL_YES : MessageKeys.GENERAL_NO))));
    }
  }

  public String filterLabel(ReportFilter filter) {

    ReportFilter effective = filter == null ? ReportFilter.all() : filter;

    Messages messages = plugin.messages();

    String status =
        effective.statuses().isEmpty()
            ? messages.plain(MessageKeys.STATUS_ALL)
            : messages.plain(NotificationService.statusKey(effective.statuses().iterator().next()));

    java.util.List<String> parts = new java.util.ArrayList<>();

    parts.add(status);

    if (!effective.category().isEmpty()) {

      parts.add(plugin.categoryDisplay(effective.category()));
    }

    if (effective.claimedBy() != null) {

      parts.add(messages.plain(MessageKeys.GUI_FILTER_MINE_LABEL));
    }

    if (!effective.query().isEmpty()) {

      parts.add(
          messages.plain(MessageKeys.GUI_FILTER_QUERY_LABEL, Map.of("query", effective.query())));
    }

    if (effective.sort() == ReportFilter.SortOrder.OLDEST) {

      parts.add(messages.plain(MessageKeys.GUI_FILTER_SORT_OLDEST));

    } else if (effective.sort() == ReportFilter.SortOrder.PRIORITY) {

      parts.add(messages.plain(MessageKeys.GUI_FILTER_SORT_PRIORITY));
    }

    if (!effective.priorities().isEmpty()) {

      parts.add(
          messages.plain(
              NotificationService.priorityKey(effective.priorities().iterator().next())));
    }

    if (effective.overdueOnly()) {

      parts.add(messages.plain(MessageKeys.GUI_FILTER_OVERDUE_LABEL));
    }

    return String.join(" · ", parts);
  }

  public List<Component> evidenceSummary(Evidence evidence) {

    Messages messages = plugin.messages();

    java.util.ArrayList<Component> lines = new java.util.ArrayList<>();

    if (evidence == null) {

      lines.add(messages.component(MessageKeys.EVIDENCE_NOT_CAPTURED, Map.of()));

      return lines;
    }

    lines.add(
        messages.component(
            MessageKeys.EVIDENCE_TARGET,
            Map.of(
                "target", evidence.targetName(),
                "online",
                    messages.plain(
                        evidence.targetOnline()
                            ? MessageKeys.GENERAL_YES
                            : MessageKeys.GENERAL_NO))));

    lines.add(
        messages.component(
            MessageKeys.EVIDENCE_CAPTURED_AT,
            Map.of("time", TimeText.absolute(evidence.capturedAt(), messages))));

    if (!evidence.world().isBlank()) {

      lines.add(
          messages.component(
              MessageKeys.EVIDENCE_LOCATION,
              Map.of(
                  "world", evidence.world(),
                  "x", format(evidence.x()),
                  "y", format(evidence.y()),
                  "z", format(evidence.z()))));
    }

    return lines;
  }

  private void infoLine(CommandSender sender, String labelKey, String value) {

    Messages messages = plugin.messages();

    Map<String, String> placeholders = new LinkedHashMap<>();

    placeholders.put("key", messages.plain(labelKey));

    placeholders.put("value", value == null ? "" : value);

    sender.sendMessage(messages.component(MessageKeys.ADMIN_INFO_LINE, placeholders));
  }

  private void ownInfoLine(CommandSender sender, String labelKey, String value) {

    Messages messages = plugin.messages();

    Map<String, String> placeholders = new LinkedHashMap<>();

    placeholders.put("key", messages.plain(labelKey));

    placeholders.put("value", value == null ? "" : value);

    sender.sendMessage(messages.component(MessageKeys.REPORT_OWN_INFO_LINE, placeholders));
  }

  private Map<String, String> linePlaceholders(Report report) {

    Messages messages = plugin.messages();

    Map<String, String> placeholders = new LinkedHashMap<>();

    placeholders.put("id", String.valueOf(report.id()));

    placeholders.put("status", messages.plain(NotificationService.statusKey(report.status())));

    placeholders.put("target", report.targetName());

    placeholders.put("reporter", report.reporterName());

    placeholders.put("category", plugin.categoryDisplay(report.category()));

    placeholders.put("created", TimeText.absolute(report.createdAt(), messages));

    placeholders.put(
        "priority", messages.plain(NotificationService.priorityKey(report.priority())));

    placeholders.put("sla", slaText(report));

    placeholders.put(
        "overdue",
        messages.plain(
            report.isOverdue(System.currentTimeMillis())
                ? MessageKeys.GENERAL_YES
                : MessageKeys.GENERAL_NO));

    placeholders.put("replies", String.valueOf(report.replies().size()));

    return placeholders;
  }

  private String slaText(Report report) {

    Messages messages = plugin.messages();

    if (report.slaDueAt() <= 0L) {

      return messages.plain(MessageKeys.GENERAL_SLA_NONE);
    }

    String due = TimeText.absolute(report.slaDueAt(), messages);

    if (!report.isTerminal() && report.isOverdue(System.currentTimeMillis())) {

      return messages.plain(MessageKeys.GENERAL_SLA_OVERDUE, Map.of("time", due));
    }

    return due;
  }

  private String replySummary(Report report) {

    return plugin
        .messages()
        .plain(
            MessageKeys.GENERAL_REPLIES_VALUE,
            Map.of(
                "total", String.valueOf(report.replies().size()),
                "unread", String.valueOf(report.unreadReplies())));
  }

  private static String format(double value) {

    return String.format(java.util.Locale.ROOT, "%.1f", value);
  }
}
