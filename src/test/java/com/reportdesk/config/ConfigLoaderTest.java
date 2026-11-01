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
import org.junit.jupiter.api.Test; import org.yaml.snakeyaml.Yaml;
import java.io.InputStream; import java.util.ArrayList; import java.util.List; import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals; import static org.junit.jupiter.api.Assertions.assertFalse; import static org.junit.jupiter.api.Assertions.assertNotNull; import static org.junit.jupiter.api.Assertions.assertTrue;
class ConfigLoaderTest {


@Test

 void bundledConfigLoadsWithoutWarnings() {



 List<String> warnings = new ArrayList<>();



 PluginConfig config = new ConfigLoader(warnings::add).load(bundledConfig());




assertTrue(warnings.isEmpty(), () -> "unexpected warnings: " + warnings);



 assertEquals("ReportDesk Server", config.serverName());



 assertEquals("zh_CN", config.language());



 assertEquals("data", config.storage().directory());



 assertEquals(50, config.storage().compactAfterEntries());



 assertEquals(360L, config.storage().compactIntervalMinutes());



 assertEquals(60, config.limits().cooldownSeconds());



 assertEquals(10, config.limits().dailyLimit());



 assertEquals(10, config.reports().textPageSize());



 assertEquals(28, config.reports().guiPageSize());



 assertEquals(3, config.reports().statsTopTargets());



 assertEquals("ui.button.click", config.gui().soundClick());



 assertEquals("block.note_block.pling", config.notifications().staffSound());



 assertTrue(config.notifications().reporterQueueOffline());



 assertEquals(20, config.notifications().reporterMaxQueued());



 assertEquals(30, config.notifications().reporterQueueRetentionDays());



 assertTrue(config.notifications().watchlistEnabled());



 assertEquals(20, config.notifications().watchlistMaxTargets());



 assertTrue(config.notifications().watchlistNotifyOnReport());



 assertTrue(config.configEditor().enabled());



 assertTrue(config.configEditor().backupOnSave());



 assertEquals(5, config.configEditor().maxBackups());



 assertEquals(256, config.configEditor().maxValueLength());



 assertTrue(config.gui().admin().enabled());



 assertTrue(config.gui().admin().confirmDestructive());



 assertEquals(20, config.evidence().chatLogSize());



 assertEquals(256, config.evidence().chatMaxLength());



 assertEquals(80, config.evidence().displayNameMaxLength());



 assertEquals(7, config.categories().size());



 assertEquals("cheating", config.category("hack").orElseThrow().id());



 assertEquals("high", config.category("cheating").orElseThrow().severity().id());



 assertEquals("other", config.defaultCategory());



 assertEquals(5, config.reports().staffStatsTop());



 assertTrue(config.reports().priority().enabled());



 assertEquals(3, config.reports().priority().repeatOffenderThreshold());



 assertEquals("high", config.reports().priority().repeatOffenderLevel().id());



 assertEquals(15, config.reports().priority().overdueCheckMinutes());



 assertEquals(2, config.reports().priority().slaHours(com.reportdesk.core.ReportPriority.URGENT));



 assertEquals(72, config.reports().priority().slaHours(com.reportdesk.core.ReportPriority.LOW));



 assertTrue(config.replies().enabled());



 assertTrue(config.replies().allowPlayerReplies());



 assertEquals(300, config.replies().maxLength());



 assertEquals(50, config.replies().maxPerReport());



 assertTrue(config.replies().notifyOnReply());



 assertFalse(config.reports().chatReport().enabled());



 assertEquals("chat", config.reports().chatReport().category());



 assertEquals(5, config.reports().chatReport().maxSelected());



 assertFalse(config.chatReportAvailable());



 assertEquals(2, config.priorityPolicy().slaHours(com.reportdesk.core.ReportPriority.URGENT));



 assertFalse(config.evidence().enabled());



 assertFalse(config.evidence().blockBreak().enabled());



 assertFalse(config.evidence().movement().enabled());



 assertFalse(config.evidence().autoDetect().enabled());



 assertFalse(config.punishments().enabled());

 }


@Test

 void emptyConfigUsesSafeDefaults() {



 List<String> warnings = new ArrayList<>();



 PluginConfig config = new ConfigLoader(warnings::add).load(new MapConfigSource(Map.of()));




assertEquals(50, config.storage().compactAfterEntries());



 assertEquals(360L, config.storage().compactIntervalMinutes());



 assertEquals(60, config.limits().cooldownSeconds());



 assertEquals(10, config.limits().dailyLimit());



 assertEquals(10, config.reports().textPageSize());



 assertEquals(28, config.reports().guiPageSize());



 assertEquals(3, config.reports().statsTopTargets());



 assertEquals(2, config.security().detailMinLength());



 assertEquals(200, config.security().detailMaxLength());



 assertTrue(config.gui().admin().enabled());



 assertTrue(config.gui().admin().confirmDestructive());



 assertNotNull(config.categories());



 assertEquals(1, config.categories().size());



 assertEquals("other", config.defaultCategory());



 assertFalse(warnings.isEmpty());

 }


@Test

 void invalidValuesAreClampedOrReplaced() {



 Map<String, Object> root = Map.of(





 "storage", Map.of("directory", "../outside"),





 "limits", Map.of("cooldown-seconds", -5, "daily-limit", 999_999),





 "security", Map.of(







 "detail-min-length", 50,







 "detail-max-length", 10,







 "player-name-pattern", "["





 ),





 "notifications", Map.of(







 "webhook", Map.of("enabled", true, "url", "ftp://example.com/hook"),







 "reporter", Map.of("max-queued", 0, "queue-retention-days", -1),







 "watchlist", Map.of("max-targets", 999)





 ),





 "reports", Map.of(







 "categories", Map.of("broken", "not-a-section"),







 "default-category", "does-not-exist",







 "stats-top-targets", 99





 ),





 "config-editor", Map.of("max-backups", -5, "max-value-length", 0)



 );



 List<String> warnings = new ArrayList<>();



 PluginConfig config = new ConfigLoader(warnings::add).load(new MapConfigSource(root));




assertEquals("data", config.storage().directory());



 assertEquals(0, config.limits().cooldownSeconds());



 assertEquals(10_000, config.limits().dailyLimit());



 assertEquals(50, config.security().detailMaxLength());



 assertEquals(20, config.reports().statsTopTargets());



 assertEquals(ConfigLoader.DEFAULT_PLAYER_PATTERN, config.security().playerNamePattern());



 assertFalse(config.notifications().webhook().enabled());



 assertEquals(1, config.notifications().reporterMaxQueued());



 assertEquals(0, config.notifications().reporterQueueRetentionDays());



 assertEquals(200, config.notifications().watchlistMaxTargets());



 assertEquals(0, config.configEditor().maxBackups());



 assertEquals(1, config.configEditor().maxValueLength());



 assertEquals(1, config.categories().size());



 assertEquals("other", config.defaultCategory());



 assertFalse(warnings.isEmpty());

 }


@Test

 void chatReportRequiresEvidenceChatCaptureAndAValidCategory() {



 List<String> warnings = new ArrayList<>();



 PluginConfig aliasResolution = new ConfigLoader(warnings::add).load(chatConfig(





 true, "insult", 3, true, true));



 assertTrue(aliasResolution.reports().chatReport().enabled());



 assertEquals("chat", aliasResolution.reports().chatReport().category());



 assertEquals(3, aliasResolution.reports().chatReport().maxSelected());



 assertTrue(aliasResolution.chatReportAvailable());




assertFalse(new ConfigLoader(warnings::add).load(chatConfig(true, "chat", 5, false, true))





 .chatReportAvailable());



 assertFalse(new ConfigLoader(warnings::add).load(chatConfig(true, "chat", 5, true, false))





 .chatReportAvailable());

 }


@Test

 void unknownOrDisabledChatCategoryDisablesThePicker() {



 List<String> unknownWarnings = new ArrayList<>();



 PluginConfig unknown = new ConfigLoader(unknownWarnings::add).load(





 chatConfig(true, "missing-category", 5, true, true));



 assertFalse(unknown.reports().chatReport().enabled());



 assertFalse(unknown.chatReportAvailable());



 assertTrue(unknownWarnings.stream().anyMatch(warning -> warning.contains("chat-report.category")),





 () -> "expected a chat-report warning, got " + unknownWarnings);




List<String> disabledWarnings = new ArrayList<>();



 PluginConfig disabled = new ConfigLoader(disabledWarnings::add).load(





 chatConfig(true, "disabled", 5, true, true));



 assertFalse(disabled.reports().chatReport().enabled());



 assertFalse(disabled.chatReportAvailable());



 assertTrue(disabledWarnings.stream().anyMatch(warning -> warning.contains("chat-report.category")),





 () -> "expected a chat-report warning, got " + disabledWarnings);

 }


@Test

 void chatReportMaxSelectedIsClamped() {



 List<String> warnings = new ArrayList<>();



 PluginConfig tooSmall = new ConfigLoader(warnings::add).load(





 chatConfig(true, "chat", 0, true, true));



 assertEquals(1, tooSmall.reports().chatReport().maxSelected());




PluginConfig tooLarge = new ConfigLoader(warnings::add).load(





 chatConfig(true, "chat", 500, true, true));



 assertEquals(100, tooLarge.reports().chatReport().maxSelected());

 }


private static MapConfigSource chatConfig(boolean enabled, String category, int maxSelected,






















 boolean evidenceEnabled, boolean chatLog) {



 return new MapConfigSource(Map.of(





 "reports", Map.of(







 "categories", Map.of(









 "chat", Map.of(











 "display", "Chat",











 "icon", "PAPER",











 "aliases", List.of("insult"),











 "enabled", true









 ),









 "disabled", Map.of(











 "display", "Disabled",











 "icon", "BARRIER",











 "enabled", false









 )







 ),







 "default-category", "chat",







 "chat-report", Map.of(









 "enabled", enabled,









 "category", category,









 "max-selected", maxSelected







 )





 ),





 "evidence", Map.of(







 "enabled", evidenceEnabled,







 "capture-on-report", true,







 "capture", Map.of("chat-log", chatLog)





 )



 ));

 }


private static MapConfigSource bundledConfig() {



 try (InputStream input = ConfigLoaderTest.class.getResourceAsStream("/config.yml")) {





 assertNotNull(input, "bundled config.yml");





 Object loaded = new Yaml().load(input);





 assertTrue(loaded instanceof Map<?, ?>, "config.yml must be a mapping");





 return new MapConfigSource((Map<?, ?>) loaded);



 } catch (java.io.IOException exception) {





 throw new AssertionError(exception);



 }

 } }
