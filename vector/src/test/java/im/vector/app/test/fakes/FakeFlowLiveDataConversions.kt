/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fakes

import androidx.lifecycle.LiveData
import androidx.lifecycle.asFlow
import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeFlowLiveDataConversions {
    fun setup() {
        mockkStatic("androidx.lifecycle.FlowLiveDataConversions")
    }
}

fun <T> LiveData<T>.givenAsFlow(): Flow<T> {
    return flowOf(value!!).also {
        every { asFlow() } returns it
    }
}
