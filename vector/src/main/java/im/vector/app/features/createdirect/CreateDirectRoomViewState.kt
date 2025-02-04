/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.createdirect

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import im.vector.app.features.userdirectory.PendingSelection

data class CreateDirectRoomViewState(
        val pendingSelections: Set<PendingSelection> = emptySet(),
        val createAndInviteState: Async<String> = Uninitialized
) : MavericksState
