/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.list.home

import com.airbnb.mvrx.MavericksState
import im.vector.app.core.platform.StateView
import im.vector.app.features.home.room.list.home.header.RoomsHeadersData

data class HomeRoomListViewState(
        val emptyState: StateView.State.Empty? = null,
        val headersData: RoomsHeadersData = RoomsHeadersData(),
) : MavericksState
