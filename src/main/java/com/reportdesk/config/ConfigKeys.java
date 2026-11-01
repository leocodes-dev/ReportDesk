/*
 * ReportDesk - Enterprise-grade player reporting and evidence system.
 * Copyright (C) 2026 William
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.reportdesk.config;
import java.util.List; import java.util.Locale; import java.util.Optional; import java.util.function.Function;
/**
* Registry of every scalar setting that can be viewed and edited in game.
*
* <p>Structure-only settings (report categories, punishment command lists,
* blocked words) are intentionally not listed: editing them line by line in a
* chat command would be error prone and they remain file-only settings.</p>
*/ public final class ConfigKeys {


/** Synthetic section for keys stored at the root of config.yml. */

 public static final String GENERAL_SECTION = "general";


private static final List<String> PRIORITY_LEVELS = List.of("low", "normal", "high", "urgent");


private static final List<ConfigKey> KEYS = List.of(



 string("server-name", PluginConfig::serverName),



 string("language", PluginConfig::language),




restartString("storage.directory", config -> config.storage().directory()),



 restartBool("storage.backup-on-save", config -> config.storage().backupOnSave()),



 restartInt("storage.max-backups", config -> config.storage().maxBackups()),



 restartInt("storage.backup-interval-minutes",





 config -> (int) config.storage().backupIntervalMinutes()),



 restartInt("storage.shutdown-timeout-seconds",





 config -> config.storage().shutdownTimeoutSeconds()),



 restartInt("storage.compact-after-entries",





 config -> config.storage().compactAfterEntries()),



 restartLong("storage.compact-interval-minutes",





 config -> config.storage().compactIntervalMinutes()),




longKey("limits.command-cooldown-millis", config -> config.limits().commandCooldownMillis()),



 intKey("limits.cooldown-seconds", config -> (int) config.limits().cooldownSeconds()),



 intKey("limits.daily-limit", config -> config.limits().dailyLimit()),



 intKey("limits.same-target-interval-seconds",





 config -> (int) config.limits().sameTargetIntervalSeconds()),



 intKey("limits.duplicate-window-seconds",





 config -> (int) config.limits().duplicateWindowSeconds()),



 intKey("limits.max-open-per-target", config -> config.limits().maxOpenPerTarget()),



 intKey("limits.min-account-age-minutes", config -> config.limits().minAccountAgeMinutes()),



 bool("limits.block-self-report", config -> config.limits().blockSelfReport()),



 bool("limits.allow-offline-target", config -> config.limits().allowOfflineTarget()),



 bool("limits.require-target-online", config -> config.limits().requireTargetOnline()),



 string("limits.bypass-permission", config -> config.limits().bypassPermission()),




intKey("reports.text-page-size", config -> config.reports().textPageSize()),



 intKey("reports.gui-page-size", config -> config.reports().guiPageSize()),



 bool("reports.auto-close-enabled", config -> config.reports().autoCloseEnabled()),



 intKey("reports.auto-close-days", config -> config.reports().autoCloseDays()),



 intKey("reports.retention-days", config -> config.reports().retentionDays()),



 intKey("reports.maintenance-interval-minutes",





 config -> config.reports().maintenanceIntervalMinutes()),



 intKey("reports.stats-top-targets", config -> config.reports().statsTopTargets()),



 intKey("reports.staff-stats-top", config -> config.reports().staffStatsTop()),



 string("reports.default-category", config -> config.reports().defaultCategory()),




bool("reports.priority.enabled", config -> config.reports().priority().enabled()),



 intKey("reports.priority.repeat-offender-threshold",





 config -> config.reports().priority().repeatOffenderThreshold()),



 choice("reports.priority.repeat-offender-level", PRIORITY_LEVELS,





 config -> config.reports().priority().repeatOffenderLevel().id()),



 intKey("reports.priority.overdue-check-minutes",





 config -> config.reports().priority().overdueCheckMinutes()),



 bool("reports.priority.overdue-alert-enabled",





 config -> config.reports().priority().overdueAlertEnabled()),



 intKey("reports.priority.sla-hours.urgent",





 config -> config.reports().priority().slaUrgentHours()),



 intKey("reports.priority.sla-hours.high",





 config -> config.reports().priority().slaHighHours()),



 intKey("reports.priority.sla-hours.normal",





 config -> config.reports().priority().slaNormalHours()),



 intKey("reports.priority.sla-hours.low",





 config -> config.reports().priority().slaLowHours()),




bool("reports.replies.enabled", config -> config.reports().replies().enabled()),



 bool("reports.replies.allow-player-replies",





 config -> config.reports().replies().allowPlayerReplies()),



 intKey("reports.replies.max-length", config -> config.reports().replies().maxLength()),



 intKey("reports.replies.max-per-report", config -> config.reports().replies().maxPerReport()),



 bool("reports.replies.notify", config -> config.reports().replies().notifyOnReply()),




bool("reports.chat-report.enabled", config -> config.reports().chatReport().enabled()),



 string("reports.chat-report.category", config -> config.reports().chatReport().category()),



 intKey("reports.chat-report.max-selected",





 config -> config.reports().chatReport().maxSelected()),




bool("gui.enabled", config -> config.gui().enabled()),



 bool("gui.fill-empty-slots", config -> config.gui().fillEmptySlots()),



 bool("gui.close-on-action", config -> config.gui().closeOnAction()),



 bool("gui.sounds.enabled", config -> config.gui().soundsEnabled()),



 string("gui.sounds.click", config -> config.gui().soundClick()),



 string("gui.sounds.success", config -> config.gui().soundSuccess()),



 string("gui.sounds.error", config -> config.gui().soundError()),



 bool("gui.admin.enabled", config -> config.gui().admin().enabled()),



 bool("gui.admin.confirm-destructive", config -> config.gui().admin().confirmDestructive()),




bool("notifications.staff.enabled", config -> config.notifications().staffEnabled()),



 string("notifications.staff.permission", config -> config.notifications().staffPermission()),



 bool("notifications.staff.sound-enabled", config -> config.notifications().staffSoundEnabled()),



 string("notifications.staff.sound", config -> config.notifications().staffSound()),



 bool("notifications.reporter.enabled", config -> config.notifications().reporterEnabled()),



 bool("notifications.reporter.queue-offline",





 config -> config.notifications().reporterQueueOffline()),



 intKey("notifications.reporter.max-queued",





 config -> config.notifications().reporterMaxQueued()),



 intKey("notifications.reporter.queue-retention-days",





 config -> config.notifications().reporterQueueRetentionDays()),



 bool("notifications.watchlist.enabled", config -> config.notifications().watchlistEnabled()),



 intKey("notifications.watchlist.max-targets",





 config -> config.notifications().watchlistMaxTargets()),



 bool("notifications.watchlist.notify-on-report",





 config -> config.notifications().watchlistNotifyOnReport()),



 bool("notifications.broadcast.enabled", config -> config.notifications().broadcastEnabled()),



 bool("notifications.webhook.enabled", config -> config.notifications().webhook().enabled()),



 string("notifications.webhook.url", config -> config.notifications().webhook().url()),



 intKey("notifications.webhook.timeout-seconds",





 config -> config.notifications().webhook().timeoutSeconds()),



 string("notifications.webhook.username", config -> config.notifications().webhook().username()),



 string("notifications.webhook.avatar-url", config -> config.notifications().webhook().avatarUrl()),



 bool("notifications.webhook.events.created",





 config -> config.notifications().webhook().eventCreated()),



 bool("notifications.webhook.events.claimed",





 config -> config.notifications().webhook().eventClaimed()),



 bool("notifications.webhook.events.closed",





 config -> config.notifications().webhook().eventClosed()),




bool("evidence.enabled", config -> config.evidence().enabled()),



 bool("evidence.capture-on-report", config -> config.evidence().captureOnReport()),



 intKey("evidence.retention-days", config -> config.evidence().retentionDays()),



 intKey("evidence.chat-log-size", config -> config.evidence().chatLogSize()),



 intKey("evidence.chat-max-length", config -> config.evidence().chatMaxLength()),



 bool("evidence.capture.location", config -> config.evidence().captureLocation()),



 bool("evidence.capture.game-mode", config -> config.evidence().captureGameMode()),



 bool("evidence.capture.health", config -> config.evidence().captureHealth()),



 bool("evidence.capture.inventory", config -> config.evidence().captureInventory()),



 intKey("evidence.capture.inventory-max-items",





 config -> config.evidence().inventoryMaxItems()),



 intKey("evidence.capture.display-name-max-length",





 config -> config.evidence().displayNameMaxLength()),



 bool("evidence.capture.chat-log", config -> config.evidence().captureChatLog()),



 bool("evidence.capture.ping", config -> config.evidence().capturePing()),



 bool("evidence.block-break.enabled", config -> config.evidence().blockBreak().enabled()),



 intKey("evidence.block-break.max-per-player",





 config -> config.evidence().blockBreak().maxPerPlayer()),



 bool("evidence.movement.enabled", config -> config.evidence().movement().enabled()),



 intKey("evidence.movement.min-interval-ticks",





 config -> config.evidence().movement().minIntervalTicks()),



 intKey("evidence.movement.max-samples", config -> config.evidence().movement().maxSamples()),



 bool("evidence.auto-detect.enabled", config -> config.evidence().autoDetect().enabled()),



 intKey("evidence.auto-detect.check-interval-seconds",





 config -> config.evidence().autoDetect().checkIntervalSeconds()),



 intKey("evidence.auto-detect.window-seconds",





 config -> config.evidence().autoDetect().windowSeconds()),



 intKey("evidence.auto-detect.block-break-threshold",





 config -> config.evidence().autoDetect().blockBreakThreshold()),



 intKey("evidence.auto-detect.cooldown-seconds",





 config -> config.evidence().autoDetect().cooldownSeconds()),



 bool("evidence.auto-detect.create-report",





 config -> config.evidence().autoDetect().createReport()),



 bool("evidence.auto-detect.notify-staff",





 config -> config.evidence().autoDetect().notifyStaff()),




bool("punishments.enabled", config -> config.punishments().enabled()),




bool("audit.enabled", config -> config.audit().enabled()),



 intKey("audit.retention-days", config -> config.audit().retentionDays()),



 intKey("audit.page-size", config -> config.audit().pageSize()),




longKey("security.admin-cooldown-millis", config -> config.security().adminCooldownMillis()),



 intKey("security.pending-timeout-seconds", config -> config.security().pendingTimeoutSeconds()),



 intKey("security.detail-min-length", config -> config.security().detailMinLength()),



 intKey("security.detail-max-length", config -> config.security().detailMaxLength()),



 intKey("security.note-max-length", config -> config.security().noteMaxLength()),



 intKey("security.max-notes-per-report", config -> config.security().maxNotesPerReport()),



 bool("security.detail-required", config -> config.security().detailRequired()),



 bool("security.allow-color-codes", config -> config.security().allowColorCodes()),



 string("security.player-name-pattern", config -> config.security().playerNamePattern()),




bool("config-editor.enabled", config -> config.configEditor().enabled()),



 bool("config-editor.backup-on-save", config -> config.configEditor().backupOnSave()),



 intKey("config-editor.max-backups", config -> config.configEditor().maxBackups()),



 intKey("config-editor.max-value-length", config -> config.configEditor().maxValueLength())

 );


private ConfigKeys() {

 }


public static List<ConfigKey> all() {



 return KEYS;

 }


public static Optional<ConfigKey> find(String path) {



 if (path == null) {





 return Optional.empty();



 }



 String normalized = path.trim().toLowerCase(Locale.ROOT);



 for (ConfigKey key : KEYS) {





 if (key.path().equals(normalized)) {







 return Optional.of(key);





 }



 }



 return Optional.empty();

 }


public static List<String> paths() {



 return KEYS.stream().map(ConfigKey::path).toList();

 }


/**


* Top-level config.yml sections that contain at least one editable key, in


* registry order. Used by the admin config page. Keys that live at the root


* of config.yml (for example {@code server-name}) are grouped under the


* synthetic {@code general} section so every key is reachable.


*/

 public static List<String> sections() {



 java.util.LinkedHashSet<String> sections = new java.util.LinkedHashSet<>();



 for (ConfigKey key : KEYS) {





 int dot = key.path().indexOf('.');





 sections.add(dot < 0 ? GENERAL_SECTION : key.path().substring(0, dot));



 }



 return List.copyOf(sections);

 }


public static List<ConfigKey> inSection(String section) {



 if (section == null || section.isBlank()) {





 return List.of();



 }



 String normalized = section.trim().toLowerCase(Locale.ROOT);



 if (GENERAL_SECTION.equals(normalized)) {





 return KEYS.stream().filter(key -> key.path().indexOf('.') < 0).toList();



 }



 String prefix = normalized + ".";



 return KEYS.stream().filter(key -> key.path().startsWith(prefix)).toList();

 }


private static ConfigKey bool(String path, Function<PluginConfig, Object> getter) {



 return new ConfigKey(path, ConfigKey.Kind.BOOLEAN, List.of(), getter, false);

 }


private static ConfigKey restartBool(String path, Function<PluginConfig, Object> getter) {



 return new ConfigKey(path, ConfigKey.Kind.BOOLEAN, List.of(), getter, true);

 }


private static ConfigKey intKey(String path, Function<PluginConfig, Object> getter) {



 return new ConfigKey(path, ConfigKey.Kind.INTEGER, List.of(), getter, false);

 }


private static ConfigKey restartInt(String path, Function<PluginConfig, Object> getter) {



 return new ConfigKey(path, ConfigKey.Kind.INTEGER, List.of(), getter, true);

 }


private static ConfigKey restartLong(String path, Function<PluginConfig, Object> getter) {



 return new ConfigKey(path, ConfigKey.Kind.LONG, List.of(), getter, true);

 }


private static ConfigKey longKey(String path, Function<PluginConfig, Object> getter) {



 return new ConfigKey(path, ConfigKey.Kind.LONG, List.of(), getter, false);

 }


private static ConfigKey string(String path, Function<PluginConfig, Object> getter) {



 return new ConfigKey(path, ConfigKey.Kind.STRING, List.of(), getter, false);

 }


private static ConfigKey restartString(String path, Function<PluginConfig, Object> getter) {



 return new ConfigKey(path, ConfigKey.Kind.STRING, List.of(), getter, true);

 }


private static ConfigKey choice(String path, List<String> suggestions,

















 Function<PluginConfig, Object> getter) {



 return new ConfigKey(path, ConfigKey.Kind.CHOICE, suggestions, getter, false);

 } }
