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
package org.jetbrains.projector.awt.stats

import org.jetbrains.projector.awt.stats.metrics.Metric
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

open class TimeStats(val blockName: String) {
  var currentStart: Long = 0
  private val currentMeasurements = mutableListOf<TimeMeasurement>()
  open val metrics = listOf<Metric>() // Override to include metrics

  @OptIn(ExperimentalContracts::class)
  fun standaloneSimpleMeasure(name: String, block: () -> Unit) {
    startMeasurement()
    simpleMeasure(name, block)
    endMeasurement()
  }


  fun startMeasurement() {
    currentStart = ServerStats.getTimestampFromStart()
  }

  open fun endMeasurement(processedObjects: Int = 1): TimeMeasurement {
    val end = ServerStats.getTimestampFromStart()
    for (metric in metrics) { // Default behavior. Override to include submeasurements
      metric.add(end - currentStart, processedObjects)
    }
    return RecMeasurement(blockName, currentStart, end, currentMeasurements.toList()).also { currentMeasurements.clear() }
  }

  fun addMeasurement(measurement: TimeMeasurement) {
    currentMeasurements.add(measurement)
  }

  @OptIn(ExperimentalContracts::class)
  fun simpleMeasure(name: String, block: () -> Unit) {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    val start = ServerStats.getTimestampFromStart()
    block()
    val end = ServerStats.getTimestampFromStart()
    currentMeasurements.add(SimpleMeasurement(name, start, end))
  }
}
