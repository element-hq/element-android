/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized

data class DevicesViewState(
        val currentSessionCrossSigningInfo: CurrentSessionCrossSigningInfo = CurrentSessionCrossSigningInfo(),
        val devices: Async<List<DeviceFullInfo>> = Uninitialized,
        val unverifiedSessionsCount: Int = 0,
        val inactiveSessionsCount: Int = 0,
        val isLoading: Boolean = false,
) : MavericksState
