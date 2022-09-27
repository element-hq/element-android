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

import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel

class GetEncryptionTrustLevelForCurrentDeviceUseCaseTest {

    private val getEncryptionTrustLevelForCurrentDeviceUseCase = GetEncryptionTrustLevelForCurrentDeviceUseCase()

    @Test
    fun `given in legacy mode when computing trust level then device is trusted`() {
        val trustMSK = false
        val legacyMode = true

        val result = getEncryptionTrustLevelForCurrentDeviceUseCase.execute(trustMSK = trustMSK, legacyMode = legacyMode)

        result shouldBeEqualTo RoomEncryptionTrustLevel.Trusted
    }

    @Test
    fun `given trustMSK is true and not in legacy mode when computing trust level then device is trusted`() {
        val trustMSK = true
        val legacyMode = false

        val result = getEncryptionTrustLevelForCurrentDeviceUseCase.execute(trustMSK = trustMSK, legacyMode = legacyMode)

        result shouldBeEqualTo RoomEncryptionTrustLevel.Trusted
    }

    @Test
    fun `given trustMSK is false and not in legacy mode when computing trust level then device is unverified`() {
        val trustMSK = false
        val legacyMode = false

        val result = getEncryptionTrustLevelForCurrentDeviceUseCase.execute(trustMSK = trustMSK, legacyMode = legacyMode)

        result shouldBeEqualTo RoomEncryptionTrustLevel.Warning
    }
}
