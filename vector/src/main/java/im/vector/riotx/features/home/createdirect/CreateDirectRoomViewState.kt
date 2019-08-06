/*
 *
 *  * Copyright 2019 New Vector Ltd
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package im.vector.riotx.features.home.createdirect

import androidx.paging.PagedList
import arrow.core.Option
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.Uninitialized
import im.vector.matrix.android.api.session.user.model.User

data class CreateDirectRoomViewState(
        val knownUsers: Async<PagedList<User>> = Uninitialized,
        val directoryUsers: Async<List<User>> = Uninitialized,
        val selectedUsers: Set<User> = emptySet(),
        val createAndInviteState: Async<String> = Uninitialized,
        val directorySearchTerm: String = "",
        val filterKnownUsersValue: Option<String> = Option.empty()
) : MvRxState {

    enum class DisplayMode {
        KNOWN_USERS,
        DIRECTORY_USERS
    }

}