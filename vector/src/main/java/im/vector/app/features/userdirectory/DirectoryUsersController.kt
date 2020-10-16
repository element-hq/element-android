/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.userdirectory

import com.airbnb.epoxy.EpoxyController
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import im.vector.app.R
import im.vector.app.core.epoxy.errorWithRetryItem
import im.vector.app.core.epoxy.loadingItem
import im.vector.app.core.epoxy.noResultItem
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.home.AvatarRenderer
import org.matrix.android.sdk.api.MatrixPatterns
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.user.model.User
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

class DirectoryUsersController @Inject constructor(private val session: Session,
                                                   private val avatarRenderer: AvatarRenderer,
                                                   private val stringProvider: StringProvider,
                                                   private val errorFormatter: ErrorFormatter) : EpoxyController() {

    private var state: UserDirectoryViewState? = null

    var callback: Callback? = null

    init {
        requestModelBuild()
    }

    fun setData(state: UserDirectoryViewState) {
        this.state = state
        requestModelBuild()
    }

    override fun buildModels() {
        val currentState = state ?: return
        val hasSearch = currentState.directorySearchTerm.isNotBlank()
        when (val asyncUsers = currentState.directoryUsers) {
            is Uninitialized -> renderEmptyState(false)
            is Loading       -> renderLoading()
            is Success       -> renderSuccess(
                    computeUsersList(asyncUsers(), currentState.directorySearchTerm),
                    currentState.getSelectedMatrixId(),
                    hasSearch
            )
            is Fail          -> renderFailure(asyncUsers.error)
        }
    }

    /**
     * Eventually add the searched terms, if it is a userId, and if not already present in the result
     */
    private fun computeUsersList(directoryUsers: List<User>, searchTerms: String): List<User> {
        return directoryUsers +
                searchTerms
                        .takeIf { terms -> MatrixPatterns.isUserId(terms) && !directoryUsers.any { it.userId == terms } }
                        ?.let { listOf(User(it)) }
                        .orEmpty()
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

    private fun renderSuccess(users: List<User>,
                              selectedUsers: List<String>,
                              hasSearch: Boolean) {
        if (users.isEmpty()) {
            renderEmptyState(hasSearch)
        } else {
            renderUsers(users, selectedUsers)
        }
    }

    private fun renderUsers(users: List<User>, selectedUsers: List<String>) {
        for (user in users) {
            if (user.userId == session.myUserId) {
                continue
            }
            val isSelected = selectedUsers.contains(user.userId)
            userDirectoryUserItem {
                id(user.userId)
                selected(isSelected)
                matrixItem(user.toMatrixItem())
                avatarRenderer(avatarRenderer)
                clickListener { _ ->
                    callback?.onItemClick(user)
                }
            }
        }
    }

    private fun renderEmptyState(hasSearch: Boolean) {
        val noResultRes = if (hasSearch) {
            R.string.no_result_placeholder
        } else {
            R.string.direct_room_start_search
        }
        noResultItem {
            id("noResult")
            text(stringProvider.getString(noResultRes))
        }
    }

    interface Callback {
        fun onItemClick(user: User)
        fun retryDirectoryUsersRequest()
    }
}
