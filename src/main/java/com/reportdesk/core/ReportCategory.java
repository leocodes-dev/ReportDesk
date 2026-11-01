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

package com.reportdesk.core;
import java.util.List; import java.util.Locale;
/**
* A configurable report category.
*
* @param priority display ordering (higher first)
* @param severity default triage priority for reports in this category
*/ public record ReportCategory(

 String id,

 String display,

 String icon,

 List<String> aliases,

 boolean enabled,

 int priority,

 List<String> description,

 ReportPriority severity ) {


public ReportCategory {



 id = id == null ? "other" : id.trim().toLowerCase(Locale.ROOT);



 display = display == null || display.isBlank() ? id : display;



 icon = icon == null || icon.isBlank() ? "PAPER" : icon.trim().toUpperCase(Locale.ROOT);



 aliases = aliases == null ? List.of() : List.copyOf(aliases);



 description = description == null ? List.of() : List.copyOf(description);



 severity = severity == null ? ReportPriority.NORMAL : severity;

 }


/**


* Backwards-compatible constructor for callers that do not set a severity.


*/

 public ReportCategory(String id, String display, String icon, List<String> aliases,












 boolean enabled, int priority, List<String> description) {



 this(id, display, icon, aliases, enabled, priority, description, ReportPriority.NORMAL);

 } }
