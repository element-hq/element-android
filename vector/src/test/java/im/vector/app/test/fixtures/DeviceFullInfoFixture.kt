/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.test.fixtures

import im.vector.app.features.settings.devices.v2.DeviceFullInfo
import im.vector.app.features.settings.devices.v2.details.extended.DeviceExtendedInfo
import im.vector.app.features.settings.devices.v2.list.DeviceType
import org.matrix.android.sdk.api.session.crypto.model.DeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel

fun aDeviceFullInfo(deviceId: String, isSelected: Boolean): DeviceFullInfo {
    return DeviceFullInfo(
            deviceInfo = DeviceInfo(
                    deviceId = deviceId,
            ),
            cryptoDeviceInfo = null,
            roomEncryptionTrustLevel = RoomEncryptionTrustLevel.Trusted,
            isInactive = true,
            isCurrentDevice = true,
            deviceExtendedInfo = DeviceExtendedInfo(
                    deviceType = DeviceType.MOBILE,
            ),
            matrixClientInfo = null,
            isSelected = isSelected,
    )
}
