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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ChatSelectionSupportTest {

  private static final ChatSample EARLY = new ChatSample(1_000L, "Bob", "first line");

  private static final ChatSample MIDDLE = new ChatSample(2_000L, "bob", "second line");

  private static final ChatSample LATE = new ChatSample(3_000L, "Charlie", "other player");

  @Test
  void fromSamplesKeepsSelectedIndicesInOriginalOrder() {

    List<ChatSample> samples = List.of(EARLY, MIDDLE, LATE);

    List<ReportChatSelection> selections = ChatSelectionSupport.fromSamples(samples, Set.of(2, 0));

    assertEquals(2, selections.size());

    assertEquals(ReportChatSelection.of(EARLY), selections.get(0));

    assertEquals(ReportChatSelection.of(LATE), selections.get(1));
  }

  @Test
  void fromSamplesIgnoresInvalidInput() {

    assertTrue(ChatSelectionSupport.fromSamples(null, Set.of(0)).isEmpty());

    assertTrue(ChatSelectionSupport.fromSamples(List.of(), Set.of(0)).isEmpty());

    assertTrue(ChatSelectionSupport.fromSamples(List.of(EARLY), Set.of()).isEmpty());

    assertTrue(ChatSelectionSupport.fromSamples(List.of(EARLY), null).isEmpty());
  }

  @Test
  void matchingRequiresExactTimestampAndMessageButIgnoresNameCase() {

    ReportChatSelection selection = new ReportChatSelection(1_000L, "BOB", "first line");

    assertTrue(ChatSelectionSupport.matches(selection, EARLY));

    assertFalse(
        ChatSelectionSupport.matches(selection, new ChatSample(1_001L, "Bob", "first line")));

    assertFalse(
        ChatSelectionSupport.matches(selection, new ChatSample(1_000L, "Bob", "first line!")));

    assertFalse(ChatSelectionSupport.matches(null, EARLY));

    assertFalse(ChatSelectionSupport.matches(selection, null));
  }

  @Test
  void isSelectedFindsAnyMatchingSample() {

    List<ReportChatSelection> selections = List.of(ReportChatSelection.of(MIDDLE));

    assertTrue(ChatSelectionSupport.isSelected(selections, MIDDLE));

    assertFalse(ChatSelectionSupport.isSelected(selections, EARLY));

    assertFalse(ChatSelectionSupport.isSelected(List.of(), EARLY));

    assertFalse(ChatSelectionSupport.isSelected(null, EARLY));
  }

  @Test
  void chronologicalFiltersByPlayerAndSortsOldestFirst() {

    List<ChatSample> unsorted = List.of(LATE, MIDDLE, EARLY);

    List<ChatSample> bobOnly = ChatSelectionSupport.chronological(unsorted, " bOb ");

    assertEquals(List.of(EARLY, MIDDLE), bobOnly);

    assertEquals(3, ChatSelectionSupport.chronological(unsorted, null).size());

    assertEquals(3, ChatSelectionSupport.chronological(unsorted, " ").size());

    assertTrue(ChatSelectionSupport.chronological(List.of(), "Bob").isEmpty());

    assertTrue(ChatSelectionSupport.chronological(null, "Bob").isEmpty());
  }
}
