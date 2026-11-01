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
/**
* Config-backed anti-abuse limits for report submission.
*/ public record RateLimitPolicy(

 long cooldownSeconds,

 int dailyLimit,

 long sameTargetIntervalSeconds,

 long duplicateWindowSeconds,

 int maxOpenPerTarget ) {


public RateLimitPolicy {



 cooldownSeconds = Math.max(0L, cooldownSeconds);



 dailyLimit = Math.max(0, dailyLimit);



 sameTargetIntervalSeconds = Math.max(0L, sameTargetIntervalSeconds);



 duplicateWindowSeconds = Math.max(0L, duplicateWindowSeconds);



 maxOpenPerTarget = Math.max(0, maxOpenPerTarget);

 } }
