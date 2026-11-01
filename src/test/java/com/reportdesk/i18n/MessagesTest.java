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

package com.reportdesk.i18n;
import org.junit.jupiter.api.Test;
import java.io.InputStream; import java.lang.reflect.Field; import java.lang.reflect.Modifier; import java.time.Instant; import java.time.ZoneOffset; import java.util.ArrayList; import java.util.List; import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals; import static org.junit.jupiter.api.Assertions.assertFalse; import static org.junit.jupiter.api.Assertions.assertTrue;
class MessagesTest {


@Test

 void nestedMapsAreFlattenedAndReadable() {



 Messages messages = Messages.fromMap(Map.of(





 "general", Map.of("none", "<gray>none</gray>", "invalid-input", "<red>Invalid: %reason%</red>"),





 "lines", List.of("<red>one</red>", "<green>two</green>")



 ), "en_US");




assertEquals("en_US", messages.language());



 assertTrue(messages.has("general.none"));



 assertEquals("<gray>none</gray>", messages.raw("general.none"));



 assertEquals("missing.key", messages.raw("missing.key"));



 assertEquals(2, messages.rawList("lines").size());



 assertEquals(List.of("<gray>none</gray>"), messages.rawList("general.none"));



 assertTrue(messages.rawList("missing").isEmpty());

 }


@Test

 void placeholdersAreInsertedUnparsed() {



 Messages messages = Messages.fromMap(Map.of(





 "greet", "<red>Hello %name%!</red>",





 "reason", "<red>Invalid: %reason%</red>"



 ), "en_US");




String greet = messages.plain("greet", Map.of("name", "<bold>Bob</bold>"));



 assertEquals("Hello <bold>Bob</bold>!", greet);




String reason = messages.plain("reason", Map.of("reason", "<click:run_command:/op>click</click>"));



 assertEquals("Invalid: <click:run_command:/op>click</click>", reason);

 }


@Test

 void prefixedOutputCombinesPrefixAndBody() {



 Messages messages = Messages.fromMap(Map.of(





 "prefix", "<gray>[RD]</gray> ",





 "body", "<white>ok</white>"



 ), "en_US");




assertEquals("[RD] ok", messages.plain(messages.prefixed("body", Map.of())));

 }


@Test

 void componentListsRenderEachLine() {



 Messages messages = Messages.fromMap(Map.of(





 "lore", List.of("<red>first</red>", "<green>second</green>")



 ), "en_US");




assertEquals(2, messages.components("lore").size());



 assertEquals("first", messages.plain(messages.components("lore").get(0)));



 assertEquals(1, messages.components("missing").size());



 assertEquals("missing", messages.plain(messages.components("missing").get(0)));

 }


@Test

 void dateFormatterFallsBackWhenPatternIsInvalid() {



 Messages invalid = Messages.fromMap(Map.of("time-format", Map.of("date", "not-a-pattern-%%%")),





 "en_US");



 assertEquals("2026-01-02 03:04",





 invalid.dateFormatter().format(Instant.parse("2026-01-02T03:04:05Z").atZone(ZoneOffset.UTC)));




Messages custom = Messages.fromMap(Map.of("time-format", Map.of("date", "yyyy/MM/dd")), "en_US");



 assertEquals("2026/01/02",





 custom.dateFormatter().format(Instant.parse("2026-01-02T03:04:05Z").atZone(ZoneOffset.UTC)));

 }


@Test

 void emptyMessagesReturnKeys() {



 Messages messages = Messages.empty();



 assertEquals("some.key", messages.raw("some.key"));



 assertEquals("some.key", messages.plain("some.key"));



 assertFalse(messages.has("some.key"));

 }


@Test

 void oldLanguageFilesAreFilledFromBundledDefaults(@org.junit.jupiter.api.io.TempDir


























 java.nio.file.Path temp) throws Exception {



 java.nio.file.Path disk = temp.resolve("zh_CN.yml");



 java.nio.file.Files.writeString(disk, """
prefix: '<gray>[custom]</gray> '
config:
  applied: '<green>custom applied</green>'
""", java.nio.charset.StandardCharsets.UTF_8);




Messages messages = Messages.load(disk, null, ignored -> { });




assertTrue(messages.plain("prefix").contains("[custom]"));



 assertEquals("custom applied", messages.plain("config.applied"));



 assertTrue(messages.has("config.rejected"));



 assertFalse("config.rejected".equals(messages.plain("config.rejected")));

 }


@Test

 void bundledLanguageFilesContainEveryMessageKey() throws Exception {



 List<String> keys = messageKeys();



 assertFalse(keys.isEmpty());




for (String language : List.of("zh_CN", "en_US")) {





 Messages messages = loadBundled(language);





 List<String> missing = new ArrayList<>();





 for (String key : keys) {







 if (!messages.has(key)) {









 missing.add(key);







 }





 }





 assertTrue(missing.isEmpty(), () -> language + " is missing keys: " + missing);



 }

 }


private static Messages loadBundled(String language) {



 try (InputStream input = MessagesTest.class.getResourceAsStream("/lang/" + language + ".yml")) {





 assertTrue(input != null, "missing bundled language file " + language);





 return Messages.fromStream(input, language);



 } catch (java.io.IOException exception) {





 throw new AssertionError(exception);



 }

 }


private static List<String> messageKeys() throws IllegalAccessException {



 List<String> keys = new ArrayList<>();



 for (Field field : MessageKeys.class.getDeclaredFields()) {





 if (Modifier.isStatic(field.getModifiers()) && field.getType() == String.class) {







 keys.add((String) field.get(null));





 }



 }



 return keys;

 } }
