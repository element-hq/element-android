/*
 * Copyright 2020-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.homeserver

import com.airbnb.mvrx.MavericksState
import org.matrix.android.sdk.api.session.homeserver.HomeServerCapabilities

data class HomeServerCapabilitiesViewState(
        val capabilities: HomeServerCapabilities = HomeServerCapabilities(),
        val isE2EByDefault: Boolean = true
) : MavericksState
