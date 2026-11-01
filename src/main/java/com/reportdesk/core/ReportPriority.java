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

package com.reportdesk.core;
import java.util.Locale;
/**
* Triage priority of a report. The value is persisted with the report so the
* ordering does not change when configuration is edited later.
*/ public enum ReportPriority {

 LOW(0),

 NORMAL(1),

 HIGH(2),

 URGENT(3);


private final int weight;


ReportPriority(int weight) {



 this.weight = weight;

 }


public int weight() {



 return weight;

 }


public String id() {



 return name().toLowerCase(Locale.ROOT);

 }


public static ReportPriority parse(String value, ReportPriority fallback) {



 if (value == null || value.isBlank()) {





 return fallback;



 }



 try {





 return valueOf(value.trim().toUpperCase(Locale.ROOT));



 } catch (IllegalArgumentException exception) {





 return fallback;



 }

 } }
