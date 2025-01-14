/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomdirectory.picker

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import im.vector.app.features.roomdirectory.RoomDirectoryServer
import org.matrix.android.sdk.api.session.room.model.thirdparty.ThirdPartyProtocol

data class RoomDirectoryPickerViewState(
        val asyncThirdPartyRequest: Async<Map<String, ThirdPartyProtocol>> = Uninitialized,
        val customHomeservers: Set<String> = emptySet(),
        val inEditMode: Boolean = false,
        val enteredServer: String = "",
        val addServerAsync: Async<Unit> = Uninitialized,
        // computed
        val directories: List<RoomDirectoryServer> = emptyList()
) : MavericksState
