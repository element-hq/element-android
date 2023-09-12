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
