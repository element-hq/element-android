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
import io.mockk.coEvery
import io.mockk.coVerifyAll
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.matrix.android.sdk.api.session.crypto.model.MXUsersDevicesMap

class RefreshDevicesUseCaseTest {

    private val fakeActiveSessionHolder = FakeActiveSessionHolder()

    private val refreshDevicesUseCase = RefreshDevicesUseCase(
            activeSessionHolder = fakeActiveSessionHolder.instance
    )

    @Test
    fun `given current session when refreshing then devices list and keys are fetched`() {
        val session = fakeActiveSessionHolder.fakeSession
        coEvery { session.cryptoService().fetchDevicesList() } returns emptyList()
        coEvery { session.cryptoService().downloadKeysIfNeeded(any(), any()) } returns MXUsersDevicesMap()

        runBlocking {
            refreshDevicesUseCase.execute()
        }

        coVerifyAll {
            session.cryptoService().fetchDevicesList()
            session.cryptoService().downloadKeysIfNeeded(listOf(session.myUserId), true)
        }
    }
}
