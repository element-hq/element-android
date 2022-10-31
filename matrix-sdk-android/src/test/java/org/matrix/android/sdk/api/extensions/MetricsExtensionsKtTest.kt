/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.matrix.android.sdk.api.metrics.MetricPlugin

internal class MetricsExtensionsKtTest {

    private val plugin = mockk<MetricPlugin> {
        every { startTransaction() } answers {}
        every { finishTransaction() } answers {}
    }

    @Test
    fun `Given plugins are invoked when calling measureMetric`() {
        measureMetric(listOf(plugin)) {
            println("measureMetric test")
        }

        verify { plugin.startTransaction() }
    }
}
