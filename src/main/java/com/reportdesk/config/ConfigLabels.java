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
import com.reportdesk.i18n.Messages;
import java.util.Locale;
/**
* Localized display names for the admin config editor.
*
* <p>Config paths are stable identifiers for server owners who edit
* {@code config.yml} by hand, but they are poor labels in a GUI. Every editable
* path and every section therefore resolves to a label and a one-line
* explanation from the language files. The raw path is still shown as a lore
* reference so administrators can map the GUI entry back to the file.</p>
*/ public final class ConfigLabels {


/** Prefix of the flattened {@code gui.admin.config-label.*} message keys. */

 public static final String LABEL_PREFIX = "gui.admin.config-label.";


/** Prefix of the flattened {@code gui.admin.config-desc.*} message keys. */

 public static final String DESCRIPTION_PREFIX = "gui.admin.config-desc.";


/** Prefix of the section label message keys. */

 public static final String SECTION_LABEL_PREFIX = "gui.admin.section.";


/** Prefix of the section description message keys. */

 public static final String SECTION_DESCRIPTION_PREFIX = "gui.admin.section-desc.";


/** Prefix of the localized type names shown in the config lore. */

 public static final String TYPE_NAME_PREFIX = "gui.admin.config-type-name.";


private ConfigLabels() {

 }


public static String labelKey(String path) {



 return key(LABEL_PREFIX, path);

 }


public static String descriptionKey(String path) {



 return key(DESCRIPTION_PREFIX, path);

 }


public static String sectionLabelKey(String section) {



 return key(SECTION_LABEL_PREFIX, section);

 }


public static String sectionDescriptionKey(String section) {



 return key(SECTION_DESCRIPTION_PREFIX, section);

 }


public static String typeNameKey(ConfigKey.Kind kind) {



 return key(TYPE_NAME_PREFIX, kind == null ? "" : kind.name());

 }


/** Human readable label, falling back to the raw path when a translation is missing. */

 public static String label(Messages messages, String path) {



 String key = labelKey(path);



 if (messages != null && messages.has(key)) {





 String label = messages.plain(key);





 if (!label.isBlank()) {







 return label;





 }



 }



 String normalized = normalize(path);



 return normalized.isEmpty() ? "-" : normalized;

 }


/** One-line explanation, or an empty string when the language file has none. */

 public static String description(Messages messages, String path) {



 String key = descriptionKey(path);



 return messages != null && messages.has(key) ? messages.plain(key) : "";

 }


public static String sectionLabel(Messages messages, String section) {



 String key = sectionLabelKey(section);



 if (messages != null && messages.has(key)) {





 String label = messages.plain(key);





 if (!label.isBlank()) {







 return label;





 }



 }



 String normalized = normalize(section);



 return normalized.isEmpty() ? "-" : normalized;

 }


public static String sectionDescription(Messages messages, String section) {



 String key = sectionDescriptionKey(section);



 return messages != null && messages.has(key) ? messages.plain(key) : "";

 }


/** Plain-language type name, for example "布尔开关" or "整数". */

 public static String typeName(Messages messages, ConfigKey.Kind kind) {



 String key = typeNameKey(kind);



 if (messages != null && messages.has(key)) {





 String name = messages.plain(key);





 if (!name.isBlank()) {







 return name;





 }



 }



 return kind == null ? "-" : kind.name().toLowerCase(Locale.ROOT);

 }


private static String key(String prefix, String value) {



 String normalized = normalize(value);



 return normalized.isEmpty() ? prefix : prefix + normalized;

 }


private static String normalize(String value) {



 return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);

 } }
