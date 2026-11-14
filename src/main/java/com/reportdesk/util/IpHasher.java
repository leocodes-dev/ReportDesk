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

package com.reportdesk.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Salted SHA-256 hash for optional reporter IP provenance.
 *
 * <p>Only the hash is persisted; the plaintext IP never leaves the Bukkit layer. The salt comes
 * from {@code security.ip-hash-salt} so two servers can correlate ban lists without sharing raw
 * IPs.
 */
public final class IpHasher {

  private IpHasher() {}

  public static String hash(String salt, String ip) {

    if (ip == null || ip.isBlank()) {

      return "";
    }

    try {

      MessageDigest digest = MessageDigest.getInstance("SHA-256");

      String input = (salt == null ? "" : salt) + ":" + ip.trim();

      byte[] out = digest.digest(input.getBytes(StandardCharsets.UTF_8));

      StringBuilder hex = new StringBuilder(out.length * 2);

      for (byte b : out) {

        hex.append(String.format("%02x", b));
      }

      return hex.toString();
    } catch (NoSuchAlgorithmException exception) {

      // SHA-256 is mandatory in every JRE; reaching this means the runtime is broken.
      throw new IllegalStateException("SHA-256 unavailable", exception);
    }
  }
}
