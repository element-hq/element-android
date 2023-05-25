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

import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel
import javax.inject.Inject

class GetEncryptionTrustLevelForDeviceUseCase @Inject constructor(
        private val getEncryptionTrustLevelForCurrentDeviceUseCase: GetEncryptionTrustLevelForCurrentDeviceUseCase,
        private val getEncryptionTrustLevelForOtherDeviceUseCase: GetEncryptionTrustLevelForOtherDeviceUseCase,
) {

    // XXX why is this using the RoomEncryptionTrustLevel?
    // should be using a new DeviceTrustShield enum
    fun execute(currentSessionCrossSigningInfo: CurrentSessionCrossSigningInfo, cryptoDeviceInfo: CryptoDeviceInfo?): RoomEncryptionTrustLevel? {
        if (cryptoDeviceInfo == null) {
            return null
        }

        val legacyMode = !currentSessionCrossSigningInfo.isCrossSigningInitialized
        val trustMSK = currentSessionCrossSigningInfo.isCrossSigningVerified
        val isCurrentDevice = !cryptoDeviceInfo.deviceId.isNullOrEmpty() && cryptoDeviceInfo.deviceId == currentSessionCrossSigningInfo.deviceId
        val deviceTrustLevel = cryptoDeviceInfo.trustLevel

        return when {
            isCurrentDevice -> getEncryptionTrustLevelForCurrentDeviceUseCase.execute(trustMSK, legacyMode)
            else -> getEncryptionTrustLevelForOtherDeviceUseCase.execute(trustMSK, legacyMode, deviceTrustLevel)
        }
    }
}
