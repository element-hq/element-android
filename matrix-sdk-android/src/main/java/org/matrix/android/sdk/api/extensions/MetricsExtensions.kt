/*
 * Copyright (c) 2022 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.api.extensions

import org.matrix.android.sdk.api.metrics.MetricPlugin
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Executes the given [block] while measuring the transaction.
 */
@OptIn(ExperimentalContracts::class)
public inline fun <T> measureMetric(metricMeasurementPlugins: List<MetricPlugin>, block: () -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    metricMeasurementPlugins.forEach { plugin -> plugin.startTransaction() }
    val answer = block()
    metricMeasurementPlugins.forEach { plugin -> plugin.finishTransaction() }
    return answer
}
