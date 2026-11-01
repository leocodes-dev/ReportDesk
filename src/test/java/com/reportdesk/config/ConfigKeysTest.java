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
import com.reportdesk.core.ReportPriority; import org.junit.jupiter.api.Test;
import java.util.HashSet; import java.util.List; import java.util.Map; import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals; import static org.junit.jupiter.api.Assertions.assertFalse; import static org.junit.jupiter.api.Assertions.assertNotNull; import static org.junit.jupiter.api.Assertions.assertThrows; import static org.junit.jupiter.api.Assertions.assertTrue;
class ConfigKeysTest {


private static final PluginConfig DEFAULTS =



 new ConfigLoader(ignored -> { }).load(new MapConfigSource(Map.of()));


@Test

 void pathsAreUniqueAndReadableOnTheDefaultConfig() {



 Set<String> seen = new HashSet<>();



 for (ConfigKey key : ConfigKeys.all()) {





 assertTrue(seen.add(key.path()), () -> "duplicate path " + key.path());





 assertFalse(key.path().isBlank());





 assertFalse(key.path().startsWith("."));





 assertFalse(key.path().endsWith("."));





 assertNotNull(key.current(DEFAULTS), () -> "no default for " + key.path());





 assertTrue(ConfigKeys.find(key.path()).isPresent());



 }

 }


@Test

 void lookupIsCaseInsensitiveAndUnknownPathsAreEmpty() {



 assertTrue(ConfigKeys.find("LIMITS.DAILY-LIMIT").isPresent());



 assertTrue(ConfigKeys.find(" limits.daily-limit ").isPresent());



 assertTrue(ConfigKeys.find("limits.not-a-setting").isEmpty());



 assertTrue(ConfigKeys.find(null).isEmpty());

 }


@Test

 void parsesBooleansLeniently() {



 ConfigKey key = required("config-editor.enabled");




assertEquals(true, key.parse("true"));



 assertEquals(true, key.parse("ON"));



 assertEquals(true, key.parse("yes"));



 assertEquals(true, key.parse("1"));



 assertEquals(false, key.parse("false"));



 assertEquals(false, key.parse("off"));



 assertEquals(false, key.parse("0"));



 assertThrows(IllegalArgumentException.class, () -> key.parse("maybe"));



 assertThrows(IllegalArgumentException.class, () -> key.parse(""));

 }


@Test

 void parsesIntegersAndRejectsOverflow() {



 ConfigKey key = required("limits.daily-limit");




assertEquals(5, key.parse(" 5 "));



 assertEquals(-1, key.parse("-1"));



 assertThrows(IllegalArgumentException.class, () -> key.parse("5.5"));



 assertThrows(IllegalArgumentException.class, () -> key.parse("abc"));



 assertThrows(IllegalArgumentException.class, () -> key.parse("99999999999"));

 }


@Test

 void parsesLongs() {



 ConfigKey key = required("limits.command-cooldown-millis");




assertEquals(1_000L, key.parse("1000"));



 assertEquals(ConfigKey.Kind.LONG, key.kind());

 }


@Test

 void choicesAreNormalizedAndValidated() {



 ConfigKey key = required("reports.priority.repeat-offender-level");




assertEquals("high", key.parse("HIGH"));



 assertEquals("urgent", key.parse("Urgent"));



 assertThrows(IllegalArgumentException.class, () -> key.parse("critical"));



 assertEquals(ReportPriority.HIGH.id(), key.current(DEFAULTS));

 }


@Test

 void stringsTrimUnquoteAndRejectNewlines() {



 ConfigKey key = required("server-name");




assertEquals("My Server", key.parse(" 'My Server' "));



 assertEquals("My Server", key.parse("\"My Server\""));



 assertEquals("", key.parse("''"));



 assertThrows(IllegalArgumentException.class, () -> key.parse("line one\nline two"));

 }


@Test

 void matchesUsesTypedComparison() {



 ConfigKey intKey = required("limits.daily-limit");



 assertTrue(intKey.matches(5L, 5));



 assertFalse(intKey.matches(5, 6));




ConfigKey boolKey = required("config-editor.enabled");



 assertTrue(boolKey.matches(true, true));



 assertFalse(boolKey.matches(true, false));

 }


@Test

 void storageSettingsAreMarkedRestartRequired() {



 assertTrue(required("storage.directory").restartRequired());



 assertTrue(required("storage.max-backups").restartRequired());



 assertFalse(required("limits.daily-limit").restartRequired());

 }


@Test

 void sectionsGroupEveryKeyAndExposeAdminGuiSwitches() {



 List<String> sections = ConfigKeys.sections();



 assertTrue(sections.contains("gui"));



 assertTrue(sections.contains("limits"));



 assertEquals(sections.size(), sections.stream().distinct().count());




for (String section : sections) {





 assertFalse(ConfigKeys.inSection(section).isEmpty(), () -> "empty section " + section);



 }



 assertTrue(ConfigKeys.inSection("gui").stream()





 .anyMatch(key -> key.path().equals("gui.admin.enabled")));



 assertTrue(ConfigKeys.inSection("gui").stream()





 .anyMatch(key -> key.path().equals("gui.admin.confirm-destructive")));



 assertEquals(true, required("gui.admin.enabled").current(DEFAULTS));



 assertEquals(true, required("gui.admin.confirm-destructive").current(DEFAULTS));



 assertTrue(ConfigKeys.inSection("missing").isEmpty());



 assertTrue(ConfigKeys.inSection("").isEmpty());

 }


private static ConfigKey required(String path) {



 return ConfigKeys.find(path).orElseThrow(() -> new AssertionError("missing key " + path));

 } }
