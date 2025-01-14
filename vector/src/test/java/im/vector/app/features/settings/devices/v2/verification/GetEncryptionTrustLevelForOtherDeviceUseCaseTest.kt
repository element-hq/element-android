/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.verification

import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test
import org.matrix.android.sdk.api.session.crypto.crosssigning.DeviceTrustLevel
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel

class GetEncryptionTrustLevelForOtherDeviceUseCaseTest {

    private val getEncryptionTrustLevelForOtherDeviceUseCase = GetEncryptionTrustLevelForOtherDeviceUseCase()

    @Test
    fun `given in legacy mode and device locally verified when computing trust level then device is trusted`() {
        val trustMSK = false
        val legacyMode = true
        val deviceTrustLevel = givenDeviceTrustLevel(locallyVerified = true, crossSigningVerified = false)

        val result = getEncryptionTrustLevelForOtherDeviceUseCase.execute(trustMSK = trustMSK, legacyMode = legacyMode, deviceTrustLevel = deviceTrustLevel)

        result shouldBeEqualTo RoomEncryptionTrustLevel.Trusted
    }

    @Test
    fun `given in legacy mode and device not locally verified when computing trust level then device is unverified`() {
        val trustMSK = false
        val legacyMode = true
        val deviceTrustLevel = givenDeviceTrustLevel(locallyVerified = false, crossSigningVerified = false)

        val result = getEncryptionTrustLevelForOtherDeviceUseCase.execute(trustMSK = trustMSK, legacyMode = legacyMode, deviceTrustLevel = deviceTrustLevel)

        result shouldBeEqualTo RoomEncryptionTrustLevel.Warning
    }

    @Test
    fun `given trustMSK is true and not in legacy mode and device cross signing verified when computing trust level then device is trusted`() {
        val trustMSK = true
        val legacyMode = false
        val deviceTrustLevel = givenDeviceTrustLevel(locallyVerified = false, crossSigningVerified = true)

        val result = getEncryptionTrustLevelForOtherDeviceUseCase.execute(trustMSK = trustMSK, legacyMode = legacyMode, deviceTrustLevel = deviceTrustLevel)

        result shouldBeEqualTo RoomEncryptionTrustLevel.Trusted
    }

    @Test
    fun `given trustMSK is true and not in legacy mode and device locally verified when computing trust level then device has default trust level`() {
        val trustMSK = true
        val legacyMode = false
        val deviceTrustLevel = givenDeviceTrustLevel(locallyVerified = true, crossSigningVerified = false)

        val result = getEncryptionTrustLevelForOtherDeviceUseCase.execute(trustMSK = trustMSK, legacyMode = legacyMode, deviceTrustLevel = deviceTrustLevel)

        result shouldBeEqualTo RoomEncryptionTrustLevel.Default
    }

    @Test
    fun `given trustMSK is true and not in legacy mode and device not verified when computing trust level then device is unverified`() {
        val trustMSK = true
        val legacyMode = false
        val deviceTrustLevel = givenDeviceTrustLevel(locallyVerified = false, crossSigningVerified = false)

        val result = getEncryptionTrustLevelForOtherDeviceUseCase.execute(trustMSK = trustMSK, legacyMode = legacyMode, deviceTrustLevel = deviceTrustLevel)

        result shouldBeEqualTo RoomEncryptionTrustLevel.Warning
    }

    @Test
    fun `given trustMSK is false and not in legacy mode when computing trust level then device has default trust level`() {
        val trustMSK = false
        val legacyMode = false
        val deviceTrustLevel = givenDeviceTrustLevel(locallyVerified = false, crossSigningVerified = false)

        val result = getEncryptionTrustLevelForOtherDeviceUseCase.execute(trustMSK = trustMSK, legacyMode = legacyMode, deviceTrustLevel = deviceTrustLevel)

        result shouldBeEqualTo RoomEncryptionTrustLevel.Default
    }

    private fun givenDeviceTrustLevel(locallyVerified: Boolean?, crossSigningVerified: Boolean): DeviceTrustLevel {
        return DeviceTrustLevel(
                crossSigningVerified = crossSigningVerified,
                locallyVerified = locallyVerified
        )
    }
}
