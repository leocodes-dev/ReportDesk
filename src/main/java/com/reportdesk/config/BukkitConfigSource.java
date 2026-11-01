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

package com.reportdesk.config;
import org.bukkit.configuration.ConfigurationSection;
import java.util.List; import java.util.Set;
/**
* Bukkit-backed configuration source. Only used for config.yml/messages.yml
* loading during plugin startup and reload.
*/ public final class BukkitConfigSource implements ConfigSource {


private final ConfigurationSection section;


public BukkitConfigSource(ConfigurationSection section) {



 this.section = section;

 }


@Override

 public String getString(String path, String fallback) {



 if (section == null) {





 return fallback;



 }



 String value = section.getString(path);



 return value == null ? fallback : value;

 }


@Override

 public int getInt(String path, int fallback) {



 return section == null ? fallback : section.getInt(path, fallback);

 }


@Override

 public boolean getBoolean(String path, boolean fallback) {



 return section == null ? fallback : section.getBoolean(path, fallback);

 }


@Override

 public List<String> getStringList(String path, List<String> fallback) {



 if (section == null || !section.isList(path)) {





 return fallback;



 }



 return section.getStringList(path);

 }


@Override

 public Set<String> keys(String path) {



 if (section == null) {





 return Set.of();



 }



 if (path == null || path.isBlank()) {





 return section.getKeys(false);



 }



 ConfigurationSection child = section.getConfigurationSection(path);



 return child == null ? Set.of() : child.getKeys(false);

 }


@Override

 public boolean isSection(String path) {



 if (section == null) {





 return false;



 }



 if (path == null || path.isBlank()) {





 return true;



 }



 return section.isConfigurationSection(path);

 }


@Override

 public ConfigSource section(String path) {



 if (section == null) {





 return new BukkitConfigSource(null);



 }



 if (path == null || path.isBlank()) {





 return this;



 }



 return new BukkitConfigSource(section.getConfigurationSection(path));

 } }
