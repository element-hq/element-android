/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import im.vector.app.features.settings.devices.v2.verification.CurrentSessionCrossSigningInfo

data class DevicesViewState(
        val currentSessionCrossSigningInfo: CurrentSessionCrossSigningInfo = CurrentSessionCrossSigningInfo(),
        val devices: Async<DeviceFullInfoList> = Uninitialized,
        val isLoading: Boolean = false,
        val isShowingIpAddress: Boolean = false,
        val delegatedOidcAuthEnabled: Boolean = false,
) : MavericksState

data class DeviceFullInfoList(
        val allSessions: List<DeviceFullInfo>,
        val unverifiedSessionsCount: Int,
        val inactiveSessionsCount: Int,
)
