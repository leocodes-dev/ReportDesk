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
import java.util.Locale;
/**
* How a report was closed. {@link #INSUFFICIENT} is stored as a
* {@link ReportStatus#REJECTED} report with a distinct verdict label.
*/ public enum Verdict {

 RESOLVED(ReportStatus.RESOLVED),

 REJECTED(ReportStatus.REJECTED),

 DUPLICATE(ReportStatus.DUPLICATE),

 INSUFFICIENT(ReportStatus.REJECTED),

 AUTO_CLOSED(ReportStatus.AUTO_CLOSED);


private final ReportStatus status;


Verdict(ReportStatus status) {



 this.status = status;

 }


public ReportStatus status() {



 return status;

 }


public String messageKey() {



 return "verdict." + name().toLowerCase(Locale.ROOT).replace('_', '-');

 }


public static Verdict parse(String value) {



 if (value == null) {





 return null;



 }



 String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');



 return switch (normalized) {





 case "RESOLVED", "RESOLVE", "ACCEPT", "ACCEPTED", "TRUE", "成立" -> RESOLVED;





 case "REJECTED", "REJECT", "FALSE", "不成立" -> REJECTED;





 case "DUPLICATE", "DUP", "重复" -> DUPLICATE;





 case "INSUFFICIENT", "EVIDENCE_INSUFFICIENT", "证据不足" -> INSUFFICIENT;





 case "AUTO_CLOSED", "AUTOCLOSED", "自动关闭" -> AUTO_CLOSED;





 default -> null;



 };

 } }
