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

package com.reportdesk.gui;
import com.reportdesk.core.ReportFilter; import org.bukkit.inventory.Inventory; import org.bukkit.inventory.InventoryHolder;
import java.util.HashMap; import java.util.Map;
/**
* Typed inventory holder. Actions are derived from the holder type and its
* slot maps instead of fragile title matching.
*/ public final class ReportDeskHolder implements InventoryHolder {


public enum Type {



 PLAYER_SELECT,



 CATEGORY_SELECT,



 CHAT_SELECT,



 CONFIRM,



 ADMIN_LIST,



 ADMIN_FILTER,



 ADMIN_DETAIL,



 VERDICT,



 DELETE,



 NOTES,



 EVIDENCE,



 REPLIES,



 ADMIN_HUB,



 ADMIN_PLAYERS,



 ADMIN_PLAYER,



 ADMIN_PLAYER_REPORTS,



 ADMIN_STATS,



 ADMIN_WATCHLIST,



 ADMIN_CONFIG_SECTIONS,



 ADMIN_CONFIG_KEYS,



 ADMIN_CONFIG_KEY,



 ADMIN_MISC,



 ADMIN_AUDIT,



 ADMIN_CONFIRM,



 ADMIN_CHAT

 }


private final Type type;

 private final long reportId;

 private final int page;

 private final ReportFilter filter;

 private final Map<Integer, String> values = new HashMap<>();

 private final Map<Integer, Long> reportIds = new HashMap<>();

 private final Map<String, String> attributes = new HashMap<>();

 private Inventory inventory;


private ReportDeskHolder(Type type, long reportId, int page, ReportFilter filter) {



 this.type = type;



 this.reportId = reportId;



 this.page = page;



 this.filter = filter;

 }


public static ReportDeskHolder of(Type type) {



 return new ReportDeskHolder(type, 0L, 1, ReportFilter.all());

 }


public static ReportDeskHolder of(Type type, long reportId) {



 return new ReportDeskHolder(type, reportId, 1, ReportFilter.all());

 }


public static ReportDeskHolder of(Type type, long reportId, int page) {



 return new ReportDeskHolder(type, reportId, page, ReportFilter.all());

 }


public static ReportDeskHolder list(ReportFilter filter, int page) {



 return new ReportDeskHolder(Type.ADMIN_LIST, 0L, page,





 filter == null ? ReportFilter.all() : filter);

 }


public static ReportDeskHolder filter(ReportFilter filter, int page) {



 return new ReportDeskHolder(Type.ADMIN_FILTER, 0L, page,





 filter == null ? ReportFilter.all() : filter);

 }


public static ReportDeskHolder paged(Type type, int page) {



 return new ReportDeskHolder(type, 0L, page, ReportFilter.all());

 }


public Type type() {



 return type;

 }


public long reportId() {



 return reportId;

 }


public int page() {



 return page;

 }


public ReportFilter filter() {



 return filter;

 }


public void putValue(int slot, String value) {



 if (value != null) {





 values.put(slot, value);



 }

 }


public String value(int slot) {



 return values.get(slot);

 }


public void putReportId(int slot, long id) {



 reportIds.put(slot, id);

 }


public Long reportId(int slot) {



 return reportIds.get(slot);

 }


public void putAttribute(String key, String value) {



 if (key != null && value != null) {





 attributes.put(key, value);



 }

 }


public String attribute(String key) {



 return key == null ? null : attributes.get(key);

 }


public String attribute(String key, String fallback) {



 String value = attribute(key);



 return value == null ? fallback : value;

 }


void setInventory(Inventory inventory) {



 this.inventory = inventory;

 }


@Override

 public Inventory getInventory() {



 return inventory;

 } }
