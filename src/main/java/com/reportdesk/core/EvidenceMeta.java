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
/**
* Small embedded summary of the evidence file so list/detail GUIs never need
* to touch the disk. The complete snapshot lives in its own JSON file.
*/ public record EvidenceMeta(

 boolean present,

 long capturedAt,

 String world,

 double x,

 double y,

 double z,

 int inventoryItems,

 int chatLines,

 int blockBreakSamples,

 int movementSamples ) {


public EvidenceMeta {



 world = world == null ? "" : world;

 }


public static EvidenceMeta none() {



 return new EvidenceMeta(false, 0L, "", 0, 0, 0, 0, 0, 0, 0);

 }


public static EvidenceMeta of(Evidence evidence) {



 if (evidence == null) {





 return none();



 }



 return new EvidenceMeta(





 true,





 evidence.capturedAt(),





 evidence.world(),





 evidence.x(),





 evidence.y(),





 evidence.z(),





 evidence.inventory().size(),





 evidence.chat().size(),





 evidence.blockBreaks().size(),





 evidence.movement().size()



 );

 }


public String shortSummary() {



 if (!present) {





 return "none";



 }



 return "items=" + inventoryItems + ",chat=" + chatLines





 + ",breaks=" + blockBreakSamples + ",moves=" + movementSamples;

 } }
