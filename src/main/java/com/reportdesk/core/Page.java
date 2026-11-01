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

import java.util.List;

/** Immutable page result with 1-based page numbers. */
public record Page<T>(List<T> items, int page, int pageSize, int totalItems, int totalPages) {

  public Page {

    items = items == null ? List.of() : List.copyOf(items);

    pageSize = Math.max(1, pageSize);

    totalItems = Math.max(0, totalItems);

    totalPages = Math.max(1, totalPages);

    page = Math.max(1, Math.min(page, totalPages));
  }

  public static <T> Page<T> of(List<T> all, int requestedPage, int requestedSize) {

    int size = Math.max(1, requestedSize);

    int total = all == null ? 0 : all.size();

    int pages = Math.max(1, (total + size - 1) / size);

    int page = Math.max(1, Math.min(requestedPage, pages));

    int from = Math.min(total, (page - 1) * size);

    int to = Math.min(total, from + size);

    List<T> slice = total == 0 ? List.of() : List.copyOf(all.subList(from, to));

    return new Page<>(slice, page, size, total, pages);
  }

  public boolean hasNext() {

    return page < totalPages;
  }

  public boolean hasPrevious() {

    return page > 1;
  }
}
