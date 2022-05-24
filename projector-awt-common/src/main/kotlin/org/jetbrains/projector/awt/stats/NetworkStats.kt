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
import java.io.FileOutputStream

object NetworkStats {
  val averagePacketSizes = Average()

  data class SentPacket(val timestamp: Long, val byteSize: Long) {
    fun toCsvString(): String {
      return "${timestamp},$byteSize\n"
    }
  }

  private val packets = mutableListOf<SentPacket>()

  private val firstPacketSent: Long by lazy { System.currentTimeMillis() - ServerStats.globalStartTime }
  var firstMessageTime = ServerStats.globalStartTime

  fun add(timestamp: Long, byteSize: Long) = synchronized(packets) {
    firstPacketSent
    packets.add(SentPacket(timestamp, byteSize))
    averagePacketSizes.add(byteSize)
  }

  val metricsFileName = "outputStats/networkMetrics.txt"
  val plottingFileName = "outputStats/networkForPlotting.csv"

  fun dumpStats() {
    FileOutputStream(metricsFileName, true).bufferedWriter().use { out ->
      out.write("${averagePacketSizes.result()}") // Average in packet size (bytes)
      out.newLine()
      out.write( // Average in network usage (Kb/s)
        "${averagePacketSizes.total * 1000 / 1024 / (System.currentTimeMillis() - firstMessageTime)}")
      out.newLine()
      out.write("!")
      out.newLine()
    }
    synchronized(packets) {
      FileOutputStream(plottingFileName, true).bufferedWriter().use { out ->
        out.write("timestamp,bytes")
        out.newLine()
        packets.forEach { out.write(it.toCsvString()) }
      }
    }
  }
}
