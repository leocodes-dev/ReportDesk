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
import java.util.ArrayList; import java.util.Comparator; import java.util.List; import java.util.Locale; import java.util.Set;
/**
* Pure helpers shared by the reporter chat picker and the staff chat review
* page. No Bukkit types are used here so the matching rules stay unit testable.
*/ public final class ChatSelectionSupport {


private ChatSelectionSupport() {

 }


public static List<ReportChatSelection> fromSamples(List<ChatSample> samples, Set<Integer> indices) {



 if (samples == null || samples.isEmpty() || indices == null || indices.isEmpty()) {





 return List.of();



 }



 List<ReportChatSelection> selections = new ArrayList<>();



 for (int index = 0; index < samples.size(); index++) {





 if (indices.contains(index)) {







 ChatSample sample = samples.get(index);







 if (sample != null) {









 selections.add(ReportChatSelection.of(sample));







 }





 }



 }



 return List.copyOf(selections);

 }


public static boolean matches(ReportChatSelection selection, ChatSample sample) {



 if (selection == null || sample == null) {





 return false;



 }



 return selection.createdAt() == sample.createdAt()





 && selection.message().equals(sample.message())





 && selection.playerName().equalsIgnoreCase(sample.playerName());

 }


public static boolean isSelected(List<ReportChatSelection> selections, ChatSample sample) {



 if (selections == null || selections.isEmpty() || sample == null) {





 return false;



 }



 for (ReportChatSelection selection : selections) {





 if (matches(selection, sample)) {







 return true;





 }



 }



 return false;

 }


/** Returns a copy ordered oldest first, which reads naturally in a history view. */

 public static List<ChatSample> chronological(List<ChatSample> samples, String playerName) {



 if (samples == null || samples.isEmpty()) {





 return List.of();



 }



 String wanted = playerName == null ? "" : playerName.trim().toLowerCase(Locale.ROOT);



 List<ChatSample> filtered = new ArrayList<>();



 for (ChatSample sample : samples) {





 if (sample == null) {







 continue;





 }





 if (wanted.isEmpty() || sample.playerName().toLowerCase(Locale.ROOT).equals(wanted)) {







 filtered.add(sample);





 }



 }



 filtered.sort(Comparator.comparingLong(ChatSample::createdAt));



 return List.copyOf(filtered);

 } }
