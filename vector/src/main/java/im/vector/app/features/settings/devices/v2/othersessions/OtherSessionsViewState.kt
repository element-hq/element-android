/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.othersessions

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import im.vector.app.features.settings.devices.v2.DeviceFullInfo
import im.vector.app.features.settings.devices.v2.filter.DeviceManagerFilterType

data class OtherSessionsViewState(
        val devices: Async<List<DeviceFullInfo>> = Uninitialized,
        val currentFilter: DeviceManagerFilterType = DeviceManagerFilterType.ALL_SESSIONS,
        val excludeCurrentDevice: Boolean = false,
        val isSelectModeEnabled: Boolean = false,
        val isLoading: Boolean = false,
        val isShowingIpAddress: Boolean = false,
        val delegatedOidcAuthEnabled: Boolean = false,
) : MavericksState {

    constructor(args: OtherSessionsArgs) : this(excludeCurrentDevice = args.excludeCurrentDevice)
}
