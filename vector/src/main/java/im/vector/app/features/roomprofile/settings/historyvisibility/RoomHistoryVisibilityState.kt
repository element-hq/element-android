/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.settings.historyvisibility

import im.vector.app.core.ui.bottomsheet.BottomSheetGenericState
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibility

data class RoomHistoryVisibilityState(
        val currentRoomHistoryVisibility: RoomHistoryVisibility = RoomHistoryVisibility.SHARED
) : BottomSheetGenericState() {

    constructor(args: RoomHistoryVisibilityBottomSheetArgs) : this(currentRoomHistoryVisibility = args.currentRoomHistoryVisibility)
}
