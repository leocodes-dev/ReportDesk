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

import java.util.List;
import java.util.Set;

/**
 * Minimal configuration abstraction so {@link ConfigLoader} can be tested without a running Bukkit
 * server.
 */
public interface ConfigSource {

  String getString(String path, String fallback);

  int getInt(String path, int fallback);

  boolean getBoolean(String path, boolean fallback);

  List<String> getStringList(String path, List<String> fallback);

  Set<String> keys(String path);

  boolean isSection(String path);

  ConfigSource section(String path);
}
