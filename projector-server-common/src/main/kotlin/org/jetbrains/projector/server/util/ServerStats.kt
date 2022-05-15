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

import java.io.File
import java.util.*
import kotlin.concurrent.schedule
import kotlin.concurrent.timer
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


open class Stats(private val blockName: String) {
  var currentStart: Long = 0
  private val currentMeasurements = mutableListOf<TimeMeasurement>()

  fun startMeasurement() {
    currentStart = ServerStats.getCurrentTimestamp()
  }

  open fun endMeasurement(): TimeMeasurement {
    val end = ServerStats.getCurrentTimestamp()
    return RecMeasurement(blockName, currentStart, end, currentMeasurements.toList()).also { currentMeasurements.clear() }
  }

  fun addMeasurement(measurement: TimeMeasurement) {
    currentMeasurements.add(measurement)
  }

  @OptIn(ExperimentalContracts::class)
  fun simpleMeasure(name: String, block: () -> Unit) {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    val start = ServerStats.getCurrentTimestamp()
    block()
    val end = ServerStats.getCurrentTimestamp()
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
  fun getCurrentTimestamp() = System.currentTimeMillis() - globalStartTime

  fun setStatsDumpCountdown() = Timer().schedule(60000) {
    CreateUpdateStats.dumpStats()
    MemoryStats.dumpStats()
    NetworkStats.dumpStats()
  }

  object CreateUpdateStats : Stats("Create update loop") {
    val file = File("stats/kek.csv")
    private const val timeThreshold: Long = 25
    private val interestingMeasurements = mutableListOf<TimeMeasurement>()

    override fun endMeasurement(): TimeMeasurement = super.endMeasurement().also {
      if (it.end - it.start > timeThreshold)
        synchronized(interestingMeasurements) { interestingMeasurements.add(it) }
    }

    fun dumpStats() = synchronized(interestingMeasurements) {
      println("Dumping to ${file.absolutePath}")
      file.printWriter().use { out ->
        out.println("timestamp,task,len")
        interestingMeasurements.forEach {
          val timestamp = it.start
          out.print(it.toCsvString("$timestamp,").removeSuffix(","))
        }
      }
    }

  }

  object CreateDataStats : Stats("Create data to send")

  object MemoryStats {
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
      val timeNow = getCurrentTimestamp()
      synchronized(usages) {
        usages.add(MemoryUsage(timeNow, totalMemory, usedMemory))
      }
    }

    val file = File("stats/memory.csv")
    fun dumpStats() = synchronized(usages) {
      file.printWriter().use { out ->
        out.println("timestamp,type,value")
        usages.forEach {
          out.print(it.toCsvString())
        }
      }
    }
  }

  object NetworkStats {
    data class SentPacket(val timestamp: Long, val byteSize: Long) {
      fun toCsvString(): String {
        return "${timestamp},$byteSize\n"
      }
    }

    private val packets = mutableListOf<SentPacket>()

    fun add(timestamp: Long, byteSize: Long) = synchronized(packets) {
      packets.add(SentPacket(timestamp, byteSize))
    }

    val file = File("stats/network.csv")
    fun dumpStats() = synchronized(packets) {
      file.printWriter().use { out ->
        out.println("timestamp,bytes")
        packets.forEach { out.print(it.toCsvString()) }
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
