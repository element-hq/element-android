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

import androidx.annotation.DrawableRes
import im.vector.app.R
import org.matrix.android.sdk.internal.crypto.crosssigning.DeviceTrustLevel

object TrustUtils {

    @DrawableRes
    fun shieldForTrust(currentDevice: Boolean, trustMSK: Boolean, legacyMode: Boolean, deviceTrustLevel: DeviceTrustLevel?): Int {
        return when {
            currentDevice -> {
                if (legacyMode) {
                    // In legacy, current session is always trusted
                    R.drawable.ic_shield_trusted
                } else {
                    // If current session doesn't trust MSK, show red shield for current device
                    R.drawable.ic_shield_trusted.takeIf { trustMSK } ?: R.drawable.ic_shield_warning
                }
            }
            else          -> {
                if (legacyMode) {
                    // use local trust
                    R.drawable.ic_shield_trusted.takeIf { deviceTrustLevel?.locallyVerified == true } ?: R.drawable.ic_shield_warning
                } else {
                    if (trustMSK) {
                        // use cross sign trust, put locally trusted in black
                        R.drawable.ic_shield_trusted.takeIf { deviceTrustLevel?.crossSigningVerified == true }
                                ?: R.drawable.ic_shield_black.takeIf { deviceTrustLevel?.locallyVerified == true }
                                ?: R.drawable.ic_shield_warning
                    } else {
                        // The current session is untrusted, so displays others in black
                        // as we can't know the cross-signing state
                        R.drawable.ic_shield_black
                    }
                }
            }
        }
    }
}
