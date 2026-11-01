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

package com.reportdesk.webhook;
import org.junit.jupiter.api.Test;
import java.util.List; import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals; import static org.junit.jupiter.api.Assertions.assertFalse; import static org.junit.jupiter.api.Assertions.assertTrue;
class WebhookPayloadTest {


@Test

 void defaultsAndBlankAvatarAreHandled() {



 WebhookPayload payload = new WebhookPayload(null, " ", null, null, 0, null);



 Map<String, Object> map = payload.toMap();




assertEquals("ReportDesk", map.get("username"));



 assertFalse(map.containsKey("avatar_url"));



 assertEquals("", map.get("content"));

 }


@Test

 void avatarIsIncludedWhenPresent() {



 WebhookPayload payload = new WebhookPayload("Desk", "https://example.com/a.png", "", "Title", 1,





 List.of());



 assertEquals("https://example.com/a.png", payload.toMap().get("avatar_url"));

 }


@Test

 void longTextIsTruncatedToPlatformLimits() {



 String longContent = "x".repeat(2_000);



 String longTitle = "t".repeat(300);



 WebhookPayload payload = new WebhookPayload("Desk", "", longContent, longTitle, 0,





 List.of(new WebhookPayload.Field("n".repeat(300), "v".repeat(1_100), true)));




Map<String, Object> map = payload.toMap();



 String content = (String) map.get("content");



 assertEquals(1_900, content.length());



 assertTrue(content.endsWith("…"));




@SuppressWarnings("unchecked")



 Map<String, Object> embed = ((List<Map<String, Object>>) map.get("embeds")).get(0);



 assertEquals(256, ((String) embed.get("title")).length());




@SuppressWarnings("unchecked")



 List<Map<String, Object>> fields = (List<Map<String, Object>>) embed.get("fields");



 assertEquals(1, fields.size());



 assertEquals(256, ((String) fields.get(0).get("name")).length());



 assertEquals(1_024, ((String) fields.get(0).get("value")).length());

 }


@Test

 void fieldsWithBlankNamesAreDroppedAndInlinePreserved() {



 WebhookPayload payload = new WebhookPayload("Desk", "", "", "Title", 5, List.of(





 new WebhookPayload.Field("", "ignored", true),





 new WebhookPayload.Field(null, "ignored", true),





 new WebhookPayload.Field("Target", "Bob", false)



 ));




@SuppressWarnings("unchecked")



 List<Map<String, Object>> fields = (List<Map<String, Object>>) ((List<Map<String, Object>>)





 payload.toMap().get("embeds")).get(0).get("fields");




assertEquals(1, fields.size());



 assertEquals("Target", fields.get(0).get("name"));



 assertEquals("Bob", fields.get(0).get("value"));



 assertEquals(false, fields.get(0).get("inline"));

 }


@Test

 void nullFieldListBecomesEmptyEmbed() {



 WebhookPayload payload = new WebhookPayload("Desk", "", "", "Title", 0, List.of());



 @SuppressWarnings("unchecked")



 List<Map<String, Object>> fields = (List<Map<String, Object>>) ((List<Map<String, Object>>)





 payload.toMap().get("embeds")).get(0).get("fields");



 assertTrue(fields.isEmpty());

 } }
