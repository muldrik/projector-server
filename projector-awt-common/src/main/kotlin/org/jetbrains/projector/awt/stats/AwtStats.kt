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

import org.jetbrains.projector.awt.stats.metrics.Average
import org.jetbrains.projector.awt.stats.metrics.Metric
import org.jetbrains.projector.awt.stats.metrics.PeakRate
import org.jetbrains.projector.awt.stats.metrics.PowerPunishingRate
import java.io.FileOutputStream

object AwtStats : TimeStats("Awt processing") {
  override val metrics: List<Metric> = listOf(
    Average(),
    PeakRate(),
    PeakRate(3),
    PeakRate(5),
    PeakRate(10),
    PeakRate(20),
    PowerPunishingRate(1.2, 3),
    PowerPunishingRate(1.2, 5),
    PowerPunishingRate(1.5, 3),
    PowerPunishingRate(1.5, 5),
    PowerPunishingRate(2.0, 3),
    PowerPunishingRate(2.0, 5)
  )

  private val metricsFileName = "outputStats/awtMetrics.csv"

  fun dumpStats() {
    FileOutputStream(metricsFileName, true).bufferedWriter().use { out ->
      out.write(Metric.csvHeader())
      out.newLine()
      for (metric in metrics) {
        out.write(metric.csvResult())
        out.newLine()
      }
      out.write("!")
      out.newLine()
    }
  }
}
