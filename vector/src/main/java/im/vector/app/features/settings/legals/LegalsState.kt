/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.legals

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import im.vector.app.features.discovery.ServerAndPolicies

data class LegalsState(
        val homeServer: Async<ServerAndPolicies?> = Uninitialized,
        val hasIdentityServer: Boolean = false,
        val identityServer: Async<ServerAndPolicies?> = Uninitialized
) : MavericksState
