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

package im.vector.app.features.settings.devices.v2

import android.content.Context
import im.vector.app.features.settings.devices.v2.filter.DeviceManagerFilterType
import im.vector.app.features.settings.devices.v2.othersessions.OtherSessionsActivity
import im.vector.app.features.settings.devices.v2.overview.SessionOverviewActivity
import im.vector.app.features.settings.devices.v2.rename.RenameSessionActivity
import javax.inject.Inject

class VectorSettingsDevicesViewNavigator @Inject constructor() {

    fun navigateToSessionOverview(context: Context, deviceId: String) {
        context.startActivity(SessionOverviewActivity.newIntent(context, deviceId))
    }

    fun navigateToOtherSessions(
            context: Context,
            defaultFilter: DeviceManagerFilterType,
            excludeCurrentDevice: Boolean,
    ) {
        context.startActivity(
                OtherSessionsActivity.newIntent(context, defaultFilter, excludeCurrentDevice)
        )
    }

    fun navigateToRenameSession(context: Context, deviceId: String) {
        context.startActivity(RenameSessionActivity.newIntent(context, deviceId))
    }
}
