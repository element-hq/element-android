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

package org.matrix.android.sdk.test.fakes

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.matrix.android.sdk.internal.session.room.state.SendStateTask

internal class FakeSendStateTask : SendStateTask by mockk() {

    fun givenExecuteRetryReturns(eventId: String) {
        coEvery { executeRetry(any(), any()) } returns eventId
    }

    fun givenExecuteRetryThrows(error: Throwable) {
        coEvery { executeRetry(any(), any()) } throws error
    }

    fun verifyExecuteRetry(params: SendStateTask.Params, remainingRetry: Int) {
        coVerify { executeRetry(params, remainingRetry) }
    }
}
