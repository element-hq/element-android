/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.roomdirectory.roompreview

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.Uninitialized
import im.vector.app.features.roomdirectory.JoinState
import org.matrix.android.sdk.api.util.MatrixItem

data class RoomPreviewViewState(
        val peekingState: Async<PeekingState> = Uninitialized,
        // The room id
        val roomId: String = "",
        val roomAlias: String? = null,

        val roomName: String? = null,
        val roomTopic: String? = null,
        val avatarUrl: String? = null,

        val shouldPeekFromServer: Boolean = false,
        /**
         * Can be empty when the server is the current user's homeserver.
         */
        val homeServers: List<String> = emptyList(),
        // Current state of the room in preview
        val roomJoinState: JoinState = JoinState.NOT_JOINED,
        // Last error of join room request
        val lastError: Throwable? = null
) : MvRxState {

    constructor(args: RoomPreviewData) : this(
            roomId = args.roomId,
            roomAlias = args.roomAlias,
            homeServers = args.homeServers,
            roomName = args.roomName,
            roomTopic = args.topic,
            avatarUrl = args.avatarUrl,
            shouldPeekFromServer = args.peekFromServer
    )

    fun matrixItem() : MatrixItem {
        return MatrixItem.RoomItem(roomId, roomName ?: roomAlias, avatarUrl)
    }
}
