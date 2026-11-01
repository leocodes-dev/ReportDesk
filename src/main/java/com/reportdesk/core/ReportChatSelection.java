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
* One chat line the reporter explicitly selected when filing a chat violation.
*
* <p>The snapshot fields mirror {@link ChatSample}; they are persisted with the
* report so staff can still see exactly what was highlighted even after the
* bounded live chat buffer has rotated.</p>
*/ public record ReportChatSelection(long createdAt, String playerName, String message) {


public ReportChatSelection {



 playerName = playerName == null ? "" : playerName;



 message = message == null ? "" : message;

 }


public static ReportChatSelection of(ChatSample sample) {



 if (sample == null) {





 return new ReportChatSelection(0L, "", "");



 }



 return new ReportChatSelection(sample.createdAt(), sample.playerName(), sample.message());

 } }
