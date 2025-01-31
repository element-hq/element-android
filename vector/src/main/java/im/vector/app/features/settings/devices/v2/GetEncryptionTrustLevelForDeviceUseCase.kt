/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2

import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel
import javax.inject.Inject

class GetEncryptionTrustLevelForDeviceUseCase @Inject constructor(
        private val getEncryptionTrustLevelForCurrentDeviceUseCase: GetEncryptionTrustLevelForCurrentDeviceUseCase,
        private val getEncryptionTrustLevelForOtherDeviceUseCase: GetEncryptionTrustLevelForOtherDeviceUseCase,
) {

    fun execute(currentSessionCrossSigningInfo: CurrentSessionCrossSigningInfo, cryptoDeviceInfo: CryptoDeviceInfo?): RoomEncryptionTrustLevel {
        val legacyMode = !currentSessionCrossSigningInfo.isCrossSigningInitialized
        val trustMSK = currentSessionCrossSigningInfo.isCrossSigningVerified
        val isCurrentDevice = !cryptoDeviceInfo?.deviceId.isNullOrEmpty() && cryptoDeviceInfo?.deviceId == currentSessionCrossSigningInfo.deviceId
        val deviceTrustLevel = cryptoDeviceInfo?.trustLevel

        return when {
            isCurrentDevice -> getEncryptionTrustLevelForCurrentDeviceUseCase.execute(trustMSK, legacyMode)
            else -> getEncryptionTrustLevelForOtherDeviceUseCase.execute(trustMSK, legacyMode, deviceTrustLevel)
        }
    }
}
