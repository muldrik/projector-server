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
import kotlin.concurrent.timer

object MemoryStats {
  val average = Average()

  data class MemoryUsage(val timestamp: Long, val total: Long, val used: Long) {
    override fun toString(): String {
      return """
          Timestamp: ${timestamp / 1000}
          Used memory: ${used / 1024 / 1024}Mb, total memory: ${total / 1024 / 1024}Mb
        """.trimIndent()
    }

    fun toCsvString(): String {
      return """
          ${timestamp},Used,${used / 1024 / 1024}
          ${timestamp},Total,${total / 1024 / 1024}
          
        """.trimIndent()
    }
  }

  private val usages = mutableListOf<MemoryUsage>()

  fun startMemoryStatsCollector() = timer("Used memory checker", true, 0, 1000) {
    val totalMemory = Runtime.getRuntime().totalMemory()
    val usedMemory = totalMemory - Runtime.getRuntime().freeMemory()
    val timeNow = ServerStats.getTimestampFromStart()
    synchronized(usages) {
      usages.add(MemoryUsage(timeNow, totalMemory, usedMemory))
      average.add(usedMemory)
    }
  }

  val metricsFileName = "outputStats/memoryMetrics.txt"
  val fileForPlottingName = "outputStats/memoryForPlotting.csv"
  fun dumpStats() {
    FileOutputStream(metricsFileName, true).bufferedWriter().use { out ->
      out.write("${average.result() / 1024 / 1024}")
      out.newLine()
      out.write("!")
      out.newLine()
    }
    synchronized(usages) {
      FileOutputStream(fileForPlottingName, true).bufferedWriter().use { out ->
        out.write("timestamp,type,value")
        out.newLine()
        usages.forEach {
          out.write(it.toCsvString())
        }
      }
    }
  }
}
