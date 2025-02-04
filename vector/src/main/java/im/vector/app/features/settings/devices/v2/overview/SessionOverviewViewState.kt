/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.overview

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import im.vector.app.features.settings.devices.v2.DeviceFullInfo
import im.vector.app.features.settings.devices.v2.notification.NotificationsStatus

data class SessionOverviewViewState(
        val deviceId: String,
        val isCurrentSessionTrusted: Boolean = false,
        val deviceInfo: Async<DeviceFullInfo> = Uninitialized,
        val isLoading: Boolean = false,
        val notificationsStatus: NotificationsStatus = NotificationsStatus.NOT_SUPPORTED,
        val isShowingIpAddress: Boolean = false,
        val externalAccountManagementUrl: String? = null,
) : MavericksState {
    constructor(args: SessionOverviewArgs) : this(
            deviceId = args.deviceId
    )
}
