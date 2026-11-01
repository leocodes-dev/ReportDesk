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

package com.reportdesk.config;
import java.util.ArrayList; import java.util.LinkedHashSet; import java.util.List; import java.util.Map; import java.util.Set;
/**
* Map-backed config source used by unit tests.
*/ public final class MapConfigSource implements ConfigSource {


private final Map<?, ?> root;


public MapConfigSource(Map<?, ?> root) {



 this.root = root == null ? Map.of() : root;

 }


@Override

 public String getString(String path, String fallback) {



 Object value = value(path);



 return value instanceof String string ? string : fallback;

 }


@Override

 public int getInt(String path, int fallback) {



 Object value = value(path);



 if (value instanceof Number number) {





 return number.intValue();



 }



 if (value instanceof String string) {





 try {







 return Integer.parseInt(string.trim());





 } catch (NumberFormatException ignored) {







 return fallback;





 }



 }



 return fallback;

 }


@Override

 public boolean getBoolean(String path, boolean fallback) {



 Object value = value(path);



 if (value instanceof Boolean bool) {





 return bool;



 }



 if (value instanceof String string) {





 if ("true".equalsIgnoreCase(string.trim())) {







 return true;





 }





 if ("false".equalsIgnoreCase(string.trim())) {







 return false;





 }



 }



 return fallback;

 }


@Override

 public List<String> getStringList(String path, List<String> fallback) {



 Object value = value(path);



 if (value instanceof List<?> list) {





 List<String> result = new ArrayList<>();





 for (Object element : list) {







 if (element != null) {









 result.add(String.valueOf(element));







 }





 }





 return result;



 }



 if (value instanceof String string) {





 return List.of(string);



 }



 return fallback;

 }


@Override

 public Set<String> keys(String path) {



 Object value = value(path);



 if (value instanceof Map<?, ?> map) {





 Set<String> keys = new LinkedHashSet<>();





 for (Object key : map.keySet()) {







 if (key != null) {









 keys.add(String.valueOf(key));







 }





 }





 return keys;



 }



 return Set.of();

 }


@Override

 public boolean isSection(String path) {



 return value(path) instanceof Map<?, ?>;

 }


@Override

 public ConfigSource section(String path) {



 Object value = value(path);



 return value instanceof Map<?, ?> map ? new MapConfigSource(map) : new MapConfigSource(Map.of());

 }


private Object value(String path) {



 if (path == null || path.isBlank()) {





 return root;



 }



 Object current = root;



 for (String part : path.split("\\.")) {





 if (!(current instanceof Map<?, ?> map)) {







 return null;





 }





 current = map.get(part);



 }



 return current;

 } }
