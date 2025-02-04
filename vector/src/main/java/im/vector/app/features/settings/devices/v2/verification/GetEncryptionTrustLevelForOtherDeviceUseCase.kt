/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.verification

import org.matrix.android.sdk.api.session.crypto.crosssigning.DeviceTrustLevel
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel
import javax.inject.Inject

class GetEncryptionTrustLevelForOtherDeviceUseCase @Inject constructor() {

    fun execute(trustMSK: Boolean, legacyMode: Boolean, deviceTrustLevel: DeviceTrustLevel?): RoomEncryptionTrustLevel {
        return if (legacyMode) {
            // use local trust
            if (deviceTrustLevel?.locallyVerified == true) {
                RoomEncryptionTrustLevel.Trusted
            } else {
                RoomEncryptionTrustLevel.Warning
            }
        } else {
            if (trustMSK) {
                // use cross sign trust, put locally trusted in black
                when {
                    deviceTrustLevel?.crossSigningVerified == true -> RoomEncryptionTrustLevel.Trusted
                    deviceTrustLevel?.locallyVerified == true -> RoomEncryptionTrustLevel.Default
                    else -> RoomEncryptionTrustLevel.Warning
                }
            } else {
                // The current session is untrusted, so displays others in black
                // as we can't know the cross-signing state
                RoomEncryptionTrustLevel.Default
            }
        }
    }
}
