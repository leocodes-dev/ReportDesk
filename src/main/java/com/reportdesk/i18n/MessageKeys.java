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

package com.reportdesk.i18n;

/**
 * Central message keys. Keeping them in one class makes missing translations a compile-visible
 * concern and lets a unit test verify both language files.
 */
public final class MessageKeys {

  private MessageKeys() {}

  public static final String PREFIX = "prefix";

  public static final String GENERAL_NO_PERMISSION = "general.no-permission";

  public static final String GENERAL_PLAYER_ONLY = "general.player-only";

  public static final String GENERAL_UNKNOWN_COMMAND = "general.unknown-command";

  public static final String GENERAL_USAGE = "general.usage";

  public static final String GENERAL_LOADING = "general.loading";

  public static final String GENERAL_RELOAD_SUCCESS = "general.reload-success";

  public static final String GENERAL_RELOAD_FAILED = "general.reload-failed";

  public static final String GENERAL_INVALID_INPUT = "general.invalid-input";

  public static final String GENERAL_PLAYER_NOT_FOUND = "general.player-not-found";

  public static final String GENERAL_OFFLINE_TARGET_DISABLED = "general.offline-target-disabled";

  public static final String GENERAL_SELF_REPORT = "general.self-report";

  public static final String GENERAL_CANCELLED = "general.cancelled";

  public static final String GENERAL_PAGE = "general.page";

  public static final String GENERAL_NONE = "general.none";

  public static final String GENERAL_YES = "general.yes";

  public static final String GENERAL_NO = "general.no";

  public static final String GENERAL_BACK = "general.back";

  public static final String GENERAL_CLOSE = "general.close";

  public static final String GENERAL_REFRESH = "general.refresh";

  public static final String GENERAL_PREVIOUS = "general.previous";

  public static final String GENERAL_PREVIOUS_LORE = "general.previous-lore";

  public static final String GENERAL_NEXT = "general.next";

  public static final String GENERAL_NEXT_LORE = "general.next-lore";

  public static final String GENERAL_SEARCH = "general.search";

  public static final String GENERAL_FILTER = "general.filter";

  public static final String GENERAL_STATS = "general.stats";

  public static final String GENERAL_CONFIRM = "general.confirm";

  public static final String GENERAL_CANCEL = "general.cancel";

  public static final String GENERAL_ENABLED = "general.enabled";

  public static final String GENERAL_DISABLED = "general.disabled";

  public static final String GENERAL_UNKNOWN_ERROR = "general.unknown-error";

  public static final String GENERAL_ACTION_COOLDOWN = "general.action-cooldown";

  public static final String GENERAL_INPUT_TIMEOUT = "general.input-timeout";

  public static final String GENERAL_TARGET_OFFLINE = "general.target-offline";

  public static final String GENERAL_REPORT_NOT_FOUND = "general.report-not-found";

  public static final String GENERAL_INVALID_ID = "general.invalid-id";

  public static final String GENERAL_INVALID_VERDICT = "general.invalid-verdict";

  public static final String GENERAL_CATEGORY = "general.category";

  public static final String GENERAL_REPORTER = "general.reporter";

  public static final String GENERAL_STAFF = "general.staff";

  public static final String GENERAL_TARGET = "general.target";

  public static final String GENERAL_STATUS = "general.status";

  public static final String GENERAL_VERDICT = "general.verdict";

  public static final String GENERAL_CREATED = "general.created";

  public static final String GENERAL_CLAIMED_BY = "general.claimed-by";

  public static final String GENERAL_CLAIMED_AT = "general.claimed-at";

  public static final String GENERAL_CLOSED_BY = "general.closed-by";

  public static final String GENERAL_CLOSED_AT = "general.closed-at";

  public static final String GENERAL_DETAIL = "general.detail";

  public static final String GENERAL_NOTE = "general.note";

  public static final String GENERAL_NOTES = "general.notes";

  public static final String GENERAL_EVIDENCE = "general.evidence";

  public static final String GENERAL_SYSTEM = "general.system";

  public static final String GENERAL_CLOSE_NOTE = "general.close-note";

  public static final String GENERAL_TARGET_HISTORY = "general.target-history";

  public static final String GENERAL_TARGET_HISTORY_VALUE = "general.target-history-value";

  public static final String GENERAL_NO_OPEN_REPORTS = "general.no-open-reports";

  public static final String GENERAL_PRIORITY = "general.priority";

  public static final String GENERAL_SLA = "general.sla";

  public static final String GENERAL_SLA_NONE = "general.sla-none";

  public static final String GENERAL_SLA_OVERDUE = "general.sla-overdue";

  public static final String GENERAL_REPLIES = "general.replies";

  public static final String GENERAL_REPLIES_VALUE = "general.replies-value";

  public static final String GENERAL_UNREAD = "general.unread";

  public static final String GENERAL_LAST_REPLY = "general.last-reply";

  public static final String GENERAL_OVERDUE_LABEL = "general.overdue-label";

  public static final String GENERAL_SMART_SCORE = "general.smart-score";

  public static final String PRIORITY_LOW = "priority.low";

  public static final String PRIORITY_NORMAL = "priority.normal";

  public static final String PRIORITY_HIGH = "priority.high";

  public static final String PRIORITY_URGENT = "priority.urgent";

  public static final String STATUS_OPEN = "status.open";

  public static final String STATUS_CLAIMED = "status.claimed";

  public static final String STATUS_RESOLVED = "status.resolved";

  public static final String STATUS_REJECTED = "status.rejected";

  public static final String STATUS_DUPLICATE = "status.duplicate";

  public static final String STATUS_AUTO_CLOSED = "status.auto-closed";

  public static final String STATUS_ALL = "status.all";

  public static final String VERDICT_RESOLVED = "verdict.resolved";

  public static final String VERDICT_REJECTED = "verdict.rejected";

  public static final String VERDICT_DUPLICATE = "verdict.duplicate";

  public static final String VERDICT_INSUFFICIENT = "verdict.insufficient";

  public static final String VERDICT_AUTO_CLOSED = "verdict.auto-closed";

  public static final String REPORT_SUCCESS = "report.success";

  public static final String REPORT_SUCCESS_STAFF = "report.success-staff";

  public static final String REPORT_COOLDOWN = "report.cooldown";

  public static final String REPORT_DAILY_LIMIT = "report.daily-limit";

  public static final String REPORT_SAME_TARGET = "report.same-target";

  public static final String REPORT_DUPLICATE = "report.duplicate";

  public static final String REPORT_MAX_OPEN = "report.max-open";

  public static final String REPORT_ACCOUNT_TOO_YOUNG = "report.account-too-young";

  public static final String REPORT_DETAIL_SHORT = "report.detail-short";

  public static final String REPORT_DETAIL_LONG = "report.detail-long";

  public static final String REPORT_DETAIL_BLOCKED = "report.detail-blocked";

  public static final String REPORT_DETAIL_REQUIRED = "report.detail-required";

  public static final String REPORT_CATEGORY_UNKNOWN = "report.category-unknown";

  public static final String REPORT_CATEGORY_DISABLED = "report.category-disabled";

  public static final String REPORT_TARGET_REQUIRED = "report.target-required";

  public static final String REPORT_GUI_DISABLED = "report.gui-disabled";

  public static final String REPORT_HELP_HEADER = "report.help-header";

  public static final String REPORT_HELP_LINE_REPORT = "report.help-line-report";

  public static final String REPORT_HELP_LINE_TARGET = "report.help-line-target";

  public static final String REPORT_HELP_LINE_DIRECT = "report.help-line-direct";

  public static final String REPORT_HELP_LINE_STATUS = "report.help-line-status";

  public static final String REPORT_HELP_LINE_VIEW = "report.help-line-view";

  public static final String REPORT_HELP_LINE_CANCEL = "report.help-line-cancel";

  public static final String REPORT_HELP_LINE_REPLY = "report.help-line-reply";

  public static final String REPORT_HELP_LINE_REPLIES = "report.help-line-replies";

  public static final String REPORT_REPLY_DISABLED = "report.reply-disabled";

  public static final String REPORT_REPLY_NOT_ALLOWED = "report.reply-not-allowed";

  public static final String REPORT_REPLY_SENT = "report.reply-sent";

  public static final String REPORT_REPLY_FULL = "report.reply-full";

  public static final String REPORT_REPLIES_HEADER = "report.replies-header";

  public static final String REPORT_REPLIES_EMPTY = "report.replies-empty";

  public static final String REPORT_REPLY_LINE = "report.reply-line";

  public static final String REPORT_OWN_HEADER = "report.own-header";

  public static final String REPORT_OWN_LINE = "report.own-line";

  public static final String REPORT_OWN_EMPTY = "report.own-empty";

  public static final String REPORT_OWN_INFO_HEADER = "report.own-info-header";

  public static final String REPORT_OWN_INFO_LINE = "report.own-info-line";

  public static final String REPORT_HELP_LINE_HISTORY = "report.help-line-history";

  public static final String REPORT_HISTORY_HEADER = "report.history-header";

  public static final String REPORT_HISTORY_STATS = "report.history-stats";

  public static final String REPORT_HISTORY_HINT = "report.history-hint";

  public static final String REPORT_HISTORY_UNKNOWN_FILTER = "report.history-unknown-filter";

  public static final String REPORT_MENU_TITLE_PLAYERS = "report.menu-title-players";

  public static final String REPORT_MENU_TITLE_CATEGORIES = "report.menu-title-categories";

  public static final String REPORT_MENU_TITLE_CONFIRM = "report.menu-title-confirm";

  public static final String REPORT_PLAYER_ITEM_NAME = "report.player-item-name";

  public static final String REPORT_PLAYER_ITEM_LORE = "report.player-item-lore";

  public static final String REPORT_CONFIRM_TARGET = "report.confirm-target";

  public static final String REPORT_CONFIRM_CATEGORY = "report.confirm-category";

  public static final String REPORT_CONFIRM_DETAIL_EMPTY = "report.confirm-detail-empty";

  public static final String REPORT_CONFIRM_DETAIL = "report.confirm-detail";

  public static final String REPORT_CONFIRM_SUBMIT = "report.confirm-submit";

  public static final String REPORT_CONFIRM_EDIT = "report.confirm-edit";

  public static final String REPORT_CONFIRM_CANCEL = "report.confirm-cancel";

  public static final String REPORT_SEARCH_PROMPT = "report.search-prompt";

  public static final String REPORT_DETAIL_PROMPT = "report.detail-prompt";

  public static final String REPORT_DETAIL_SAVED = "report.detail-saved";

  public static final String REPORT_SUBMITTED_BROADCAST = "report.submitted-broadcast";

  public static final String REPORT_CHAT_TITLE = "report.chat-title";

  public static final String REPORT_CHAT_ITEM_NAME = "report.chat-item-name";

  public static final String REPORT_CHAT_ITEM_LORE = "report.chat-item-lore";

  public static final String REPORT_CHAT_STATE_SELECTED = "report.chat-state-selected";

  public static final String REPORT_CHAT_STATE_UNSELECTED = "report.chat-state-unselected";

  public static final String REPORT_CHAT_EMPTY = "report.chat-empty";

  public static final String REPORT_CHAT_SELECT_NONE = "report.chat-select-none";

  public static final String REPORT_CHAT_LIMIT = "report.chat-limit";

  public static final String REPORT_CHAT_CONTINUE = "report.chat-continue";

  public static final String REPORT_CHAT_CONTINUE_LORE = "report.chat-continue-lore";

  public static final String REPORT_CHAT_CLEAR_NAME = "report.chat-clear-name";

  public static final String REPORT_CHAT_CLEAR_LORE = "report.chat-clear-lore";

  public static final String REPORT_CONFIRM_CHAT_NAME = "report.confirm-chat-name";

  public static final String REPORT_CONFIRM_CHAT_LORE = "report.confirm-chat-lore";

  public static final String REPORT_CONFIRM_CHAT_LINE = "report.confirm-chat-line";

  public static final String REPORT_CONFIRM_CHAT_EMPTY = "report.confirm-chat-empty";

  public static final String REPORT_CONFIRM_CHAT_EDIT_NAME = "report.confirm-chat-edit-name";

  public static final String REPORT_CONFIRM_CHAT_EDIT_LORE = "report.confirm-chat-edit-lore";

  public static final String GUI_LIST_TITLE = "gui.list-title";

  public static final String GUI_LIST_ITEM_NAME = "gui.list-item-name";

  public static final String GUI_LIST_ITEM_LORE = "gui.list-item-lore";

  public static final String GUI_FILTER_NAME = "gui.filter-name";

  public static final String GUI_FILTER_LORE = "gui.filter-lore";

  public static final String GUI_FILTER_MENU_TITLE = "gui.filter-menu-title";

  public static final String GUI_FILTER_STATUS_LORE = "gui.filter-status-lore";

  public static final String GUI_FILTER_CATEGORY_NAME = "gui.filter-category-name";

  public static final String GUI_FILTER_CATEGORY_LORE = "gui.filter-category-lore";

  public static final String GUI_FILTER_MINE_NAME = "gui.filter-mine-name";

  public static final String GUI_FILTER_MINE_LORE = "gui.filter-mine-lore";

  public static final String GUI_FILTER_SORT_NAME = "gui.filter-sort-name";

  public static final String GUI_FILTER_SORT_LORE = "gui.filter-sort-lore";

  public static final String GUI_FILTER_RESET_NAME = "gui.filter-reset-name";

  public static final String GUI_FILTER_RESET_LORE = "gui.filter-reset-lore";

  public static final String GUI_FILTER_SELECTED = "gui.filter-selected";

  public static final String GUI_FILTER_MINE_LABEL = "gui.filter-mine-label";

  public static final String GUI_FILTER_QUERY_LABEL = "gui.filter-query-label";

  public static final String GUI_FILTER_ALL_CATEGORIES = "gui.filter-all-categories";

  public static final String GUI_FILTER_SORT_NEWEST = "gui.filter-sort-newest";

  public static final String GUI_FILTER_SORT_OLDEST = "gui.filter-sort-oldest";

  public static final String GUI_FILTER_SORT_PRIORITY = "gui.filter-sort-priority";

  public static final String GUI_FILTER_PRIORITY_NAME = "gui.filter-priority-name";

  public static final String GUI_FILTER_PRIORITY_LORE = "gui.filter-priority-lore";

  public static final String GUI_FILTER_ALL_PRIORITIES = "gui.filter-all-priorities";

  public static final String GUI_FILTER_OVERDUE_NAME = "gui.filter-overdue-name";

  public static final String GUI_FILTER_OVERDUE_LORE = "gui.filter-overdue-lore";

  public static final String GUI_FILTER_OVERDUE_LABEL = "gui.filter-overdue-label";

  public static final String GUI_DETAIL_REPLIES_NAME = "gui.detail-replies-name";

  public static final String GUI_DETAIL_REPLIES_LORE = "gui.detail-replies-lore";

  public static final String GUI_REPLIES_TITLE = "gui.replies-title";

  public static final String GUI_REPLIES_ITEM_NAME = "gui.replies-item-name";

  public static final String GUI_REPLIES_ITEM_LORE = "gui.replies-item-lore";

  public static final String GUI_REPLIES_EMPTY = "gui.replies-empty";

  public static final String GUI_REPLIES_ADD_NAME = "gui.replies-add-name";

  public static final String GUI_REPLIES_ADD_LORE = "gui.replies-add-lore";

  public static final String GUI_REPLIES_ADD_PROMPT = "gui.replies-add-prompt";

  public static final String GUI_STATS_NAME = "gui.stats-name";

  public static final String GUI_STATS_LORE = "gui.stats-lore";

  public static final String GUI_SEARCH_NAME = "gui.search-name";

  public static final String GUI_SEARCH_LORE = "gui.search-lore";

  public static final String GUI_PAGE_NAME = "gui.page-name";

  public static final String GUI_EMPTY = "gui.empty";

  public static final String GUI_DETAIL_TITLE = "gui.detail-title";

  public static final String GUI_DETAIL_INFO_NAME = "gui.detail-info-name";

  public static final String GUI_DETAIL_INFO_LORE = "gui.detail-info-lore";

  public static final String GUI_DETAIL_DETAIL_NAME = "gui.detail-detail-name";

  public static final String GUI_DETAIL_DETAIL_LORE = "gui.detail-detail-lore";

  public static final String GUI_DETAIL_CLAIM_NAME = "gui.detail-claim-name";

  public static final String GUI_DETAIL_CLAIM_LORE = "gui.detail-claim-lore";

  public static final String GUI_DETAIL_RELEASE_NAME = "gui.detail-release-name";

  public static final String GUI_DETAIL_RELEASE_LORE = "gui.detail-release-lore";

  public static final String GUI_DETAIL_TELEPORT_NAME = "gui.detail-teleport-name";

  public static final String GUI_DETAIL_TELEPORT_LORE = "gui.detail-teleport-lore";

  public static final String GUI_DETAIL_CLOSE_NAME = "gui.detail-close-name";

  public static final String GUI_DETAIL_CLOSE_LORE = "gui.detail-close-lore";

  public static final String GUI_DETAIL_EVIDENCE_NAME = "gui.detail-evidence-name";

  public static final String GUI_DETAIL_EVIDENCE_LORE = "gui.detail-evidence-lore";

  public static final String GUI_DETAIL_NOTES_NAME = "gui.detail-notes-name";

  public static final String GUI_DETAIL_NOTES_LORE = "gui.detail-notes-lore";

  public static final String GUI_DETAIL_REOPEN_NAME = "gui.detail-reopen-name";

  public static final String GUI_DETAIL_REOPEN_LORE = "gui.detail-reopen-lore";

  public static final String GUI_DETAIL_DELETE_NAME = "gui.detail-delete-name";

  public static final String GUI_DETAIL_DELETE_LORE = "gui.detail-delete-lore";

  public static final String GUI_DETAIL_CLOSE_NOTE_NAME = "gui.detail-close-note-name";

  public static final String GUI_DETAIL_CLOSE_NOTE_LORE = "gui.detail-close-note-lore";

  public static final String GUI_DETAIL_NEXT_NAME = "gui.detail-next-name";

  public static final String GUI_DETAIL_NEXT_LORE = "gui.detail-next-lore";

  public static final String GUI_HISTORY_TITLE = "gui.history-title";

  public static final String GUI_HISTORY_ENTRY_NAME = "gui.history-entry-name";

  public static final String GUI_HISTORY_ENTRY_LORE = "gui.history-entry-lore";

  public static final String GUI_HISTORY_HEADER_NAME = "gui.history-header-name";

  public static final String GUI_HISTORY_HEADER_LORE = "gui.history-header-lore";

  public static final String GUI_HISTORY_ITEM_NAME = "gui.history-item-name";

  public static final String GUI_HISTORY_ITEM_LORE = "gui.history-item-lore";

  public static final String GUI_HISTORY_FILTER_TITLE = "gui.history-filter-title";

  public static final String GUI_HISTORY_DETAIL_TITLE = "gui.history-detail-title";

  public static final String GUI_HISTORY_INFO_NAME = "gui.history-info-name";

  public static final String GUI_HISTORY_INFO_LORE = "gui.history-info-lore";

  public static final String GUI_HISTORY_RESULT_NAME = "gui.history-result-name";

  public static final String GUI_HISTORY_RESULT_VERDICT = "gui.history-result-verdict";

  public static final String GUI_HISTORY_RESULT_CLAIMED = "gui.history-result-claimed";

  public static final String GUI_HISTORY_RESULT_PENDING = "gui.history-result-pending";

  public static final String GUI_HISTORY_RESULT_CLOSED_BY = "gui.history-result-closed-by";

  public static final String GUI_HISTORY_RESULT_NOTE = "gui.history-result-note";

  public static final String GUI_HISTORY_RESULT_TARGET = "gui.history-result-target";

  public static final String GUI_HISTORY_EVIDENCE_NAME = "gui.history-evidence-name";

  public static final String GUI_HISTORY_EVIDENCE_LORE = "gui.history-evidence-lore";

  public static final String GUI_HISTORY_REPLIES_NAME = "gui.history-replies-name";

  public static final String GUI_HISTORY_REPLIES_LORE = "gui.history-replies-lore";

  public static final String GUI_HISTORY_REPLY_NAME = "gui.history-reply-name";

  public static final String GUI_HISTORY_REPLY_LORE = "gui.history-reply-lore";

  public static final String GUI_HISTORY_REPLY_DISABLED = "gui.history-reply-disabled";

  public static final String GUI_HISTORY_REPLY_PROMPT = "gui.history-reply-prompt";

  public static final String GUI_HISTORY_REPLIES_TITLE = "gui.history-replies-title";

  public static final String GUI_DELETE_TITLE = "gui.delete-title";

  public static final String GUI_DELETE_YES_NAME = "gui.delete-yes-name";

  public static final String GUI_DELETE_YES_LORE = "gui.delete-yes-lore";

  public static final String GUI_DELETE_NO_NAME = "gui.delete-no-name";

  public static final String GUI_DELETE_NO_LORE = "gui.delete-no-lore";

  public static final String GUI_VERDICT_TITLE = "gui.verdict-title";

  public static final String GUI_VERDICT_RESOLVED_NAME = "gui.verdict-resolved-name";

  public static final String GUI_VERDICT_RESOLVED_LORE = "gui.verdict-resolved-lore";

  public static final String GUI_VERDICT_REJECTED_NAME = "gui.verdict-rejected-name";

  public static final String GUI_VERDICT_REJECTED_LORE = "gui.verdict-rejected-lore";

  public static final String GUI_VERDICT_DUPLICATE_NAME = "gui.verdict-duplicate-name";

  public static final String GUI_VERDICT_DUPLICATE_LORE = "gui.verdict-duplicate-lore";

  public static final String GUI_VERDICT_INSUFFICIENT_NAME = "gui.verdict-insufficient-name";

  public static final String GUI_VERDICT_INSUFFICIENT_LORE = "gui.verdict-insufficient-lore";

  public static final String GUI_NOTES_TITLE = "gui.notes-title";

  public static final String GUI_NOTES_ITEM_NAME = "gui.notes-item-name";

  public static final String GUI_NOTES_ITEM_LORE = "gui.notes-item-lore";

  public static final String GUI_NOTES_EMPTY = "gui.notes-empty";

  public static final String GUI_NOTES_ADD_NAME = "gui.notes-add-name";

  public static final String GUI_NOTES_ADD_LORE = "gui.notes-add-lore";

  public static final String GUI_NOTES_ADD_PROMPT = "gui.notes-add-prompt";

  public static final String GUI_SEARCH_PROMPT = "gui.search-prompt";

  public static final String GUI_LOADING = "gui.loading";

  public static final String GUI_EVIDENCE_LOADING = "gui.evidence-loading";

  public static final String ADMIN_GUI_HUB_TITLE = "gui.admin.hub-title";

  public static final String ADMIN_GUI_HUB_INFO_NAME = "gui.admin.hub-info-name";

  public static final String ADMIN_GUI_HUB_INFO_LORE = "gui.admin.hub-info-lore";

  public static final String ADMIN_GUI_HUB_REPORTS_NAME = "gui.admin.hub-reports-name";

  public static final String ADMIN_GUI_HUB_REPORTS_LORE = "gui.admin.hub-reports-lore";

  public static final String ADMIN_GUI_HUB_NEXT_NAME = "gui.admin.hub-next-name";

  public static final String ADMIN_GUI_HUB_NEXT_LORE = "gui.admin.hub-next-lore";

  public static final String ADMIN_GUI_HUB_PLAYERS_NAME = "gui.admin.hub-players-name";

  public static final String ADMIN_GUI_HUB_PLAYERS_LORE = "gui.admin.hub-players-lore";

  public static final String ADMIN_GUI_HUB_STATS_NAME = "gui.admin.hub-stats-name";

  public static final String ADMIN_GUI_HUB_STATS_LORE = "gui.admin.hub-stats-lore";

  public static final String ADMIN_GUI_HUB_WATCH_NAME = "gui.admin.hub-watch-name";

  public static final String ADMIN_GUI_HUB_WATCH_LORE = "gui.admin.hub-watch-lore";

  public static final String ADMIN_GUI_HUB_CONFIG_NAME = "gui.admin.hub-config-name";

  public static final String ADMIN_GUI_HUB_CONFIG_LORE = "gui.admin.hub-config-lore";

  public static final String ADMIN_GUI_HUB_AUDIT_NAME = "gui.admin.hub-audit-name";

  public static final String ADMIN_GUI_HUB_AUDIT_LORE = "gui.admin.hub-audit-lore";

  public static final String ADMIN_GUI_HUB_MISC_NAME = "gui.admin.hub-misc-name";

  public static final String ADMIN_GUI_HUB_MISC_LORE = "gui.admin.hub-misc-lore";

  public static final String ADMIN_GUI_REFRESH_NAME = "gui.admin.refresh-name";

  public static final String ADMIN_GUI_REFRESH_LORE = "gui.admin.refresh-lore";

  public static final String ADMIN_GUI_BACK = "gui.admin.back";

  public static final String ADMIN_GUI_HUB_BACK = "gui.admin.hub-back";

  public static final String ADMIN_GUI_DISABLED = "gui.admin.disabled";

  public static final String ADMIN_GUI_ONLINE = "gui.admin.online";

  public static final String ADMIN_GUI_OFFLINE = "gui.admin.offline";

  public static final String ADMIN_GUI_NO_OPEN = "gui.admin.no-open";

  public static final String ADMIN_GUI_PLAYERS_TITLE = "gui.admin.players-title";

  public static final String ADMIN_GUI_PLAYERS_SEARCH_NAME = "gui.admin.players-search-name";

  public static final String ADMIN_GUI_PLAYERS_SEARCH_LORE = "gui.admin.players-search-lore";

  public static final String ADMIN_GUI_PLAYERS_SEARCH_PROMPT = "gui.admin.players-search-prompt";

  public static final String ADMIN_GUI_PLAYERS_EMPTY = "gui.admin.players-empty";

  public static final String ADMIN_GUI_PLAYER_ITEM_NAME = "gui.admin.player-item-name";

  public static final String ADMIN_GUI_PLAYER_ITEM_LORE = "gui.admin.player-item-lore";

  public static final String ADMIN_GUI_PLAYER_TITLE = "gui.admin.player-title";

  public static final String ADMIN_GUI_PLAYER_INFO_NAME = "gui.admin.player-info-name";

  public static final String ADMIN_GUI_PLAYER_INFO_LORE = "gui.admin.player-info-lore";

  public static final String ADMIN_GUI_PLAYER_OPEN_NAME = "gui.admin.player-open-name";

  public static final String ADMIN_GUI_PLAYER_OPEN_LORE = "gui.admin.player-open-lore";

  public static final String ADMIN_GUI_PLAYER_ALL_NAME = "gui.admin.player-all-name";

  public static final String ADMIN_GUI_PLAYER_ALL_LORE = "gui.admin.player-all-lore";

  public static final String ADMIN_GUI_PLAYER_VERDICT_NAME = "gui.admin.player-verdict-name";

  public static final String ADMIN_GUI_PLAYER_VERDICT_LORE = "gui.admin.player-verdict-lore";

  public static final String ADMIN_GUI_PLAYER_REPORTER_NAME = "gui.admin.player-reporter-name";

  public static final String ADMIN_GUI_PLAYER_REPORTER_LORE = "gui.admin.player-reporter-lore";

  public static final String ADMIN_GUI_PLAYER_TP_NAME = "gui.admin.player-tp-name";

  public static final String ADMIN_GUI_PLAYER_TP_LORE = "gui.admin.player-tp-lore";

  public static final String ADMIN_GUI_PLAYER_WATCH_NAME = "gui.admin.player-watch-name";

  public static final String ADMIN_GUI_PLAYER_WATCH_LORE = "gui.admin.player-watch-lore";

  public static final String ADMIN_GUI_PLAYER_UNWATCH_NAME = "gui.admin.player-unwatch-name";

  public static final String ADMIN_GUI_PLAYER_UNWATCH_LORE = "gui.admin.player-unwatch-lore";

  public static final String ADMIN_GUI_PLAYER_REPORTS_TITLE = "gui.admin.player-reports-title";

  public static final String ADMIN_GUI_SCOPE_ALL = "gui.admin.scope-all";

  public static final String ADMIN_GUI_SCOPE_OPEN = "gui.admin.scope-open";

  public static final String ADMIN_GUI_SCOPE_TARGET = "gui.admin.scope-target";

  public static final String ADMIN_GUI_SCOPE_REPORTER = "gui.admin.scope-reporter";

  public static final String ADMIN_GUI_STATS_TITLE = "gui.admin.stats-title";

  public static final String ADMIN_GUI_STATS_OVERVIEW_NAME = "gui.admin.stats-overview-name";

  public static final String ADMIN_GUI_STATS_OVERVIEW_LORE = "gui.admin.stats-overview-lore";

  public static final String ADMIN_GUI_STATS_STATUS_NAME = "gui.admin.stats-status-name";

  public static final String ADMIN_GUI_STATS_CATEGORIES_NAME = "gui.admin.stats-categories-name";

  public static final String ADMIN_GUI_STATS_TARGETS_NAME = "gui.admin.stats-targets-name";

  public static final String ADMIN_GUI_STATS_STAFF_NAME = "gui.admin.stats-staff-name";

  public static final String ADMIN_GUI_STATS_OVERDUE_NAME = "gui.admin.stats-overdue-name";

  public static final String ADMIN_GUI_STATS_OVERDUE_LORE = "gui.admin.stats-overdue-lore";

  public static final String ADMIN_GUI_STATS_AVG_NAME = "gui.admin.stats-avg-name";

  public static final String ADMIN_GUI_STATS_AVG_LORE = "gui.admin.stats-avg-lore";

  public static final String ADMIN_GUI_STATS_LINE = "gui.admin.stats-line";

  public static final String ADMIN_GUI_STATS_NONE = "gui.admin.stats-none";

  public static final String ADMIN_GUI_WATCHLIST_TITLE = "gui.admin.watchlist-title";

  public static final String ADMIN_GUI_WATCHLIST_ADD_NAME = "gui.admin.watchlist-add-name";

  public static final String ADMIN_GUI_WATCHLIST_ADD_LORE = "gui.admin.watchlist-add-lore";

  public static final String ADMIN_GUI_WATCHLIST_ADD_PROMPT = "gui.admin.watchlist-add-prompt";

  public static final String ADMIN_GUI_WATCHLIST_ITEM_NAME = "gui.admin.watchlist-item-name";

  public static final String ADMIN_GUI_WATCHLIST_ITEM_LORE = "gui.admin.watchlist-item-lore";

  public static final String ADMIN_GUI_WATCHLIST_EMPTY = "gui.admin.watchlist-empty";

  public static final String ADMIN_GUI_WATCHLIST_DISABLED = "gui.admin.watchlist-disabled";

  public static final String ADMIN_GUI_WATCHLIST_ALREADY = "gui.admin.watchlist-already";

  public static final String ADMIN_GUI_CONFIG_TITLE = "gui.admin.config-title";

  public static final String ADMIN_GUI_CONFIG_SECTION_NAME = "gui.admin.config-section-name";

  public static final String ADMIN_GUI_CONFIG_SECTION_LORE = "gui.admin.config-section-lore";

  public static final String ADMIN_GUI_CONFIG_KEYS_TITLE = "gui.admin.config-keys-title";

  public static final String ADMIN_GUI_CONFIG_KEY_NAME = "gui.admin.config-key-name";

  public static final String ADMIN_GUI_CONFIG_KEY_LORE = "gui.admin.config-key-lore";

  public static final String ADMIN_GUI_CONFIG_KEY_PROMPT = "gui.admin.config-key-prompt";

  public static final String ADMIN_GUI_CONFIG_TYPE = "gui.admin.config-type";

  public static final String ADMIN_GUI_CONFIG_SEARCH_NAME = "gui.admin.config-search-name";

  public static final String ADMIN_GUI_CONFIG_SEARCH_LORE = "gui.admin.config-search-lore";

  public static final String ADMIN_GUI_CONFIG_SEARCH_PROMPT = "gui.admin.config-search-prompt";

  public static final String ADMIN_GUI_CONFIG_SEARCH_TITLE = "gui.admin.config-search-title";

  public static final String ADMIN_GUI_CONFIG_SEARCH_EMPTY = "gui.admin.config-search-empty";

  public static final String ADMIN_GUI_CONFIG_SEARCH_SCOPE = "gui.admin.config-search-scope";

  public static final String ADMIN_GUI_CONFIG_LIST_HEADER_NAME =
      "gui.admin.config-list-header-name";

  public static final String ADMIN_GUI_CONFIG_LIST_HEADER_LORE =
      "gui.admin.config-list-header-lore";

  public static final String ADMIN_GUI_CONFIG_SECTIONS_HEADER_NAME =
      "gui.admin.config-sections-header-name";

  public static final String ADMIN_GUI_CONFIG_SECTIONS_HEADER_LORE =
      "gui.admin.config-sections-header-lore";

  public static final String ADMIN_GUI_CONFIG_READY = "gui.admin.config-ready";

  public static final String ADMIN_GUI_CONFIG_NOT_READY = "gui.admin.config-not-ready";

  public static final String ADMIN_GUI_CONFIG_RELOAD_NAME = "gui.admin.config-reload-name";

  public static final String ADMIN_GUI_CONFIG_RELOAD_LORE = "gui.admin.config-reload-lore";

  public static final String ADMIN_GUI_CONFIG_KEY_STATE_DEFAULT =
      "gui.admin.config-key-state-default";

  public static final String ADMIN_GUI_CONFIG_KEY_STATE_MODIFIED =
      "gui.admin.config-key-state-modified";

  public static final String ADMIN_GUI_CONFIG_KEY_HINT_BOOLEAN =
      "gui.admin.config-key-hint-boolean";

  public static final String ADMIN_GUI_CONFIG_KEY_HINT_CHOICE = "gui.admin.config-key-hint-choice";

  public static final String ADMIN_GUI_CONFIG_KEY_HINT_VALUE = "gui.admin.config-key-hint-value";

  public static final String ADMIN_GUI_CONFIG_KEY_HINT_SHIFT = "gui.admin.config-key-hint-shift";

  public static final String ADMIN_GUI_CONFIG_KEY_HINT_RESET = "gui.admin.config-key-hint-reset";

  public static final String ADMIN_GUI_CONFIG_DETAIL_TITLE = "gui.admin.config-detail-title";

  public static final String ADMIN_GUI_CONFIG_DETAIL_INFO_NAME =
      "gui.admin.config-detail-info-name";

  public static final String ADMIN_GUI_CONFIG_DETAIL_INFO_LORE =
      "gui.admin.config-detail-info-lore";

  public static final String ADMIN_GUI_CONFIG_DETAIL_TOGGLE_NAME =
      "gui.admin.config-detail-toggle-name";

  public static final String ADMIN_GUI_CONFIG_DETAIL_TOGGLE_LORE =
      "gui.admin.config-detail-toggle-lore";

  public static final String ADMIN_GUI_CONFIG_DETAIL_NEXT_NAME =
      "gui.admin.config-detail-next-name";

  public static final String ADMIN_GUI_CONFIG_DETAIL_NEXT_LORE =
      "gui.admin.config-detail-next-lore";

  public static final String ADMIN_GUI_CONFIG_DETAIL_EXACT_NAME =
      "gui.admin.config-detail-exact-name";

  public static final String ADMIN_GUI_CONFIG_DETAIL_EXACT_LORE =
      "gui.admin.config-detail-exact-lore";

  public static final String ADMIN_GUI_CONFIG_DETAIL_RESET_NAME =
      "gui.admin.config-detail-reset-name";

  public static final String ADMIN_GUI_CONFIG_DETAIL_RESET_LORE =
      "gui.admin.config-detail-reset-lore";

  public static final String ADMIN_GUI_CONFIG_DETAIL_ADJUST_NAME =
      "gui.admin.config-detail-adjust-name";

  public static final String ADMIN_GUI_CONFIG_DETAIL_ADJUST_LORE =
      "gui.admin.config-detail-adjust-lore";

  public static final String ADMIN_GUI_CONFIG_DETAIL_ADJUST_FAILED =
      "gui.admin.config-detail-adjust-failed";

  public static final String ADMIN_GUI_CONFIG_DETAIL_NUMBER_HINT =
      "gui.admin.config-detail-number-hint";

  public static final String ADMIN_GUI_CONFIG_DETAIL_BACK = "gui.admin.config-detail-back";

  /**
   * New keys for the localized config editor. They are separate from the
   *
   * <p>legacy {@code config-key-*} keys so an older lang file on disk cannot
   *
   * <p>override the label/description layout after an upgrade.
   */
  public static final String ADMIN_GUI_CONFIG_ENTRY_SECTION_LORE =
      "gui.admin.config-entry-section-lore";

  public static final String ADMIN_GUI_CONFIG_ENTRY_SEARCH_LORE =
      "gui.admin.config-entry-search-lore";

  public static final String ADMIN_GUI_CONFIG_ENTRY_NAME = "gui.admin.config-entry-name";

  public static final String ADMIN_GUI_CONFIG_ENTRY_LORE = "gui.admin.config-entry-lore";

  public static final String ADMIN_GUI_CONFIG_ENTRY_PROMPT = "gui.admin.config-entry-prompt";

  public static final String ADMIN_GUI_CONFIG_ENTRY_DETAIL_TITLE =
      "gui.admin.config-entry-detail-title";

  public static final String ADMIN_GUI_CONFIG_ENTRY_DETAIL_INFO_NAME =
      "gui.admin.config-entry-detail-info-name";

  public static final String ADMIN_GUI_CONFIG_ENTRY_DETAIL_INFO_LORE =
      "gui.admin.config-entry-detail-info-lore";

  public static final String ADMIN_GUI_CONFIG_ENTRY_DETAIL_ADJUST_LORE =
      "gui.admin.config-entry-detail-adjust-lore";

  public static final String ADMIN_GUI_CONFIG_ENTRY_DIVIDER = "gui.admin.config-entry-divider";

  public static final String ADMIN_GUI_CONFIG_ENTRY_CHOICES = "gui.admin.config-entry-choices";

  public static final String ADMIN_GUI_CONFIG_ENTRY_CURRENT_NAME =
      "gui.admin.config-entry-current-name";

  public static final String ADMIN_GUI_CONFIG_ENTRY_STATE_DEFAULT =
      "gui.admin.config-entry-state-default";

  public static final String ADMIN_GUI_CONFIG_ENTRY_STATE_MODIFIED =
      "gui.admin.config-entry-state-modified";

  public static final String ADMIN_GUI_CONFIG_FILTER_CHANGED_NAME =
      "gui.admin.config-filter-changed-name";

  public static final String ADMIN_GUI_CONFIG_FILTER_CHANGED_LORE =
      "gui.admin.config-filter-changed-lore";

  public static final String ADMIN_GUI_CONFIG_FILTER_RESTART_NAME =
      "gui.admin.config-filter-restart-name";

  public static final String ADMIN_GUI_CONFIG_FILTER_RESTART_LORE =
      "gui.admin.config-filter-restart-lore";

  public static final String ADMIN_GUI_CONFIG_FILTER_EMPTY = "gui.admin.config-filter-empty";

  public static final String ADMIN_GUI_CONFIG_STATE_ON = "gui.admin.config-state-on";

  public static final String ADMIN_GUI_CONFIG_STATE_OFF = "gui.admin.config-state-off";

  public static final String ADMIN_GUI_CONFIG_CHANGED_NAME = "gui.admin.config-changed-name";

  public static final String ADMIN_GUI_CONFIG_CHANGED_LORE = "gui.admin.config-changed-lore";

  public static final String ADMIN_GUI_CONFIG_SCOPE_CHANGED = "gui.admin.config-scope-changed";

  public static final String ADMIN_GUI_CONFIG_SCOPE_RESTART = "gui.admin.config-scope-restart";

  public static final String ADMIN_GUI_CONFIG_SCOPE_CHANGED_RESTART =
      "gui.admin.config-scope-changed-restart";

  public static final String ADMIN_GUI_CONFIG_VALUE_TRUE = "gui.admin.config-value-true";

  public static final String ADMIN_GUI_CONFIG_VALUE_FALSE = "gui.admin.config-value-false";

  public static final String ADMIN_GUI_CONFIG_DETAIL_ADJUST_PREVIEW =
      "gui.admin.config-detail-adjust-preview";

  public static final String ADMIN_GUI_MISC_TITLE = "gui.admin.misc-title";

  public static final String ADMIN_GUI_MISC_INFO_NAME = "gui.admin.misc-info-name";

  public static final String ADMIN_GUI_MISC_INFO_LORE = "gui.admin.misc-info-lore";

  public static final String ADMIN_GUI_MISC_AUDIT_NAME = "gui.admin.misc-audit-name";

  public static final String ADMIN_GUI_MISC_AUDIT_LORE = "gui.admin.misc-audit-lore";

  public static final String ADMIN_GUI_MISC_EXPORT_NAME = "gui.admin.misc-export-name";

  public static final String ADMIN_GUI_MISC_EXPORT_LORE = "gui.admin.misc-export-lore";

  public static final String ADMIN_GUI_MISC_MAINTENANCE_NAME = "gui.admin.misc-maintenance-name";

  public static final String ADMIN_GUI_MISC_MAINTENANCE_LORE = "gui.admin.misc-maintenance-lore";

  public static final String ADMIN_GUI_MISC_PURGE_NAME = "gui.admin.misc-purge-name";

  public static final String ADMIN_GUI_MISC_PURGE_LORE = "gui.admin.misc-purge-lore";

  public static final String ADMIN_GUI_MISC_RELOAD_NAME = "gui.admin.misc-reload-name";

  public static final String ADMIN_GUI_MISC_RELOAD_LORE = "gui.admin.misc-reload-lore";

  public static final String ADMIN_GUI_MISC_HELP_NAME = "gui.admin.misc-help-name";

  public static final String ADMIN_GUI_MISC_HELP_LORE = "gui.admin.misc-help-lore";

  public static final String ADMIN_GUI_MISC_DATA_NAME = "gui.admin.misc-data-name";

  public static final String ADMIN_GUI_MISC_DATA_LORE = "gui.admin.misc-data-lore";

  public static final String ADMIN_GUI_MISC_RETENTION_NAME = "gui.admin.misc-retention-name";

  public static final String ADMIN_GUI_MISC_RETENTION_LORE = "gui.admin.misc-retention-lore";

  public static final String ADMIN_GUI_MISC_WEBHOOK_NAME = "gui.admin.misc-webhook-name";

  public static final String ADMIN_GUI_MISC_WEBHOOK_LORE = "gui.admin.misc-webhook-lore";

  public static final String ADMIN_GUI_MISC_WEBHOOK_DISABLED = "gui.admin.misc-webhook-disabled";

  public static final String ADMIN_GUI_MISC_WEBHOOK_SENT = "gui.admin.misc-webhook-sent";

  public static final String ADMIN_GUI_MISC_WEBHOOK_FAILED = "gui.admin.misc-webhook-failed";

  public static final String ADMIN_GUI_MAINTENANCE_DONE = "gui.admin.maintenance-done";

  public static final String ADMIN_GUI_CONFIRM_TITLE = "gui.admin.confirm-title";

  public static final String ADMIN_GUI_CONFIRM_YES_NAME = "gui.admin.confirm-yes-name";

  public static final String ADMIN_GUI_CONFIRM_YES_LORE = "gui.admin.confirm-yes-lore";

  public static final String ADMIN_GUI_CONFIRM_NO_NAME = "gui.admin.confirm-no-name";

  public static final String ADMIN_GUI_CONFIRM_NO_LORE = "gui.admin.confirm-no-lore";

  public static final String ADMIN_GUI_ACTION_PURGE = "gui.admin.action-purge";

  public static final String ADMIN_GUI_ACTION_MAINTENANCE = "gui.admin.action-maintenance";

  public static final String ADMIN_GUI_AUDIT_TITLE = "gui.admin.audit-title";

  public static final String ADMIN_GUI_AUDIT_ITEM_NAME = "gui.admin.audit-item-name";

  public static final String ADMIN_GUI_AUDIT_ITEM_LORE = "gui.admin.audit-item-lore";

  public static final String ADMIN_GUI_AUDIT_DISABLED = "gui.admin.audit-disabled";

  public static final String ADMIN_GUI_AUDIT_READ_FAILED = "gui.admin.audit-read-failed";

  public static final String ADMIN_GUI_QUICK_OPEN_NAME = "gui.admin.quick-open-name";

  public static final String ADMIN_GUI_QUICK_OPEN_LORE = "gui.admin.quick-open-lore";

  public static final String ADMIN_GUI_QUICK_OVERDUE_NAME = "gui.admin.quick-overdue-name";

  public static final String ADMIN_GUI_QUICK_OVERDUE_LORE = "gui.admin.quick-overdue-lore";

  public static final String ADMIN_GUI_QUICK_MINE_NAME = "gui.admin.quick-mine-name";

  public static final String ADMIN_GUI_QUICK_MINE_LORE = "gui.admin.quick-mine-lore";

  public static final String ADMIN_GUI_QUICK_NEXT_NAME = "gui.admin.quick-next-name";

  public static final String ADMIN_GUI_QUICK_NEXT_LORE = "gui.admin.quick-next-lore";

  public static final String ADMIN_GUI_QUEUE_TITLE = "gui.admin.queue-title";

  public static final String ADMIN_GUI_QUEUE_HEADER_NAME = "gui.admin.queue-header-name";

  public static final String ADMIN_GUI_QUEUE_HEADER_LORE = "gui.admin.queue-header-lore";

  public static final String ADMIN_GUI_QUEUE_ITEM_NAME = "gui.admin.queue-item-name";

  public static final String ADMIN_GUI_QUEUE_ITEM_OVERDUE = "gui.admin.queue-item-overdue";

  public static final String ADMIN_GUI_QUEUE_ITEM_QUICK_CLAIM =
      "gui.admin.queue-item-quick-claim";

  public static final String ADMIN_GUI_PLAYERS_HEADER_NAME = "gui.admin.players-header-name";

  public static final String ADMIN_GUI_PLAYERS_HEADER_LORE = "gui.admin.players-header-lore";

  public static final String ADMIN_GUI_PLAYER_REPORTS_HEADER_NAME =
      "gui.admin.player-reports-header-name";

  public static final String ADMIN_GUI_PLAYER_REPORTS_HEADER_LORE =
      "gui.admin.player-reports-header-lore";

  public static final String ADMIN_GUI_WATCHLIST_HEADER_NAME = "gui.admin.watchlist-header-name";

  public static final String ADMIN_GUI_WATCHLIST_HEADER_LORE = "gui.admin.watchlist-header-lore";

  public static final String ADMIN_GUI_AUDIT_HEADER_NAME = "gui.admin.audit-header-name";

  public static final String ADMIN_GUI_AUDIT_HEADER_LORE = "gui.admin.audit-header-lore";

  public static final String ADMIN_GUI_FILTER_HEADER_NAME = "gui.admin.filter-header-name";

  public static final String ADMIN_GUI_FILTER_HEADER_LORE = "gui.admin.filter-header-lore";

  public static final String ADMIN_GUI_MAINTENANCE_STARTED = "gui.admin.maintenance-started";

  public static final String ADMIN_GUI_CHAT_REVIEW_NAME = "gui.admin.chat-review-name";

  public static final String ADMIN_GUI_CHAT_REVIEW_LORE = "gui.admin.chat-review-lore";

  public static final String ADMIN_GUI_CHAT_TITLE = "gui.admin.chat-title";

  public static final String ADMIN_GUI_CHAT_ITEM_NAME = "gui.admin.chat-item-name";

  public static final String ADMIN_GUI_CHAT_SELECTED_NAME = "gui.admin.chat-selected-name";

  public static final String ADMIN_GUI_CHAT_ITEM_LORE = "gui.admin.chat-item-lore";

  public static final String ADMIN_GUI_CHAT_SELECTED_LORE = "gui.admin.chat-selected-lore";

  public static final String ADMIN_GUI_CHAT_EMPTY = "gui.admin.chat-empty";

  public static final String ADMIN_GUI_CHAT_SUMMARY_NAME = "gui.admin.chat-summary-name";

  public static final String ADMIN_GUI_CHAT_SUMMARY_LORE = "gui.admin.chat-summary-lore";

  public static final String NOTIFY_STAFF_NEW = "notify.staff-new";

  public static final String NOTIFY_STAFF_CLAIMED = "notify.staff-claimed";

  public static final String NOTIFY_STAFF_CLOSED = "notify.staff-closed";

  public static final String NOTIFY_STAFF_REOPENED = "notify.staff-reopened";

  public static final String NOTIFY_REPORTER_CLAIMED = "notify.reporter-claimed";

  public static final String NOTIFY_REPORTER_CLOSED = "notify.reporter-closed";

  public static final String NOTIFY_REPORTER_REOPENED = "notify.reporter-reopened";

  public static final String NOTIFY_REPORTER_REPLY = "notify.reporter-reply";

  public static final String NOTIFY_STAFF_REPLY = "notify.staff-reply";

  public static final String NOTIFY_STAFF_OVERDUE = "notify.staff-overdue";

  public static final String NOTIFY_STAFF_WATCHED = "notify.staff-watched";

  public static final String NOTIFY_BROADCAST_NEW = "notify.broadcast-new";

  public static final String ADMIN_HELP_HEADER = "admin.help-header";

  public static final String ADMIN_HELP_LIST = "admin.help-list";

  public static final String ADMIN_HELP_SEARCH = "admin.help-search";

  public static final String ADMIN_HELP_VIEW = "admin.help-view";

  public static final String ADMIN_HELP_CLAIM = "admin.help-claim";

  public static final String ADMIN_HELP_RELEASE = "admin.help-release";

  public static final String ADMIN_HELP_CLOSE = "admin.help-close";

  public static final String ADMIN_HELP_REOPEN = "admin.help-reopen";

  public static final String ADMIN_HELP_TP = "admin.help-tp";

  public static final String ADMIN_HELP_EVIDENCE = "admin.help-evidence";

  public static final String ADMIN_HELP_NOTE = "admin.help-note";

  public static final String ADMIN_HELP_DELETE = "admin.help-delete";

  public static final String ADMIN_HELP_PURGE = "admin.help-purge";

  public static final String ADMIN_HELP_STATS = "admin.help-stats";

  public static final String ADMIN_HELP_REPLY = "admin.help-reply";

  public static final String ADMIN_HELP_REPLIES = "admin.help-replies";

  public static final String ADMIN_HELP_OVERDUE = "admin.help-overdue";

  public static final String ADMIN_HELP_WATCH = "admin.help-watch";

  public static final String ADMIN_HELP_UNWATCH = "admin.help-unwatch";

  public static final String ADMIN_HELP_WATCHING = "admin.help-watching";

  public static final String ADMIN_WATCH_ADDED = "admin.watch-added";

  public static final String ADMIN_WATCH_REMOVED = "admin.watch-removed";

  public static final String ADMIN_WATCH_LIMIT = "admin.watch-limit";

  public static final String ADMIN_WATCH_DISABLED = "admin.watch-disabled";

  public static final String ADMIN_WATCH_NOT_FOUND = "admin.watch-not-found";

  public static final String ADMIN_WATCHING_HEADER = "admin.watching-header";

  public static final String ADMIN_WATCHING_EMPTY = "admin.watching-empty";

  public static final String ADMIN_WATCHING_LINE = "admin.watching-line";

  public static final String ADMIN_REPLY_SENT = "admin.reply-sent";

  public static final String ADMIN_REPLY_DISABLED = "admin.reply-disabled";

  public static final String ADMIN_REPLY_FULL = "admin.reply-full";

  public static final String ADMIN_REPLIES_HEADER = "admin.replies-header";

  public static final String ADMIN_REPLIES_EMPTY = "admin.replies-empty";

  public static final String ADMIN_REPLY_LINE = "admin.reply-line";

  public static final String ADMIN_STATS_STAFF = "admin.stats-staff";

  public static final String ADMIN_HELP_EXPORT = "admin.help-export";

  public static final String ADMIN_HELP_AUDIT = "admin.help-audit";

  public static final String ADMIN_HELP_RELOAD = "admin.help-reload";

  public static final String ADMIN_HELP_CONFIG = "admin.help-config";

  public static final String ADMIN_HELP_MENU = "admin.help-menu";

  public static final String CONFIG_LIST_HEADER = "config.list-header";

  public static final String CONFIG_LIST_LINE = "config.list-line";

  public static final String CONFIG_LIST_EMPTY = "config.list-empty";

  public static final String CONFIG_GET = "config.get";

  public static final String CONFIG_DEFAULT = "config.default";

  public static final String CONFIG_RESTART = "config.restart";

  public static final String CONFIG_APPLIED = "config.applied";

  public static final String CONFIG_RESET = "config.reset";

  public static final String CONFIG_REJECTED = "config.rejected";

  public static final String CONFIG_DISABLED = "config.disabled";

  public static final String CONFIG_LOADING = "config.loading";

  public static final String CONFIG_BUSY = "config.busy";

  public static final String CONFIG_UNKNOWN = "config.unknown";

  public static final String CONFIG_WRITE_FAILED = "config.write-failed";

  public static final String ADMIN_CLAIMED = "admin.claimed";

  public static final String ADMIN_ALREADY_CLAIMED = "admin.already-claimed";

  public static final String ADMIN_RELEASED = "admin.released";

  public static final String ADMIN_NOT_CLAIMED = "admin.not-claimed";

  public static final String ADMIN_CLOSED = "admin.closed";

  public static final String ADMIN_ALREADY_CLOSED = "admin.already-closed";

  public static final String ADMIN_REOPENED = "admin.reopened";

  public static final String ADMIN_NOT_CLOSED = "admin.not-closed";

  public static final String ADMIN_DELETED = "admin.deleted";

  public static final String ADMIN_NOTE_ADDED = "admin.note-added";

  public static final String ADMIN_NOTES_FULL = "admin.notes-full";

  public static final String ADMIN_TELEPORTED = "admin.teleported";

  public static final String ADMIN_TARGET_NOT_ONLINE = "admin.target-not-online";

  public static final String ADMIN_EVIDENCE_DISABLED = "admin.evidence-disabled";

  public static final String ADMIN_EVIDENCE_EMPTY = "admin.evidence-empty";

  public static final String ADMIN_PURGED = "admin.purged";

  public static final String ADMIN_PURGE_NONE = "admin.purge-none";

  public static final String ADMIN_EXPORT_STARTED = "admin.export-started";

  public static final String ADMIN_EXPORT_DONE = "admin.export-done";

  public static final String ADMIN_EXPORT_FAILED = "admin.export-failed";

  public static final String ADMIN_AUDIT_HEADER = "admin.audit-header";

  public static final String ADMIN_AUDIT_LINE = "admin.audit-line";

  public static final String ADMIN_AUDIT_EMPTY = "admin.audit-empty";

  public static final String ADMIN_STATS_HEADER = "admin.stats-header";

  public static final String ADMIN_STATS_TOTAL = "admin.stats-total";

  public static final String ADMIN_STATS_STATUS = "admin.stats-status";

  public static final String ADMIN_STATS_VERDICTS = "admin.stats-verdicts";

  public static final String ADMIN_STATS_AVG = "admin.stats-avg";

  public static final String ADMIN_STATS_CATEGORY = "admin.stats-category";

  public static final String ADMIN_STATS_TOP_TARGET = "admin.stats-top-target";

  public static final String ADMIN_LIST_HEADER = "admin.list-header";

  public static final String ADMIN_LIST_LINE = "admin.list-line";

  public static final String ADMIN_LIST_FOOTER = "admin.list-footer";

  public static final String ADMIN_LIST_UNKNOWN_FILTER = "admin.list-unknown-filter";

  public static final String ADMIN_INFO_HEADER = "admin.info-header";

  public static final String ADMIN_INFO_LINE = "admin.info-line";

  public static final String ADMIN_RELOADED = "admin.reloaded";

  public static final String ADMIN_BANNED_WORD = "admin.banned-word";

  public static final String ADMIN_CONSOLE_ACTOR = "admin.console-actor";

  public static final String EVIDENCE_HEADER = "evidence.header";

  public static final String EVIDENCE_TARGET = "evidence.target";

  public static final String EVIDENCE_CAPTURED_AT = "evidence.captured-at";

  public static final String EVIDENCE_LOCATION = "evidence.location";

  public static final String EVIDENCE_GAME_MODE = "evidence.game-mode";

  public static final String EVIDENCE_HEALTH = "evidence.health";

  public static final String EVIDENCE_INVENTORY = "evidence.inventory";

  public static final String EVIDENCE_ARMOR = "evidence.armor";

  public static final String EVIDENCE_CHAT = "evidence.chat";

  public static final String EVIDENCE_CHAT_LINE = "evidence.chat-line";

  public static final String EVIDENCE_BLOCK_BREAK = "evidence.block-break";

  public static final String EVIDENCE_BLOCK_BREAK_LINE = "evidence.block-break-line";

  public static final String EVIDENCE_MOVEMENT = "evidence.movement";

  public static final String EVIDENCE_MOVEMENT_LINE = "evidence.movement-line";

  public static final String EVIDENCE_OFFLINE = "evidence.offline";

  public static final String EVIDENCE_NOT_CAPTURED = "evidence.not-captured";

  public static final String EVIDENCE_INVENTORY_EMPTY = "evidence.inventory-empty";

  public static final String EVIDENCE_CHAT_EMPTY = "evidence.chat-empty";

  public static final String EVIDENCE_AUTO_REPORT_DETAIL = "evidence.auto-report-detail";

  public static final String EVIDENCE_AUTO_REPORT_STAFF = "evidence.auto-report-staff";

  public static final String WEBHOOK_TITLE_CREATED = "webhook.title-created";

  public static final String WEBHOOK_TITLE_CLAIMED = "webhook.title-claimed";

  public static final String WEBHOOK_TITLE_CLOSED = "webhook.title-closed";

  public static final String WEBHOOK_FIELD_TARGET = "webhook.field-target";

  public static final String WEBHOOK_FIELD_REPORTER = "webhook.field-reporter";

  public static final String WEBHOOK_FIELD_CATEGORY = "webhook.field-category";

  public static final String WEBHOOK_FIELD_STATUS = "webhook.field-status";

  public static final String WEBHOOK_FIELD_STAFF = "webhook.field-staff";

  public static final String WEBHOOK_FIELD_ID = "webhook.field-id";

  public static final String WEBHOOK_FIELD_DETAIL = "webhook.field-detail";

  public static final String WEBHOOK_FIELD_SERVER = "webhook.field-server";

  public static final String WEBHOOK_CONTENT_CREATED = "webhook.content-created";

  public static final String WEBHOOK_CONTENT_CLAIMED = "webhook.content-claimed";

  public static final String WEBHOOK_CONTENT_CLOSED = "webhook.content-closed";

  public static final String WEBHOOK_TEST_TITLE = "webhook.test-title";

  public static final String WEBHOOK_TEST_CONTENT = "webhook.test-content";

  public static final String WEBHOOK_FAILURE_ALERT = "webhook.failure-alert";

  public static final String TIME_NOW = "time.now";

  public static final String TIME_SECONDS_AGO = "time.seconds-ago";

  public static final String TIME_MINUTES_AGO = "time.minutes-ago";

  public static final String TIME_HOURS_AGO = "time.hours-ago";

  public static final String TIME_DAYS_AGO = "time.days-ago";

  public static final String TIME_MINUTE = "time.minute";

  public static final String TIME_SECOND = "time.second";

  public static final String TIME_FORMAT_DATE = "time-format.date";

  // Bedrock form adaptation (Geyser + Floodgate + Cumulus).

  public static final String BEDROCK_FORM_REPORT_TITLE = "bedrock.report-title";

  public static final String BEDROCK_FORM_REPORT_CONTENT = "bedrock.report-content";

  public static final String BEDROCK_FORM_TARGET_PLACEHOLDER = "bedrock.target-placeholder";

  public static final String BEDROCK_FORM_DETAIL_PLACEHOLDER = "bedrock.detail-placeholder";

  public static final String BEDROCK_FORM_REPORT_SUBMITTED = "bedrock.report-submitted";

  public static final String BEDROCK_FORM_HISTORY_TITLE = "bedrock.history-title";

  public static final String BEDROCK_FORM_HISTORY_CONTENT = "bedrock.history-content";

  public static final String BEDROCK_FORM_ADMIN_TITLE = "bedrock.admin-title";

  public static final String BEDROCK_FORM_ADMIN_CONTENT = "bedrock.admin-content";
}
