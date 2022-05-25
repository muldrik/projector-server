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

import org.jetbrains.projector.awt.stats.metrics.*
import java.io.File
import java.io.FileOutputStream

object AwtStats : TopLevelTimeStats("Awt processing") {
  override val metrics: List<Metric> = listOf(
    Average(),
    EventFrequency(10),
    EventFrequency(50),
    PeakRate(),
    PeakRate(3),
    PeakRate(5),
    PeakRate(10),
    PeakRate(20),
    PeakRate(40),
    PowerPunishingRate(1.2, 3),
    PowerPunishingRate(1.2, 5),
    PowerPunishingRate(1.5, 5),
    PowerPunishingRate(2.0, 5),
    PowerPunishingRate(1.5, 15),
    PowerPunishingRate(1.5, 30),
  )

  override val plottingFileName = "outputStats/awtForPlotting.csv"
  override val metricsFileName = "outputStats/awtMetrics.csv"

}
