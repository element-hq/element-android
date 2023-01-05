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

package im.vector.app.features.settings.devices.v2

import im.vector.app.test.fakes.FakeActiveSessionHolder
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.mockk.verifyAll
import org.junit.Test
import org.matrix.android.sdk.api.NoOpMatrixCallback

class RefreshDevicesUseCaseTest {

    private val fakeActiveSessionHolder = FakeActiveSessionHolder()

    private val refreshDevicesUseCase = RefreshDevicesUseCase(
            activeSessionHolder = fakeActiveSessionHolder.instance
    )

    @Test
    fun `given current session when refreshing then devices list and keys are fetched`() {
        val session = fakeActiveSessionHolder.fakeSession
        every { session.cryptoService().fetchDevicesList(any()) } just runs
        every { session.cryptoService().downloadKeys(any(), any(), any()) } just runs

        refreshDevicesUseCase.execute()

        verifyAll {
            session.cryptoService().fetchDevicesList(match { it is NoOpMatrixCallback })
            session.cryptoService().downloadKeys(listOf(session.myUserId), true, match { it is NoOpMatrixCallback })
        }
    }
}
