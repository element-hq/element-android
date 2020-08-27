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

import com.airbnb.mvrx.MvRxState
import im.vector.app.features.roomdirectory.JoinState

data class RoomPreviewViewState(
        // The room id
        val roomId: String = "",
        val roomAlias: String? = null,
        /**
         * Can be empty when the server is the current user's home server.
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
            homeServers = args.homeServers
    )
}
