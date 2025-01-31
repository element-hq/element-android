/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2

import android.content.Context
import im.vector.app.features.settings.devices.v2.othersessions.OtherSessionsActivity
import im.vector.app.features.settings.devices.v2.overview.SessionOverviewActivity
import javax.inject.Inject

class VectorSettingsDevicesViewNavigator @Inject constructor() {

    fun navigateToSessionOverview(context: Context, deviceId: String) {
        context.startActivity(SessionOverviewActivity.newIntent(context, deviceId))
    }

    fun navigateToOtherSessions(context: Context) {
        context.startActivity(OtherSessionsActivity.newIntent(context))
    }
}
