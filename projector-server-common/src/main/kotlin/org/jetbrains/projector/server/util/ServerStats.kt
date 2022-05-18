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
package org.jetbrains.projector.server.util

import org.jetbrains.projector.server.util.stats.*
import java.io.File
import java.util.*
import kotlin.concurrent.schedule
import kotlin.concurrent.timer
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


open class Stats(val blockName: String) {
  var currentStart: Long = 0
  private val currentMeasurements = mutableListOf<TimeMeasurement>()
  open val metrics = listOf<Metric>() // Override to include metrics

  fun standaloneSimpleMeasure(name: String, block: () -> Unit) {
    startMeasurement()
    simpleMeasure(name, block)
    endMeasurement()
  }


  fun startMeasurement() {
    currentStart = ServerStats.getTimestampFromStart()
  }

  open fun endMeasurement(): TimeMeasurement {
    val end = ServerStats.getTimestampFromStart()
    for (metric in metrics) { // Default behavior. Override to include submeasurements
      metric.add(end - currentStart)
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

sealed class TimeMeasurement(open val name: String, open val start: Long, open val end: Long) {
  abstract fun toCsvString(commonPrefix: String = ""): String
}

data class SimpleMeasurement(override val name: String, override val start: Long, override val end: Long) : TimeMeasurement(name, start,
                                                                                                                            end) {
  override fun toString(): String {
    return "$name: ${end - start} ms"
  }

  override fun toCsvString(commonPrefix: String): String {
    return "${commonPrefix}$name,${end-start}\n"
  }
}


data class RecMeasurement(
  override val name: String,
  override val start: Long,
  override val end: Long,
  val subMeasurements: List<TimeMeasurement>,
) : TimeMeasurement(name, start, end) {
  override fun toCsvString(commonPrefix: String): String {
    val res = StringBuilder()
    subMeasurements.forEach { measurement ->
      res.append(measurement.toCsvString(commonPrefix))
    }
    return res.toString()
  }
}

fun TimeMeasurement.unroll(): List<SimpleMeasurement> {
  return when (this) {
    is SimpleMeasurement -> listOf(this)
    is RecMeasurement -> this.subMeasurements.map { it.unroll() }.flatten()
  }
}

val EmptyMeasurement = SimpleMeasurement("Empty", 0, 0)

object ServerStats {
  val globalStartTime = System.currentTimeMillis()
  fun getTimestampFromStart() = System.currentTimeMillis() - globalStartTime

  fun setStatsDumpCountdown() = Timer().schedule(60000) {
    println("Dumping the stats")
    CreateUpdateStats.dumpStats()
    MemoryStats.dumpStats()
    NetworkStats.dumpStats()
    AwtStats.dumpStats()
  }

  object CreateUpdateStats : Stats("Create update loop") {
    override val metrics = listOf(
      Average(blockName),
      TimeRate(blockName),
      PeakRate(blockName, 3),
      PeakRate(blockName, 5),
      PeakRate(blockName, 10),
      PeakRate(blockName, 20),
      PowerPunishingRate(blockName, 3, 1.2),
      PowerPunishingRate(blockName, 5, 1.2),
      PowerPunishingRate(blockName, 3, 1.5),
      PowerPunishingRate(blockName, 5, 1.5),
      PowerPunishingRate(blockName, 3, 2.0),
      PowerPunishingRate(blockName, 5, 2.0)
    )

    private const val timeThreshold: Long = 8
    private val interestingMeasurements = mutableListOf<TimeMeasurement>()
    override fun endMeasurement(): TimeMeasurement = super.endMeasurement().also {
      if (it.end - it.start > timeThreshold)
        synchronized(interestingMeasurements) { interestingMeasurements.add(it) }
    }

    private val csvFile = File("stats/createUpdateTime.csv")
    private val metricsFile = File("stats/createUpdateTime.txt")
    fun dumpStats() {

      metricsFile.printWriter().use { out ->
        out.println("Time stats:")
        for (metric in metrics) {
          out.println("${metric.name} ${metric.dumpResult()}")
        }
      }


      synchronized(interestingMeasurements) {
        csvFile.printWriter().use { out ->
          out.println("timestamp,task,len")
          interestingMeasurements.forEach {
            val timestamp = it.start
            out.print(it.toCsvString("$timestamp,").removeSuffix(","))
          }
        }
      }
    }
  }
  val CreateDataStats = Stats("Create data to send")


  object MemoryStats {
    val average = Average("Memory usage")

    data class MemoryUsage(val timestamp: Long, val total: Long, val used: Long) {
      override fun toString(): String {
        return """
          Timestamp: ${timestamp / 1000}
          Used memory: ${used / 1024 / 1024}Mb, total memory: ${total / 1024 / 1024}Mb
        """.trimIndent()
      }
      fun toCsvString(): String {
        return """
          ${timestamp},Used,${used/1024/1024}
          ${timestamp},Total,${total/1024/1024}
          
        """.trimIndent()
      }
    }

    private val usages = mutableListOf<MemoryUsage>()

    fun startMemoryStatsCollector() = timer("Used memory checker", true, 0, 1000) {
      val totalMemory = Runtime.getRuntime().totalMemory()
      val usedMemory = totalMemory - Runtime.getRuntime().freeMemory()
      val timeNow = getTimestampFromStart()
      synchronized(usages) {
        usages.add(MemoryUsage(timeNow, totalMemory, usedMemory))
        average.add(usedMemory)
      }
    }

    val metricsFile = File("stats/memory.txt")
    val csvFile = File("stats/memory.csv")
    fun dumpStats() = synchronized(usages) {
      metricsFile.printWriter().use { out ->
        out.println(average.dumpResult())
      }

      csvFile.printWriter().use { out ->
        out.println("timestamp,type,value")
        usages.forEach {
          out.print(it.toCsvString())
        }
      }
    }
  }

  object NetworkStats {
    val average = Average("Network usage")

    data class SentPacket(val timestamp: Long, val byteSize: Long) {
      fun toCsvString(): String {
        return "${timestamp},$byteSize\n"
      }
    }

    private val packets = mutableListOf<SentPacket>()

    fun add(timestamp: Long, byteSize: Long) = synchronized(packets) {
      packets.add(SentPacket(timestamp, byteSize))
    }

    val metricsFile = File("stats/network.txt")
    val csvFile = File("stats/network.csv")
    fun dumpStats() = synchronized(packets) {
      metricsFile.printWriter().use { out ->
        out.println("Average in network usage (Kb/s): ${average.result() / 1024}")
      }

      csvFile.printWriter().use { out ->
        out.println("timestamp,bytes")
        packets.forEach { out.print(it.toCsvString()) }
      }
    }
  }

  object AwtStats: Stats("Awt processing") {
    override val metrics: List<Metric> = listOf(
      Average(blockName),
      TimeRate(blockName),
      PeakRate(blockName, 3),
      PeakRate(blockName, 5),
      PeakRate(blockName, 10),
      PeakRate(blockName, 20),
      PowerPunishingRate(blockName, 3, 1.2),
      PowerPunishingRate(blockName, 5, 1.2),
      PowerPunishingRate(blockName, 3, 1.5),
      PowerPunishingRate(blockName, 5, 1.5),
      PowerPunishingRate(blockName, 3, 2.0),
      PowerPunishingRate(blockName, 5, 2.0)
    )

    private val metricsFile = File("stats/createUpdateTime.txt")

    fun dumpStats() {
      metricsFile.printWriter().use { out ->
        out.println("Time stats:")
        for (metric in CreateUpdateStats.metrics) {
          out.println("${metric.name} ${metric.dumpResult()}")
        }
      }
    }
  }
}





/*

const val measurementsEnabled = true

class Measurement(val name: String, val startTime: Long, val endTime: Long, val subMeasurements: List<Measurement>)

data class MeasuredBlock<T>(val result : T, val measurement: Measurement)

data class BlockSplits<T>(val result: T, val subMeasurements: List<Measurement>)

fun <T> measureTime(blockName: String, block: () -> T): MeasuredBlock<T> {
  val startTime = System.currentTimeMillis()
  val res = block()
  return MeasuredBlock(res, Measurement(blockName, startTime, System.currentTimeMillis(), emptyList()))
}

fun <T> measureTimeRec(blockName: String, block: () -> BlockSplits<T>): MeasuredBlock<T> {
  val startTime = System.currentTimeMillis()
  val blockSplits = block()
  val blockRes = blockSplits.result
  val subMeasurements = blockSplits.subMeasurements
  if (subMeasurements.isEmpty()) {}
  return MeasuredBlock(blockRes, Measurement(blockName, startTime, System.currentTimeMillis(), subMeasurements))
}

fun kek() {
  timer("Used memory checker", true, 0, 1000) {
    val totalMemory = Runtime.getRuntime().totalMemory()
    val usedMemory = totalMemory - Runtime.getRuntime().freeMemory()
    // Print used and total memory in megabytes
    println("Used memory: ${usedMemory / 1024 / 1024}Mb, total memory: ${totalMemory / 1024 / 1024}Mb")
  }
}

// val (res, measurements) = measureTime {}
//

 */
