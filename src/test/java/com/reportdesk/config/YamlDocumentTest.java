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
import org.junit.jupiter.api.Test;
import java.io.IOException; import java.io.InputStream; import java.nio.charset.StandardCharsets; import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals; import static org.junit.jupiter.api.Assertions.assertFalse; import static org.junit.jupiter.api.Assertions.assertNotNull; import static org.junit.jupiter.api.Assertions.assertThrows; import static org.junit.jupiter.api.Assertions.assertTrue;
class YamlDocumentTest {


private static final String SAMPLE = """
# top comment
name: 'demo'
limits:
  # daily cap
  daily-limit: 10 # inline
  cooldown-seconds: 60
reports:
  priority:
    enabled: true
    sla-hours:
      high: 6
""";


@Test

 void replacesValueAndKeepsComments() {



 String updated = YamlDocument.set(SAMPLE, "limits.daily-limit", 5);




assertTrue(updated.contains("# top comment"));



 assertTrue(updated.contains("# daily cap"));



 assertTrue(updated.contains("daily-limit: 5 # inline"));



 Map<String, Object> parsed = YamlDocument.parse(updated);



 assertEquals(5, YamlDocument.valueAt(parsed, "limits.daily-limit"));



 assertEquals(60, YamlDocument.valueAt(parsed, "limits.cooldown-seconds"));



 assertEquals("demo", YamlDocument.valueAt(parsed, "name"));

 }


@Test

 void quotesStringsAndRoundTripsSpecialCharacters() {



 String value = "He said 'hi' # now";




String updated = YamlDocument.set(SAMPLE, "name", value);




assertEquals(value, YamlDocument.valueAt(YamlDocument.parse(updated), "name"));



 assertTrue(updated.contains("name: 'He said ''hi'' # now'"));

 }


@Test

 void insertsMissingKeyUnderParentSection() {



 String withoutKey = SAMPLE.replace("  cooldown-seconds: 60\n", "");




String updated = YamlDocument.set(withoutKey, "limits.cooldown-seconds", 90);




assertEquals(90, YamlDocument.valueAt(YamlDocument.parse(updated),





 "limits.cooldown-seconds"));



 assertEquals(1, countLinesStartingWith(updated, "limits:"));

 }


@Test

 void createsMissingSectionAtEndOfFile() {



 String updated = YamlDocument.set(SAMPLE, "audit.page-size", 7);




assertEquals(7, YamlDocument.valueAt(YamlDocument.parse(updated), "audit.page-size"));



 assertTrue(updated.contains("audit:\n  page-size: 7"));



 assertTrue(updated.endsWith("\n"));

 }


@Test

 void createsDeepMissingSections() {



 String updated = YamlDocument.set(SAMPLE, "notifications.webhook.events.created", false);




assertEquals(Boolean.FALSE,





 YamlDocument.valueAt(YamlDocument.parse(updated),







 "notifications.webhook.events.created"));

 }


@Test

 void updatesNestedKeyWithoutTouchingSiblings() {



 String updated = YamlDocument.set(SAMPLE, "reports.priority.sla-hours.high", 4);




Map<String, Object> parsed = YamlDocument.parse(updated);



 assertEquals(4, YamlDocument.valueAt(parsed, "reports.priority.sla-hours.high"));



 assertEquals(true, YamlDocument.valueAt(parsed, "reports.priority.enabled"));

 }


@Test

 void preservesWindowsLineEndings() {



 String windows = SAMPLE.replace("\n", "\r\n");




String updated = YamlDocument.set(windows, "limits.daily-limit", 3);




assertTrue(updated.contains("\r\n"));



 assertFalse(updated.replace("\r\n", "").contains("\n"));



 assertEquals(3, YamlDocument.valueAt(YamlDocument.parse(updated), "limits.daily-limit"));

 }


@Test

 void rejectsInvalidYaml() {



 assertThrows(IllegalArgumentException.class, () -> YamlDocument.parse("limits: [unclosed"));



 assertThrows(IllegalArgumentException.class, () -> YamlDocument.parse("- just\n- a list"));



 assertThrows(IllegalArgumentException.class, () -> YamlDocument.set("limits: [unclosed",





 "limits.priority", 1));

 }


@Test

 void emptyDocumentParsesToEmptyMap() {



 assertTrue(YamlDocument.parse(null).isEmpty());



 assertTrue(YamlDocument.parse(" ").isEmpty());



 assertTrue(YamlDocument.parse("# only a comment").isEmpty());

 }


@Test

 void everyRegistryPathExistsInBundledConfig() throws IOException {



 Map<String, Object> parsed = YamlDocument.parse(bundledConfig());




for (ConfigKey key : ConfigKeys.all()) {





 assertNotNull(YamlDocument.valueAt(parsed, key.path()),







 () -> "missing bundled config key " + key.path());



 }

 }


@Test

 void bundledConfigCanRewriteEveryEditableKey() throws IOException {



 String text = bundledConfig();



 Map<String, Object> original = YamlDocument.parse(text);



 String updated = text;




for (ConfigKey key : ConfigKeys.all()) {





 Object value = YamlDocument.valueAt(original, key.path());





 updated = YamlDocument.set(updated, key.path(), value);



 }




Map<String, Object> reparsed = YamlDocument.parse(updated);



 for (ConfigKey key : ConfigKeys.all()) {





 assertNotNull(YamlDocument.valueAt(reparsed, key.path()), key.path());



 }

 }


private static int countLinesStartingWith(String text, String prefix) {



 int count = 0;



 for (String line : text.split("\r?\n")) {





 if (line.startsWith(prefix)) {







 count++;





 }



 }



 return count;

 }


private static String bundledConfig() throws IOException {



 try (InputStream input = YamlDocumentTest.class.getResourceAsStream("/config.yml")) {





 assertNotNull(input, "bundled config.yml");





 return new String(input.readAllBytes(), StandardCharsets.UTF_8);



 }

 } }
