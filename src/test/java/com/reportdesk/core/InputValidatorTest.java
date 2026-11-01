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
import org.junit.jupiter.api.Test;

class InputValidatorTest {

  private final InputValidator validator =
      new InputValidator(
          new InputValidator.Settings(
              2, 10, 20, true, false, List.of("badword"), "^[A-Za-z0-9_]{3,16}$"));

  @Test
  void detailLengthAndRequiredAreChecked() {

    assertTrue(validator.validateDetail("hello").valid());

    assertEquals(InputValidator.Reason.EMPTY, validator.validateDetail("").reason());

    assertEquals(InputValidator.Reason.TOO_SHORT, validator.validateDetail("a").reason());

    assertEquals(InputValidator.Reason.TOO_LONG, validator.validateDetail("12345678901").reason());
  }

  @Test
  void controlCharactersAndColorCodesAreRejected() {

    assertEquals(
        InputValidator.Reason.ILLEGAL_CHARACTERS, validator.validateDetail("hi\u0007x").reason());

    assertEquals(
        InputValidator.Reason.ILLEGAL_CHARACTERS, validator.validateDetail("hi\u00a7c").reason());
  }

  @Test
  void blockedWordsAreCaseInsensitive() {

    InputValidator.Result result = validator.validateDetail("BADWORD");

    assertFalse(result.valid());

    assertEquals(InputValidator.Reason.BLOCKED_WORD, result.reason());

    assertEquals("badword", result.word());
  }

  @Test
  void playerNamesUseConfiguredPattern() {

    assertTrue(validator.validatePlayerName("Notch_123").valid());

    assertFalse(validator.validatePlayerName("ab").valid());

    assertFalse(validator.validatePlayerName("bad name").valid());

    assertFalse(validator.validatePlayerName("a".repeat(17)).valid());

    assertEquals("Notch", validator.normalizeName(" Notch "));
  }

  @Test
  void notesAndQueriesHaveTheirOwnLimits() {

    assertTrue(validator.validateNote("ok").valid());

    assertFalse(validator.validateNote("x".repeat(21)).valid());

    assertTrue(validator.validateQuery("").valid());

    assertTrue(validator.validateQuery("search").valid());
  }

  @Test
  void colorCodesCanBeAllowedExplicitly() {

    InputValidator permissive =
        new InputValidator(
            new InputValidator.Settings(
                0, 100, 100, false, true, List.of(), "^[A-Za-z0-9_]{3,16}$"));

    assertTrue(permissive.validateDetail("hello \u00a7agreen").valid());
  }
}
