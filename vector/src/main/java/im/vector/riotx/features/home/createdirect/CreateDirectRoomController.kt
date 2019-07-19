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

import com.airbnb.epoxy.EpoxyController
import com.airbnb.epoxy.VisibilityState
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Incomplete
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.session.user.model.User
import im.vector.matrix.android.internal.util.firstLetterOfDisplayName
import im.vector.riotx.core.epoxy.errorWithRetryItem
import im.vector.riotx.core.epoxy.loadingItem
import im.vector.riotx.core.error.ErrorFormatter
import im.vector.riotx.features.home.AvatarRenderer
import javax.inject.Inject

class CreateDirectRoomController @Inject constructor(private val avatarRenderer: AvatarRenderer,
                                                     private val errorFormatter: ErrorFormatter) : EpoxyController() {

    private var state: CreateDirectRoomViewState? = null
    var displayMode = CreateDirectRoomViewState.DisplayMode.KNOWN_USERS

    var callback: Callback? = null

    init {
        requestModelBuild()
    }

    fun setData(state: CreateDirectRoomViewState) {
        this.state = state
        requestModelBuild()
    }

    override fun buildModels() {
        val currentState = state ?: return
        val asyncUsers = if (displayMode == CreateDirectRoomViewState.DisplayMode.DIRECTORY_USERS) {
            currentState.directoryUsers
        } else {
            currentState.knownUsers
        }
        when (asyncUsers) {
            is Incomplete -> renderLoading()
            is Success    -> renderUsers(asyncUsers(), currentState.selectedUsers)
            is Fail       -> renderFailure(asyncUsers.error)
        }
    }

    private fun renderLoading() {
        loadingItem {
            id("loading")
        }
    }

    private fun renderFailure(failure: Throwable) {
        errorWithRetryItem {
            id("error")
            text(errorFormatter.toHumanReadable(failure))
            listener { callback?.retryDirectoryUsersRequest() }
        }
    }

    private fun renderUsers(users: List<User>, selectedUsers: Set<User>) {
        var lastFirstLetter: String? = null
        users.forEach { user ->
            val isSelected = selectedUsers.contains(user)
            val currentFirstLetter = user.displayName.firstLetterOfDisplayName()
            val showLetter = currentFirstLetter.isNotEmpty() && lastFirstLetter != currentFirstLetter
            lastFirstLetter = currentFirstLetter

            CreateDirectRoomLetterHeaderItem_()
                    .id(currentFirstLetter)
                    .letter(currentFirstLetter)
                    .addIf(showLetter, this)

            createDirectRoomUserItem {
                id(user.userId)
                selected(isSelected)
                userId(user.userId)
                name(user.displayName)
                avatarUrl(user.avatarUrl)
                avatarRenderer(avatarRenderer)
                clickListener { _ ->
                    callback?.onItemClick(user)
                }
            }
        }
    }

    interface Callback {
        fun onItemClick(user: User)
        fun retryDirectoryUsersRequest() {
            // NO-OP
        }
    }

}