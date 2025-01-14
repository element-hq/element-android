/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.discovery.change

import com.airbnb.mvrx.MavericksState

data class SetIdentityServerState(
        val homeServerUrl: String = "",
        // Will contain the default identity server url if any
        val defaultIdentityServerUrl: String? = null
) : MavericksState
