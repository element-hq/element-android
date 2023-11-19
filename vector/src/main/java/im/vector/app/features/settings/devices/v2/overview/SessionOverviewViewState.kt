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
