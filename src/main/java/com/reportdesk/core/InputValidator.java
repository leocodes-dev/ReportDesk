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
import java.util.List; import java.util.Locale; import java.util.regex.Pattern; import java.util.regex.PatternSyntaxException;
/**
* Input validation for player names, report details and staff notes.
*
* <p>User text is never parsed as MiniMessage: it is inserted through
* {@code Placeholder.unparsed} in the message layer. This validator additionally
* rejects control characters and, unless enabled, section-sign colour codes.</p>
*/ public final class InputValidator {


public record Settings(



 int detailMinLength,



 int detailMaxLength,



 int noteMaxLength,



 boolean detailRequired,



 boolean allowColorCodes,



 List<String> blockedWords,



 String playerNamePattern

 ) {




public Settings {





 detailMinLength = Math.max(0, detailMinLength);





 detailMaxLength = Math.max(1, detailMaxLength);





 noteMaxLength = Math.max(1, noteMaxLength);





 blockedWords = blockedWords == null ? List.of() : List.copyOf(blockedWords);





 playerNamePattern = playerNamePattern == null || playerNamePattern.isBlank()







 ? "^[A-Za-z0-9_]{3,16}$" : playerNamePattern;



 }

 }


public enum Reason {



 OK,



 EMPTY,



 TOO_SHORT,



 TOO_LONG,



 ILLEGAL_CHARACTERS,



 BLOCKED_WORD,



 INVALID_PLAYER_NAME

 }


public record Result(boolean valid, Reason reason, String word) {




public static Result ok() {





 return new Result(true, Reason.OK, "");



 }




public static Result error(Reason reason) {





 return new Result(false, reason, "");



 }




public static Result blocked(String word) {





 return new Result(false, Reason.BLOCKED_WORD, word == null ? "" : word);



 }

 }


private final Settings settings;

 private final Pattern namePattern;


public InputValidator(Settings settings) {



 this.settings = settings == null ? new Settings(0, 200, 500, false, false, List.of(), null) : settings;



 Pattern compiled;



 try {





 compiled = Pattern.compile(this.settings.playerNamePattern());



 } catch (PatternSyntaxException exception) {





 compiled = Pattern.compile("^[A-Za-z0-9_]{3,16}$");



 }



 this.namePattern = compiled;

 }


public Settings settings() {



 return settings;

 }


public Result validateDetail(String value) {



 return validate(value, settings.detailMinLength(), settings.detailMaxLength(), settings.detailRequired());

 }


public Result validateNote(String value) {



 return validate(value, 1, settings.noteMaxLength(), true);

 }


public Result validateReply(String value, int maxLength) {



 return validate(value, 1, Math.max(1, maxLength), true);

 }


public Result validateQuery(String value) {



 if (value == null || value.isBlank()) {





 return Result.ok();



 }



 return validate(value, 1, settings.noteMaxLength(), false);

 }


public Result validatePlayerName(String value) {



 if (value == null || value.isBlank()) {





 return Result.error(Reason.INVALID_PLAYER_NAME);



 }



 String trimmed = value.trim();



 Result base = validate(trimmed, 1, 64, true);



 if (!base.valid()) {





 return base;



 }



 if (!namePattern.matcher(trimmed).matches()) {





 return Result.error(Reason.INVALID_PLAYER_NAME);



 }



 return Result.ok();

 }


public String normalizeName(String value) {



 return value == null ? "" : value.trim();

 }


private Result validate(String value, int min, int max, boolean required) {



 if (value == null) {





 return required ? Result.error(Reason.EMPTY) : Result.ok();



 }



 String trimmed = value.trim();



 if (trimmed.isEmpty()) {





 return required ? Result.error(Reason.EMPTY) : Result.ok();



 }



 int length = trimmed.codePointCount(0, trimmed.length());



 if (length < min) {





 return Result.error(Reason.TOO_SHORT);



 }



 if (length > max) {





 return Result.error(Reason.TOO_LONG);



 }



 for (int index = 0; index < trimmed.length(); index++) {





 char character = trimmed.charAt(index);





 if (Character.isISOControl(character)) {







 return Result.error(Reason.ILLEGAL_CHARACTERS);





 }





 if (character == '\u00a7' && !settings.allowColorCodes()) {







 return Result.error(Reason.ILLEGAL_CHARACTERS);





 }



 }



 String lower = trimmed.toLowerCase(Locale.ROOT);



 for (String blocked : settings.blockedWords()) {





 if (blocked == null || blocked.isBlank()) {







 continue;





 }





 if (lower.contains(blocked.toLowerCase(Locale.ROOT))) {







 return Result.blocked(blocked);





 }



 }



 return Result.ok();

 } }
