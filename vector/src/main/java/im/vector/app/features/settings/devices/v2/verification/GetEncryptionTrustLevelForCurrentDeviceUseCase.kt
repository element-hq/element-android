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
