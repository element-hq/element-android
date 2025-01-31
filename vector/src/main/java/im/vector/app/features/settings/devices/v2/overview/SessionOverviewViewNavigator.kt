/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.overview

import android.content.Context
import im.vector.app.features.settings.devices.v2.details.SessionDetailsActivity
import javax.inject.Inject

class SessionOverviewViewNavigator @Inject constructor() {

    fun navigateToSessionDetails(context: Context, deviceId: String) {
        context.startActivity(SessionDetailsActivity.newIntent(context, deviceId))
    }
}
