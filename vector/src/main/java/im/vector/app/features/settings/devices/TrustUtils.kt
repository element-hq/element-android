/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.settings.devices

import org.matrix.android.sdk.api.session.crypto.crosssigning.DeviceTrustLevel
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel

object TrustUtils {

    fun shieldForTrust(currentDevice: Boolean,
                       trustMSK: Boolean,
                       legacyMode: Boolean,
                       deviceTrustLevel: DeviceTrustLevel?): RoomEncryptionTrustLevel {
        return when {
            currentDevice -> {
                if (legacyMode) {
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
            else          -> {
                if (legacyMode) {
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

                            deviceTrustLevel?.locallyVerified == true      -> RoomEncryptionTrustLevel.Default
                            else                                           -> RoomEncryptionTrustLevel.Warning
                        }
                    } else {
                        // The current session is untrusted, so displays others in black
                        // as we can't know the cross-signing state
                        RoomEncryptionTrustLevel.Default
                    }
                }
            }
        }
    }
}
