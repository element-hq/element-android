/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.verification

import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel
import javax.inject.Inject

class GetEncryptionTrustLevelForCurrentDeviceUseCase @Inject constructor() {

    fun execute(trustMSK: Boolean, legacyMode: Boolean): RoomEncryptionTrustLevel {
        return if (legacyMode) {
            // In legacy, current session is always trusted
            RoomEncryptionTrustLevel.Trusted
        } else {
            // If current session doesn't trust MSK, show red shield for current device
            if (trustMSK) {
                RoomEncryptionTrustLevel.Trusted
            } else {
                RoomEncryptionTrustLevel.Warning
            }
        }
    }
}
