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

package com.reportdesk;
import org.junit.jupiter.api.Test; import org.yaml.snakeyaml.Yaml;
import java.io.InputStream; import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals; import static org.junit.jupiter.api.Assertions.assertNotNull; import static org.junit.jupiter.api.Assertions.assertTrue;
class PluginDescriptorTest {


@Test

 void paperDescriptorKeepsApiVersionAt120() {



 Map<String, Object> descriptor = read("/paper-plugin.yml");




assertEquals("1.20", descriptor.get("api-version"),





 "paper-plugin.yml must keep api-version: '1.20' unless the compatibility plan is updated");



 assertEquals("ReportDesk", descriptor.get("name"));



 assertEquals("com.reportdesk.ReportDeskPlugin", descriptor.get("main"));

 }


@Test

 void legacyDescriptorMatchesAndDeclaresCommands() {



 Map<String, Object> descriptor = read("/plugin.yml");




assertEquals("1.20", descriptor.get("api-version"));



 assertEquals("com.reportdesk.ReportDeskPlugin", descriptor.get("main"));




assertTrue(descriptor.get("commands") instanceof Map<?, ?>, "commands must be a mapping");



 Map<?, ?> commands = (Map<?, ?>) descriptor.get("commands");



 assertTrue(commands.containsKey("report"));



 assertTrue(commands.containsKey("reports"));




Map<?, ?> permissions = (Map<?, ?>) descriptor.get("permissions");



 assertTrue(permissions.containsKey("reportdesk.report"));



 assertTrue(permissions.containsKey("reportdesk.admin"));



 assertTrue(permissions.containsKey("reportdesk.bypass.limits"));

 }


private static Map<String, Object> read(String resource) {



 try (InputStream input = PluginDescriptorTest.class.getResourceAsStream(resource)) {





 assertNotNull(input, "missing resource " + resource);





 Object loaded = new Yaml().load(input);





 assertTrue(loaded instanceof Map<?, ?>, resource + " must be a mapping");





 @SuppressWarnings("unchecked")





 Map<String, Object> map = (Map<String, Object>) loaded;





 return map;



 } catch (java.io.IOException exception) {





 throw new AssertionError(exception);



 }

 } }
