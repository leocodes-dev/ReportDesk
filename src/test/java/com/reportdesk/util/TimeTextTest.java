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
import com.reportdesk.i18n.Messages; import org.junit.jupiter.api.Test;
import java.io.InputStream; import java.time.Instant; import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals; import static org.junit.jupiter.api.Assertions.assertNotNull; import static org.junit.jupiter.api.Assertions.assertTrue;
class TimeTextTest {


private final Messages messages = Messages.fromMap(Map.of(



 "time", Map.of(





 "now", "<green>now</green>",





 "seconds-ago", "<gray>%value%s ago</gray>",





 "minutes-ago", "<gray>%value%m ago</gray>",





 "hours-ago", "<gray>%value%h ago</gray>",





 "days-ago", "<gray>%value%d ago</gray>"



 ),



 "general", Map.of("none", "<gray>-</gray>"),



 "time-format", Map.of("date", "yyyy-MM-dd")

 ), "en_US");


@Test

 void absoluteTimestampUsesConfiguredFormatter() {



 long timestamp = Instant.parse("2026-01-02T03:04:05Z").toEpochMilli();



 assertEquals("2026-01-02", TimeText.absolute(timestamp, messages));



 assertEquals("-", TimeText.absolute(0L, messages));

 }


@Test

 void relativeDescribesSecondsMinutesHoursAndDays() {



 long now = 10L * 86_400_000L;



 assertEquals("now", messages.plain(TimeText.relative(now - 1_000L, now, messages)));



 assertEquals("10s ago", messages.plain(TimeText.relative(now - 10_000L, now, messages)));



 assertEquals("5m ago", messages.plain(TimeText.relative(now - 5L * 60_000L, now, messages)));



 assertEquals("3h ago", messages.plain(TimeText.relative(now - 3L * 3_600_000L, now, messages)));



 assertEquals("2d ago", messages.plain(TimeText.relative(now - 2L * 86_400_000L, now, messages)));

 }


@Test

 void negativeOrMissingTimestampsAreSafe() {



 assertEquals("-", messages.plain(TimeText.relative(0L, 1_000L, messages)));



 assertEquals("now", messages.plain(TimeText.relative(2_000L, 1_000L, messages)));

 }


@Test

 void durationFormatsLargestUnits() {



 assertEquals("-", TimeText.duration(0L, messages));



 assertEquals("5s", TimeText.duration(5_000L, messages));



 assertEquals("2m 3s", TimeText.duration(123_000L, messages));



 assertEquals("1h 1m", TimeText.duration(3_660_000L, messages));



 assertEquals("2d 1h", TimeText.duration(2L * 86_400_000L + 3_600_000L, messages));

 }


@Test

 void realLanguageFileCanBeLoaded() {



 try (InputStream input = TimeTextTest.class.getResourceAsStream("/lang/en_US.yml")) {





 assertNotNull(input);





 Messages real = Messages.fromStream(input, "en_US");





 assertTrue(real.has("time.now"), "time.now missing");





 String raw = real.raw("time.now");





 net.kyori.adventure.text.Component rendered = real.render(raw, Map.of());





 assertEquals("just now", real.plain(rendered),







 "raw=" + raw + " rendered=" + real.plain(rendered));



 } catch (java.io.IOException exception) {





 throw new AssertionError(exception);



 }

 } }
