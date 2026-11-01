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

package com.reportdesk.gui;
import com.reportdesk.config.ConfigKey; import com.reportdesk.config.ConfigKeys; import com.reportdesk.config.ConfigLoader; import com.reportdesk.config.MapConfigSource; import com.reportdesk.config.PluginConfig; import org.junit.jupiter.api.Test;
import java.util.List; import java.util.Map; import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals; import static org.junit.jupiter.api.Assertions.assertFalse; import static org.junit.jupiter.api.Assertions.assertTrue;
class ConfigGuiSupportTest {


private static final PluginConfig DEFAULTS =



 new ConfigLoader(ignored -> { }).load(new MapConfigSource(Map.of()));


private static final PluginConfig CHANGED =



 new ConfigLoader(ignored -> { }).load(new MapConfigSource(Map.of(





 "limits", Map.of("daily-limit", 7),





 "gui", Map.of("admin", Map.of("enabled", false)),





 "reports", Map.of("priority", Map.of("repeat-offender-level", "urgent"))



 )));


@Test

 void detectsValuesThatStillMatchTheBundledDefaults() {



 ConfigKey boolKey = required("gui.admin.enabled");



 ConfigKey intKey = required("limits.daily-limit");



 ConfigKey choiceKey = required("reports.priority.repeat-offender-level");




assertTrue(ConfigGuiSupport.isDefault(boolKey, DEFAULTS, DEFAULTS));



 assertFalse(ConfigGuiSupport.isDefault(boolKey, CHANGED, DEFAULTS));




assertTrue(ConfigGuiSupport.isDefault(intKey, DEFAULTS, DEFAULTS));



 assertFalse(ConfigGuiSupport.isDefault(intKey, CHANGED, DEFAULTS));




assertTrue(ConfigGuiSupport.isDefault(choiceKey, DEFAULTS, DEFAULTS));



 assertFalse(ConfigGuiSupport.isDefault(choiceKey, CHANGED, DEFAULTS));




assertFalse(ConfigGuiSupport.isDefault(boolKey, null, DEFAULTS));



 assertFalse(ConfigGuiSupport.isDefault(null, DEFAULTS, DEFAULTS));

 }


@Test

 void countsModifiedKeysInASection() {



 List<ConfigKey> limits = ConfigKeys.inSection("limits");



 assertEquals(0, ConfigGuiSupport.modifiedCount(limits, DEFAULTS, DEFAULTS));



 assertEquals(1, ConfigGuiSupport.modifiedCount(limits, CHANGED, DEFAULTS));



 assertEquals(0, ConfigGuiSupport.modifiedCount(List.of(), CHANGED, DEFAULTS));

 }


@Test

 void booleansToggleWithoutTyping() {



 ConfigKey key = required("gui.admin.enabled");




assertEquals(Optional.of("false"), ConfigGuiSupport.quickChange(key, DEFAULTS));



 assertEquals(Optional.of("true"), ConfigGuiSupport.quickChange(key, CHANGED));

 }


@Test

 void choicesCycleAndWrapAround() {



 ConfigKey key = required("reports.priority.repeat-offender-level");




// Default is high, so the next choice is urgent.



 assertEquals(Optional.of("urgent"), ConfigGuiSupport.quickChange(key, DEFAULTS));



 assertEquals(Optional.of("low"), ConfigGuiSupport.nextChoice(key, "urgent"));



 assertEquals(Optional.of("low"), ConfigGuiSupport.nextChoice(key, "unknown"));




ConfigKey noChoices = required("limits.daily-limit");



 assertEquals(Optional.empty(), ConfigGuiSupport.nextChoice(noChoices, 5));

 }


@Test

 void numericKeysUseTheChatPromptInsteadOfCycle() {



 ConfigKey integerKey = required("limits.daily-limit");



 ConfigKey stringKey = required("server-name");




assertEquals(Optional.empty(), ConfigGuiSupport.quickChange(integerKey, DEFAULTS));



 assertEquals(Optional.empty(), ConfigGuiSupport.quickChange(stringKey, DEFAULTS));



 assertEquals(Optional.empty(), ConfigGuiSupport.quickChange(null, DEFAULTS));

 }


@Test

 void numericAdjustmentRespectsTheKeyKind() {



 ConfigKey integerKey = required("limits.daily-limit");



 ConfigKey longKey = required("limits.command-cooldown-millis");



 ConfigKey boolKey = required("gui.admin.enabled");




int current = (Integer) integerKey.current(DEFAULTS);



 assertEquals(Optional.of(String.valueOf(current + 10)),





 ConfigGuiSupport.adjustedValue(integerKey, DEFAULTS, 10));



 assertEquals(Optional.of(String.valueOf(current - 1)),





 ConfigGuiSupport.adjustedValue(integerKey, DEFAULTS, -1));




long cooldown = (Long) longKey.current(DEFAULTS);



 assertEquals(Optional.of(String.valueOf(cooldown + 100)),





 ConfigGuiSupport.adjustedValue(longKey, DEFAULTS, 100));




assertEquals(Optional.empty(), ConfigGuiSupport.adjustedValue(boolKey, DEFAULTS, 1));



 assertEquals(Optional.empty(), ConfigGuiSupport.adjustedValue(null, DEFAULTS, 1));

 }


@Test

 void adjustmentStepsAreAscending() {



 long[] steps = ConfigGuiSupport.adjustSteps(required("limits.daily-limit"));




assertEquals(3, steps.length);



 assertEquals(1L, steps[0]);



 assertEquals(10L, steps[1]);



 assertEquals(100L, steps[2]);

 }


@Test

 void searchMatchesPathAndValueCaseInsensitively() {



 ConfigKey key = required("limits.daily-limit");




assertTrue(ConfigGuiSupport.matchesQuery(key, DEFAULTS, "daily"));



 assertTrue(ConfigGuiSupport.matchesQuery(key, DEFAULTS, "LIMITS.DAILY"));



 assertTrue(ConfigGuiSupport.matchesQuery(key, DEFAULTS, " "));



 assertTrue(ConfigGuiSupport.matchesQuery(key, DEFAULTS, null));



 assertTrue(ConfigGuiSupport.matchesQuery(key, DEFAULTS, String.valueOf(





 key.current(DEFAULTS))));




assertFalse(ConfigGuiSupport.matchesQuery(key, DEFAULTS, "not-a-setting"));



 assertFalse(ConfigGuiSupport.matchesQuery(null, DEFAULTS, "limits"));

 }


@Test

 void searchMatchesLocalizedLabelAndDescription() {



 ConfigKey key = required("limits.daily-limit");




assertTrue(ConfigGuiSupport.matchesQuery(key, DEFAULTS, "每日举报上限",





 "每日举报上限", "每名玩家每天最多可以提交的举报数量。"));



 assertTrue(ConfigGuiSupport.matchesQuery(key, DEFAULTS, "DAILY REPORT LIMIT",





 "Daily Report Limit", "Maximum reports one player may submit per day."));



 assertTrue(ConfigGuiSupport.matchesQuery(key, DEFAULTS, "每天最多",





 "每日举报上限", "每名玩家每天最多可以提交的举报数量。"));




assertFalse(ConfigGuiSupport.matchesQuery(key, DEFAULTS, "完全无关",





 "每日举报上限", "每名玩家每天最多可以提交的举报数量。"));

 }


private static ConfigKey required(String path) {



 return ConfigKeys.find(path).orElseThrow(() -> new AssertionError("missing key " + path));

 } }
