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

package com.reportdesk.util;
import com.reportdesk.i18n.MessageKeys; import com.reportdesk.i18n.Messages; import net.kyori.adventure.text.Component;
import java.time.Instant; import java.time.ZoneId; import java.util.Map; import java.util.concurrent.TimeUnit;
/**
* Human readable timestamps for chat output and GUI lore.
*/ public final class TimeText {


private TimeText() {

 }


public static String absolute(long timestamp, Messages messages) {



 if (timestamp <= 0) {





 return "-";



 }



 return messages.dateFormatter().withZone(ZoneId.systemDefault())





 .format(Instant.ofEpochMilli(timestamp));

 }


public static Component relative(long timestamp, long now, Messages messages) {



 if (timestamp <= 0) {





 return messages.component(MessageKeys.GENERAL_NONE);



 }



 long elapsed = Math.max(0L, now - timestamp);



 long seconds = TimeUnit.MILLISECONDS.toSeconds(elapsed);



 if (seconds < 60) {





 if (seconds <= 5) {







 return messages.component(MessageKeys.TIME_NOW);





 }





 return messages.component(MessageKeys.TIME_SECONDS_AGO, Map.of("value", String.valueOf(seconds)));



 }



 long minutes = TimeUnit.SECONDS.toMinutes(seconds);



 if (minutes < 60) {





 return messages.component(MessageKeys.TIME_MINUTES_AGO, Map.of("value", String.valueOf(minutes)));



 }



 long hours = TimeUnit.MINUTES.toHours(minutes);



 if (hours < 24) {





 return messages.component(MessageKeys.TIME_HOURS_AGO, Map.of("value", String.valueOf(hours)));



 }



 long days = TimeUnit.HOURS.toDays(hours);



 return messages.component(MessageKeys.TIME_DAYS_AGO, Map.of("value", String.valueOf(days)));

 }


public static String duration(long millis, Messages messages) {



 if (millis <= 0) {





 return "-";



 }



 long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);



 long minutes = TimeUnit.SECONDS.toMinutes(seconds);



 long hours = TimeUnit.MINUTES.toHours(minutes);



 long days = TimeUnit.HOURS.toDays(hours);



 if (days > 0) {





 return days + "d " + (hours % 24) + "h";



 }



 if (hours > 0) {





 return hours + "h " + (minutes % 60) + "m";



 }



 if (minutes > 0) {





 return minutes + "m " + (seconds % 60) + "s";



 }



 return seconds + "s";

 } }
