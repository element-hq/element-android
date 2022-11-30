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

import im.vector.app.features.settings.devices.v2.DeviceFullInfo
import im.vector.app.features.settings.devices.v2.details.extended.DeviceExtendedInfo
import im.vector.app.features.settings.devices.v2.list.DeviceType
import org.matrix.android.sdk.api.extensions.orFalse
import javax.inject.Inject

class CheckIfSectionDeviceIsVisibleUseCase @Inject constructor() {

    fun execute(deviceFullInfo: DeviceFullInfo): Boolean {
        val hasExtendedInfo = when (deviceFullInfo.deviceExtendedInfo.deviceType) {
            DeviceType.MOBILE -> hasAnyDeviceExtendedInfoMobile(deviceFullInfo.deviceExtendedInfo)
            DeviceType.WEB -> hasAnyDeviceExtendedInfoWeb(deviceFullInfo.deviceExtendedInfo)
            DeviceType.DESKTOP -> hasAnyDeviceExtendedInfoDesktop(deviceFullInfo.deviceExtendedInfo)
            DeviceType.UNKNOWN -> false
        }

        return hasExtendedInfo || deviceFullInfo.deviceInfo.lastSeenIp?.isNotEmpty().orFalse()
    }

    private fun hasAnyDeviceExtendedInfoMobile(deviceExtendedInfo: DeviceExtendedInfo): Boolean {
        return deviceExtendedInfo.deviceModel?.isNotEmpty().orFalse() ||
                deviceExtendedInfo.deviceOperatingSystem?.isNotEmpty().orFalse()
    }

    private fun hasAnyDeviceExtendedInfoWeb(deviceExtendedInfo: DeviceExtendedInfo): Boolean {
        return deviceExtendedInfo.clientName?.isNotEmpty().orFalse() ||
                deviceExtendedInfo.deviceOperatingSystem?.isNotEmpty().orFalse()
    }

    private fun hasAnyDeviceExtendedInfoDesktop(deviceExtendedInfo: DeviceExtendedInfo): Boolean {
        return deviceExtendedInfo.deviceOperatingSystem?.isNotEmpty().orFalse()
    }
}
