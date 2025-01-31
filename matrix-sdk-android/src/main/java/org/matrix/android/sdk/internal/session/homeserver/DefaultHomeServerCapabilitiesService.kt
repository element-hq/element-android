/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.homeserver

import org.matrix.android.sdk.api.session.homeserver.HomeServerCapabilities
import org.matrix.android.sdk.api.session.homeserver.HomeServerCapabilitiesService
import javax.inject.Inject

internal class DefaultHomeServerCapabilitiesService @Inject constructor(
        private val homeServerCapabilitiesDataSource: HomeServerCapabilitiesDataSource,
        private val getHomeServerCapabilitiesTask: GetHomeServerCapabilitiesTask
) : HomeServerCapabilitiesService {

    override suspend fun refreshHomeServerCapabilities() {
        getHomeServerCapabilitiesTask.execute(GetHomeServerCapabilitiesTask.Params(forceRefresh = true))
    }

    override fun getHomeServerCapabilities(): HomeServerCapabilities {
        return homeServerCapabilitiesDataSource.getHomeServerCapabilities()
                ?: HomeServerCapabilities()
    }
}
