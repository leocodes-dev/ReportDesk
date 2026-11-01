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
import com.reportdesk.i18n.Messages; import org.junit.jupiter.api.Test;
import java.io.InputStream; import java.util.ArrayList; import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals; import static org.junit.jupiter.api.Assertions.assertFalse; import static org.junit.jupiter.api.Assertions.assertTrue;
class ConfigLabelsTest {


@Test

 void everyConfigKeyAndSectionHasLocalizedText() {



 for (String language : List.of("zh_CN", "en_US")) {





 Messages messages = loadBundled(language);





 List<String> missing = new ArrayList<>();






for (ConfigKey key : ConfigKeys.all()) {







 String label = ConfigLabels.label(messages, key.path());







 String description = ConfigLabels.description(messages, key.path());







 if (label.equals(key.path()) || label.isBlank()) {









 missing.add(language + " label: " + key.path());







 }







 if (description.isBlank()) {









 missing.add(language + " description: " + key.path());







 }





 }






for (String section : ConfigKeys.sections()) {







 String label = ConfigLabels.sectionLabel(messages, section);







 String description = ConfigLabels.sectionDescription(messages, section);







 if (label.equals(section) || label.isBlank()) {









 missing.add(language + " section label: " + section);







 }







 if (description.isBlank()) {









 missing.add(language + " section description: " + section);







 }





 }






for (ConfigKey.Kind kind : ConfigKey.Kind.values()) {







 String typeName = ConfigLabels.typeName(messages, kind);







 if (typeName.equals(kind.name().toLowerCase(java.util.Locale.ROOT))









 || typeName.isBlank()) {









 missing.add(language + " type name: " + kind);







 }





 }






assertTrue(missing.isEmpty(), () -> language + " is missing localized config text: "







 + missing);



 }

 }


@Test

 void missingTranslationsFallBackToTheRawPath() {



 Messages messages = Messages.empty();




assertEquals("limits.daily-limit",





 ConfigLabels.label(messages, "limits.daily-limit"));



 assertEquals("limits", ConfigLabels.sectionLabel(messages, "limits"));



 assertEquals("", ConfigLabels.description(messages, "limits.daily-limit"));



 assertEquals("", ConfigLabels.sectionDescription(messages, "limits"));



 assertEquals("integer", ConfigLabels.typeName(messages, ConfigKey.Kind.INTEGER));

 }


@Test

 void labelKeysMatchTheFlattenedLanguageLayout() {



 assertEquals("gui.admin.config-label.limits.daily-limit",





 ConfigLabels.labelKey("limits.daily-limit"));



 assertEquals("gui.admin.config-desc.limits.daily-limit",





 ConfigLabels.descriptionKey("limits.daily-limit"));



 assertEquals("gui.admin.section.limits", ConfigLabels.sectionLabelKey("limits"));



 assertEquals("gui.admin.section-desc.limits",





 ConfigLabels.sectionDescriptionKey("limits"));



 assertEquals("gui.admin.config-type-name.integer",





 ConfigLabels.typeNameKey(ConfigKey.Kind.INTEGER));

 }


private static Messages loadBundled(String language) {



 try (InputStream input = ConfigLabelsTest.class.getResourceAsStream(





 "/lang/" + language + ".yml")) {





 assertFalse(input == null, "missing bundled language file " + language);





 return Messages.fromStream(input, language);



 } catch (java.io.IOException exception) {





 throw new AssertionError(exception);



 }

 } }
