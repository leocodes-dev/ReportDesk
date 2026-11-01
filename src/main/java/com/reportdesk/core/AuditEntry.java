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

import java.time.Instant;
import java.util.UUID;

/**
 * One audit log entry. The format is tab-separated so it stays readable and is
 * easy to parse without a JSON dependency on the read path.
 */
public record AuditEntry(
    long timestamp,
    UUID actorId,
    String actorName,
    String action,
    long reportId,
    String detail) {

    public AuditEntry {
        actorId = actorId == null ? new UUID(0L, 0L) : actorId;
        actorName = clean(actorName);
        action = clean(action);
        detail = clean(detail);
    }

    public String toLine() {
        return Instant.ofEpochMilli(timestamp) + "\t"
            + actorId + "\t"
            + actorName + "\t"
            + action + "\t"
            + (reportId <= 0 ? "-" : String.valueOf(reportId)) + "\t"
            + detail;
    }

    public static AuditEntry fromLine(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }
        String[] parts = line.split("\t", -1);
        if (parts.length < 6) {
            return null;
        }
        try {
            long timestamp = Instant.parse(parts[0]).toEpochMilli();
            UUID actor = UUID.fromString(parts[1]);
            long reportId = "-".equals(parts[4]) ? 0L : Long.parseLong(parts[4]);
            return new AuditEntry(timestamp, actor, parts[2], parts[3], reportId, parts[5]);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static String clean(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\t', ' ').replace('\r', ' ').replace('\n', ' ');
    }
}
