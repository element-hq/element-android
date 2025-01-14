/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomdirectory.roompreview

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.Uninitialized
import im.vector.app.features.roomdirectory.JoinState
import org.matrix.android.sdk.api.session.permalinks.PermalinkData
import org.matrix.android.sdk.api.session.room.model.RoomType
import org.matrix.android.sdk.api.util.MatrixItem

data class RoomPreviewViewState(
        val peekingState: Async<PeekingState> = Uninitialized,
        // The room id
        val roomId: String = "",
        val roomAlias: String? = null,
        val roomType: String? = null,

        val roomName: String? = null,
        val roomTopic: String? = null,
        val numJoinMembers: Int? = null,
        val avatarUrl: String? = null,

        val shouldPeekFromServer: Boolean = false,
        /**
         * Can be empty when the server is the current user's homeserver.
         */
        val homeServers: List<String> = emptyList(),
        // Current state of the room in preview
        val roomJoinState: JoinState = JoinState.NOT_JOINED,
        // Last error of join room request
        val lastError: Throwable? = null,

        val fromEmailInvite: PermalinkData.RoomEmailInviteLink? = null,
        // used only if it's an email invite
        val isEmailBoundToAccount: Boolean = false
) : MavericksState {

    constructor(args: RoomPreviewData) : this(
            roomId = args.roomId,
            roomAlias = args.roomAlias,
            homeServers = args.homeServers,
            roomName = args.roomName,
            roomTopic = args.topic,
            numJoinMembers = args.numJoinedMembers,
            avatarUrl = args.avatarUrl,
            shouldPeekFromServer = args.peekFromServer,
            fromEmailInvite = args.fromEmailInvite,
            roomType = args.roomType
    )

    fun matrixItem(): MatrixItem {
        return if (roomType == RoomType.SPACE) MatrixItem.SpaceItem(roomId, roomName ?: roomAlias, avatarUrl)
        else MatrixItem.RoomItem(roomId, roomName ?: roomAlias, avatarUrl)
    }
}
