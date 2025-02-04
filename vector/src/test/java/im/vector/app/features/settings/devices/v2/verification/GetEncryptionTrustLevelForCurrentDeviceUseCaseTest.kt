/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
