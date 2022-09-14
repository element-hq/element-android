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

package im.vector.app.features.settings.devices.v2.details

import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.crypto.model.DeviceInfo
import javax.inject.Inject

class CheckIfSectionSessionIsVisibleUseCase @Inject constructor() {

    fun execute(deviceInfo: DeviceInfo): Boolean {
        return deviceInfo.displayName?.isNotEmpty().orFalse() ||
                deviceInfo.deviceId?.isNotEmpty().orFalse() ||
                (deviceInfo.lastSeenTs ?: 0) > 0
    }
}
