/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
