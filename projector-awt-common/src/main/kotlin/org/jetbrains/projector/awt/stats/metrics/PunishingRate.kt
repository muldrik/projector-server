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
package org.jetbrains.projector.awt.stats.metrics

import org.jetbrains.projector.awt.stats.ServerStats
import kotlin.math.pow
import kotlin.math.roundToLong


open class PunishingRate(threshold: Long = 0, objectsThreshold: Int = 0, name: String = "Punishing rate", val errorFunction: (Long, Int) -> Long)
  : Metric(threshold, objectsThreshold, name) {
  var totalTime: Long = 0
  override var measurementUnit = "errorFun(ms)/second"
  override fun add(value: Long, processedObjects: Int) {
    totalTime += errorFunction(value, processedObjects)
  }

  override fun result(): Long = totalTime * 1000 / ServerStats.getTimestampFromStart()
}

class PowerPunishingRate(val power: Double, threshold: Long = 0, objectsThreshold: Int = 0, name: String = "Power punishing rate") :
  PunishingRate(threshold, objectsThreshold, name, { time, processedObjects ->
    if (time < threshold || processedObjects < objectsThreshold) 0L
    else time.toDouble().pow(power).roundToLong()
  }) {
  override var measurementUnit = "(ms)^$power / second"
  }


