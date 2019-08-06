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

package im.vector.riotx.features.roomdirectory

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.Uninitialized
import im.vector.matrix.android.api.session.room.model.roomdirectory.PublicRoom

data class PublicRoomsViewState(
        // The current filter
        val currentFilter: String = "",
        // Store cumul of pagination result
        val publicRooms: List<PublicRoom> = emptyList(),
        // Current pagination request
        val asyncPublicRoomsRequest: Async<List<PublicRoom>> = Uninitialized,
        // True if more result are available server side
        val hasMore: Boolean = false,
        // Set of roomIds that the user wants to join
        val joiningRoomsIds: Set<String> = emptySet(),
        // Set of roomIds that the user wants to join, but an error occurred
        val joiningErrorRoomsIds: Set<String> = emptySet(),
        // Set of joined roomId,
        val joinedRoomsIds: Set<String> = emptySet(),
        val roomDirectoryDisplayName: String? = null
) : MvRxState