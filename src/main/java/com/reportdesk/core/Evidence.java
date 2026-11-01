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
import java.util.List; import java.util.UUID;
/**
* Complete evidence snapshot. It is written to its own JSON file on the storage
* executor; the main thread only builds the immutable instance.
*/ public record Evidence(

 int schema,

 long reportId,

 long capturedAt,

 UUID targetId,

 String targetName,

 boolean targetOnline,

 String world,

 double x,

 double y,

 double z,

 float yaw,

 float pitch,

 String gameMode,

 double health,

 int foodLevel,

 int expLevel,

 int ping,

 List<String> inventory,

 List<String> armor,

 List<ChatSample> chat,

 List<BreakSample> blockBreaks,

 List<MoveSample> movement,

 String server ) {


public Evidence {



 targetName = targetName == null ? "" : targetName;



 world = world == null ? "" : world;



 gameMode = gameMode == null ? "" : gameMode;



 inventory = inventory == null ? List.of() : List.copyOf(inventory);



 armor = armor == null ? List.of() : List.copyOf(armor);



 chat = chat == null ? List.of() : List.copyOf(chat);



 blockBreaks = blockBreaks == null ? List.of() : List.copyOf(blockBreaks);



 movement = movement == null ? List.of() : List.copyOf(movement);



 server = server == null ? "" : server;

 } }
