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

package im.vector.app.features.settings.devices.v2.overview

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import im.vector.app.features.settings.devices.DeviceFullInfo
import im.vector.app.test.fakes.FakeActiveSessionHolder
import im.vector.app.test.fakes.FakeFlowLiveDataConversions
import im.vector.app.test.fakes.givenAsFlow
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.DeviceInfo
import org.matrix.android.sdk.api.util.Optional

private const val A_DEVICE_ID = "device-id"

class GetDeviceFullInfoUseCaseTest {

    private val fakeActiveSessionHolder = FakeActiveSessionHolder()
    private val fakeFlowLiveDataConversions = FakeFlowLiveDataConversions()

    private val getDeviceFullInfoUseCase = GetDeviceFullInfoUseCase(
            activeSessionHolder = fakeActiveSessionHolder.instance
    )

    @Before
    fun setUp() {
        fakeFlowLiveDataConversions.setup()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `given an active session and info for device when getting device info then the result is correct`() = runTest {
        val deviceInfo = DeviceInfo()
        fakeActiveSessionHolder.fakeSession.fakeCryptoService.myDevicesInfoWithIdLiveData = MutableLiveData(Optional(deviceInfo))
        fakeActiveSessionHolder.fakeSession.fakeCryptoService.myDevicesInfoWithIdLiveData.givenAsFlow()
        val cryptoDeviceInfo = CryptoDeviceInfo(deviceId = A_DEVICE_ID, userId = "")
        fakeActiveSessionHolder.fakeSession.fakeCryptoService.cryptoDeviceInfoWithIdLiveData = MutableLiveData(Optional(cryptoDeviceInfo))
        fakeActiveSessionHolder.fakeSession.fakeCryptoService.cryptoDeviceInfoWithIdLiveData.givenAsFlow()

        val deviceFullInfo = getDeviceFullInfoUseCase.execute(A_DEVICE_ID).firstOrNull()

        deviceFullInfo shouldBeEqualTo Optional(DeviceFullInfo(deviceInfo = deviceInfo, cryptoDeviceInfo = cryptoDeviceInfo))
        verify { fakeActiveSessionHolder.instance.getSafeActiveSession() }
        verify { fakeActiveSessionHolder.fakeSession.fakeCryptoService.getMyDevicesInfoLive(A_DEVICE_ID).asFlow() }
        verify { fakeActiveSessionHolder.fakeSession.fakeCryptoService.getLiveCryptoDeviceInfoWithId(A_DEVICE_ID).asFlow() }
    }

    @Test
    fun `given an active session and no info for device when getting device info then the result is null`() = runTest {
        fakeActiveSessionHolder.fakeSession.fakeCryptoService.myDevicesInfoWithIdLiveData = MutableLiveData(Optional(null))
        fakeActiveSessionHolder.fakeSession.fakeCryptoService.myDevicesInfoWithIdLiveData.givenAsFlow()
        fakeActiveSessionHolder.fakeSession.fakeCryptoService.cryptoDeviceInfoWithIdLiveData = MutableLiveData(Optional(null))
        fakeActiveSessionHolder.fakeSession.fakeCryptoService.cryptoDeviceInfoWithIdLiveData.givenAsFlow()

        val deviceFullInfo = getDeviceFullInfoUseCase.execute(A_DEVICE_ID).firstOrNull()

        deviceFullInfo shouldBeEqualTo Optional(null)
        verify { fakeActiveSessionHolder.instance.getSafeActiveSession() }
        verify { fakeActiveSessionHolder.fakeSession.fakeCryptoService.getMyDevicesInfoLive(A_DEVICE_ID).asFlow() }
        verify { fakeActiveSessionHolder.fakeSession.fakeCryptoService.getLiveCryptoDeviceInfoWithId(A_DEVICE_ID).asFlow() }
    }

    @Test
    fun `given no active session when getting device info then the result is empty`() = runTest {
        fakeActiveSessionHolder.givenGetSafeActiveSessionReturns(null)

        val deviceFullInfo = getDeviceFullInfoUseCase.execute(A_DEVICE_ID).firstOrNull()

        deviceFullInfo shouldBeEqualTo null
        verify { fakeActiveSessionHolder.instance.getSafeActiveSession() }
    }
}
