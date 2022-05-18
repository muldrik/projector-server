/*
 * Copyright (c) 2019-2022, JetBrains s.r.o. and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. JetBrains designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact JetBrains, Na Hrebenech II 1718/10, Prague, 14000, Czech Republic
 * if you need additional information or have any questions.
 */
package org.jetbrains.projector.server.util.stats

import org.jetbrains.projector.server.util.ServerStats
import kotlin.math.pow
import kotlin.math.roundToLong


open class PunishingRate(name: String, val errorFunction: (Long) -> Long): Metric(name) {
  var totalTime: Long = 0
  override fun add(value: Long) {
      totalTime += errorFunction(value)
  }

  override fun dumpResult(): String {
    return "Punishing rate in $name: ${totalTime * 1000 / ServerStats.getTimestampFromStart()} "
  }
}

class PowerPunishingRate(name: String, val threshold: Long, val power: Double): PunishingRate(name, { time ->
  if (time < threshold) 0L
  else time.toDouble().pow(power).roundToLong()
}) {
  override fun dumpResult(): String {
    return "Power punishing rate in $name. Power n^($power). Threshold ${threshold}ms: ${totalTime * 1000 / ServerStats.getTimestampFromStart()}"
  }
}
