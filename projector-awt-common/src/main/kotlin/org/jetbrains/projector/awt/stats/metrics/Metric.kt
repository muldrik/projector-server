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


// At the moment all metrics can be described by a Long value. Change if needed
abstract class Metric(val threshold: Long = 0, val objectsThreshold: Int = 0, val name: String) {
  abstract fun add(value: Long, processedObjects: Int = 1)
  abstract fun result(): Long
  open fun csvParamNames(): String = "Time threshold=${threshold};Objects threshold=${objectsThreshold}" // Used to generate CSV file and make and Excel sheet with it
  open var measurementUnit = "" // Override or change with .let{...} to specify the unit of measurement
  open fun csvResult(): String = name + "," + csvParamNames() + "," + measurementUnit + "," + result()
  companion object {
    fun csvHeader(): String = "Name,Params,Measurement Unit,Value"
  }
}
