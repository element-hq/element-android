/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
