/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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
import io.mockk.slot
import org.amshove.kluent.shouldBeEqualTo
import org.matrix.android.sdk.internal.session.filter.Filter
import org.matrix.android.sdk.internal.session.filter.SaveFilterTask
import java.util.UUID

internal class FakeSaveFilterTask : SaveFilterTask by mockk() {

    init {
        coEvery { execute(any()) } returns UUID.randomUUID().toString()
    }

    fun verifyExecution(filter: Filter) {
        val slot = slot<SaveFilterTask.Params>()
        coVerify { execute(capture(slot)) }
        val params = slot.captured
        params.filter shouldBeEqualTo filter
    }
}
