/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.notifications

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import im.vector.app.features.home.room.list.actions.RoomListActionsArgs
import im.vector.app.features.roomprofile.RoomProfileArgs
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.notification.RoomNotificationState

data class RoomNotificationSettingsViewState(
        val roomId: String,
        val roomSummary: Async<RoomSummary> = Uninitialized,
        val isLoading: Boolean = false,
        val notificationState: Async<RoomNotificationState> = Uninitialized
) : MavericksState {
    constructor(args: RoomProfileArgs) : this(roomId = args.roomId)
    constructor(args: RoomListActionsArgs) : this(roomId = args.roomId)
}

/**
 * Used to map this old room notification settings to the new options in v2.
 */
val RoomNotificationSettingsViewState.notificationStateMapped: Async<RoomNotificationState>
    get() {
        return when {
            /**
             * if in an encrypted room, mentions notifications are not supported so show "None" as selected.
             * Also in the new settings there is no notion of notifications without sound so it maps to noisy also
             */
            (roomSummary()?.isEncrypted == true && notificationState() == RoomNotificationState.MENTIONS_ONLY)
            -> Success(RoomNotificationState.MUTE)
            notificationState() == RoomNotificationState.ALL_MESSAGES -> Success(RoomNotificationState.ALL_MESSAGES_NOISY)
            else -> notificationState
        }
    }

/**
 * Used to enumerate the new settings in notification settings v2. Notifications without sound and mentions in encrypted rooms not supported.
 */
val RoomNotificationSettingsViewState.notificationOptions: List<RoomNotificationState>
    get() {
        return if (roomSummary()?.isEncrypted == true) {
            listOf(RoomNotificationState.ALL_MESSAGES_NOISY, RoomNotificationState.MUTE)
        } else {
            listOf(RoomNotificationState.ALL_MESSAGES_NOISY, RoomNotificationState.MENTIONS_ONLY, RoomNotificationState.MUTE)
        }
    }
