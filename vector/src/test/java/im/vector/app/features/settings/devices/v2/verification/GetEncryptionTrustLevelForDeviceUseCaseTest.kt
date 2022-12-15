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

package im.vector.app.features.settings.devices.v2.verification

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import org.matrix.android.sdk.api.session.crypto.crosssigning.DeviceTrustLevel
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel

private const val A_DEVICE_ID = "device-id"
private const val A_DEVICE_ID_2 = "device-id-2"

class GetEncryptionTrustLevelForDeviceUseCaseTest {

    private val getEncryptionTrustLevelForCurrentDeviceUseCase = mockk<GetEncryptionTrustLevelForCurrentDeviceUseCase>()
    private val getEncryptionTrustLevelForOtherDeviceUseCase = mockk<GetEncryptionTrustLevelForOtherDeviceUseCase>()

    private val getEncryptionTrustLevelForDeviceUseCase = GetEncryptionTrustLevelForDeviceUseCase(
            getEncryptionTrustLevelForCurrentDeviceUseCase = getEncryptionTrustLevelForCurrentDeviceUseCase,
            getEncryptionTrustLevelForOtherDeviceUseCase = getEncryptionTrustLevelForOtherDeviceUseCase,
    )

    @Test
    fun `given is current device when computing trust level then the correct sub use case result is returned`() {
        val currentSessionCrossSigningInfo = givenCurrentSessionCrossSigningInfo(
                deviceId = A_DEVICE_ID,
                isCrossSigningInitialized = true,
                isCrossSigningVerified = false
        )
        val cryptoDeviceInfo = givenCryptoDeviceInfo(
                deviceId = A_DEVICE_ID,
                trustLevel = null
        )
        val trustLevel = RoomEncryptionTrustLevel.Trusted
        every { getEncryptionTrustLevelForCurrentDeviceUseCase.execute(any(), any()) } returns trustLevel

        val result = getEncryptionTrustLevelForDeviceUseCase.execute(currentSessionCrossSigningInfo, cryptoDeviceInfo)

        result shouldBeEqualTo trustLevel
        verify {
            getEncryptionTrustLevelForCurrentDeviceUseCase.execute(
                    trustMSK = currentSessionCrossSigningInfo.isCrossSigningVerified,
                    legacyMode = !currentSessionCrossSigningInfo.isCrossSigningInitialized
            )
        }
    }

    @Test
    fun `given is not current device when computing trust level then the correct sub use case result is returned`() {
        val currentSessionCrossSigningInfo = givenCurrentSessionCrossSigningInfo(
                deviceId = A_DEVICE_ID,
                isCrossSigningInitialized = true,
                isCrossSigningVerified = false
        )
        val cryptoDeviceInfo = givenCryptoDeviceInfo(
                deviceId = A_DEVICE_ID_2,
                trustLevel = null
        )
        val trustLevel = RoomEncryptionTrustLevel.Trusted
        every { getEncryptionTrustLevelForOtherDeviceUseCase.execute(any(), any(), any()) } returns trustLevel

        val result = getEncryptionTrustLevelForDeviceUseCase.execute(currentSessionCrossSigningInfo, cryptoDeviceInfo)

        result shouldBeEqualTo trustLevel
        verify {
            getEncryptionTrustLevelForOtherDeviceUseCase.execute(
                    trustMSK = currentSessionCrossSigningInfo.isCrossSigningVerified,
                    legacyMode = !currentSessionCrossSigningInfo.isCrossSigningInitialized,
                    deviceTrustLevel = cryptoDeviceInfo.trustLevel
            )
        }
    }

    @Test
    fun `given no crypto device info when computing trust level then result is null`() {
        val currentSessionCrossSigningInfo = givenCurrentSessionCrossSigningInfo(
                deviceId = A_DEVICE_ID,
                isCrossSigningInitialized = true,
                isCrossSigningVerified = false
        )
        val cryptoDeviceInfo = null

        val result = getEncryptionTrustLevelForDeviceUseCase.execute(currentSessionCrossSigningInfo, cryptoDeviceInfo)

        result shouldBe null
    }

    private fun givenCurrentSessionCrossSigningInfo(
            deviceId: String,
            isCrossSigningInitialized: Boolean,
            isCrossSigningVerified: Boolean
    ): CurrentSessionCrossSigningInfo {
        return CurrentSessionCrossSigningInfo(
                deviceId = deviceId,
                isCrossSigningInitialized = isCrossSigningInitialized,
                isCrossSigningVerified = isCrossSigningVerified
        )
    }

    private fun givenCryptoDeviceInfo(
            deviceId: String,
            trustLevel: DeviceTrustLevel?
    ): CryptoDeviceInfo {
        return CryptoDeviceInfo(
                userId = "",
                deviceId = deviceId,
                trustLevel = trustLevel
        )
    }
}
