/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.overview

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import im.vector.app.features.settings.devices.v2.DeviceFullInfo

data class SessionOverviewViewState(
        val deviceId: String,
        val isCurrentSession: Boolean = false,
        val deviceInfo: Async<DeviceFullInfo> = Uninitialized,
) : MavericksState {
    constructor(args: SessionOverviewArgs) : this(
            deviceId = args.deviceId
    )
}
