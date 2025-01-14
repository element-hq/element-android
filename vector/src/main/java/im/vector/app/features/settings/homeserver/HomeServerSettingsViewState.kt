/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.homeserver

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import org.matrix.android.sdk.api.federation.FederationVersion
import org.matrix.android.sdk.api.session.homeserver.HomeServerCapabilities

data class HomeServerSettingsViewState(
        val homeserverUrl: String = "",
        val homeserverClientServerApiUrl: String = "",
        val homeServerCapabilities: HomeServerCapabilities = HomeServerCapabilities(),
        val federationVersion: Async<FederationVersion> = Uninitialized
) : MavericksState
