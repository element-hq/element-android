/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.filter

import im.vector.app.features.settings.devices.v2.DeviceFullInfo
import org.matrix.android.sdk.api.extensions.orFalse
import javax.inject.Inject

class FilterDevicesUseCase @Inject constructor() {

    fun execute(
            devices: List<DeviceFullInfo>,
            filterType: DeviceManagerFilterType,
            excludedDeviceIds: List<String> = emptyList(),
    ): List<DeviceFullInfo> {
        return devices
                .filter {
                    when (filterType) {
                        DeviceManagerFilterType.ALL_SESSIONS -> true
                        DeviceManagerFilterType.VERIFIED -> it.cryptoDeviceInfo?.isVerified.orFalse()
                        DeviceManagerFilterType.UNVERIFIED -> !it.cryptoDeviceInfo?.isVerified.orFalse()
                        DeviceManagerFilterType.INACTIVE -> it.isInactive
                    }
                }
                .filter { it.deviceInfo.deviceId !in excludedDeviceIds }
    }
}
