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

package com.reportdesk.storage;

import com.reportdesk.core.Report;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;

/**
 * Pure CSV export helper (RFC 4180 quoting) so the formatting is testable without touching the file
 * system.
 */
public final class CsvExporter {

  private static final DateTimeFormatter TIME =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

  private CsvExporter() {}

  public static String toCsv(Collection<Report> reports) {

    StringBuilder builder = new StringBuilder(256);

    builder
        .append(
            "id,created_at,status,category,target,reporter,claimed_by,closed_by,verdict,detail,notes,reported_from,ip_hash")
        .append('\n');

    if (reports != null) {

      for (Report report : List.copyOf(reports)) {

        append(builder, report.id());

        builder.append(',');

        append(builder, TIME.format(Instant.ofEpochMilli(report.createdAt())));

        builder.append(',');

        append(builder, report.status().name());

        builder.append(',');

        append(builder, report.category());

        builder.append(',');

        append(builder, report.targetName());

        builder.append(',');

        append(builder, report.reporterName());

        builder.append(',');

        append(builder, report.claimedByName());

        builder.append(',');

        append(builder, report.closedByName());

        builder.append(',');

        append(builder, report.verdict());

        builder.append(',');

        append(builder, report.detail());

        builder.append(',');

        append(builder, String.valueOf(report.notes().size()));

        builder.append(',');

        append(builder, report.reportedFrom());

        builder.append(',');

        append(builder, report.ipHash());

        builder.append('\n');
      }
    }

    return builder.toString();
  }

  private static void append(StringBuilder builder, Object value) {

    String text = value == null ? "" : String.valueOf(value);

    text = text.replace('\r', ' ').replace('\n', ' ');

    boolean quote = text.indexOf(',') >= 0 || text.indexOf('"') >= 0 || text.indexOf(';') >= 0;

    if (quote) {

      builder.append('"');
    }

    builder.append(text.replace("\"", "\"\""));

    if (quote) {

      builder.append('"');
    }
  }
}
