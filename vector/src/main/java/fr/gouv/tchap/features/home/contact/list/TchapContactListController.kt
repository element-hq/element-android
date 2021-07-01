/*
 * Copyright (c) 2021 New Vector Ltd
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

package fr.gouv.tchap.features.home.contact.list

import android.view.View
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
import im.vector.app.features.userdirectory.actionItem
import im.vector.app.features.userdirectory.userDirectoryUserItem
import im.vector.app.features.userdirectory.userListHeaderItem
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.user.model.User
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

class TchapContactListController @Inject constructor(private val session: Session,
                                                     private val avatarRenderer: AvatarRenderer,
                                                     private val stringProvider: StringProvider,
                                                     private val errorFormatter: ErrorFormatter) : EpoxyController() {

    private var state: TchapContactListViewState? = null

    var callback: Callback? = null

    fun setData(state: TchapContactListViewState) {
        this.state = state
        requestModelBuild()
    }

    override fun buildModels() {
        val currentState = state ?: return
        val host = this

        if (currentState.showSearch) {
            actionItem {
                id(R.drawable.ic_tchap_contact_search)
                title(host.stringProvider.getString(R.string.search_in_my_contacts))
                actionIconRes(R.drawable.ic_tchap_contact_search)
                clickAction(View.OnClickListener {
                    host.callback?.onContactSearchClick()
                })
            }
        }

        buildlocalContacts(currentState)

        when (val asyncUsers = currentState.directoryUsers) {
            is Uninitialized -> {
            }
            is Loading -> renderLoading()
            is Fail -> renderFailure(asyncUsers.error)
            is Success -> buildDirectoryUsers(
                    asyncUsers(),
                    currentState.searchTerm,
                    emptyList()
            )
        }
    }

    private fun buildlocalContacts(currentState: TchapContactListViewState) {
        val host = this
        userListHeaderItem {
            id("local_header", 1)
            header(host.stringProvider.getString(R.string.local_address_book_header))
        }

        currentState.filteredLocalUsers.toMutableList().sortedWith() { contact1, contact2 ->
            val lhs = contact1.getBestName()
            val rhs = contact2.getBestName()

            String.CASE_INSENSITIVE_ORDER.compare(lhs, rhs)
        }.forEach { item ->
            userDirectoryUserItem {
                id(item.userId)
                selected(false)
                matrixItem(item.toMatrixItem())
                avatarRenderer(host.avatarRenderer)
                clickListener { _ ->
                    host.callback?.onItemClick(item)
                }
            }
        }
    }

//    private fun buildKnownUsers(currentState: TchapContactListViewState, selectedUsers: List<String>) {
//        currentState.knownUsers()
//                ?.filter { it.userId != session.myUserId }
//                ?.let { userList ->
//                    userListHeaderItem {
//                        id("known_header")
//                        header(stringProvider.getString(R.string.direct_room_user_list_known_title))
//                    }
//
//                    if (userList.isEmpty()) {
//                        renderEmptyState()
//                        return
//                    }
//                    userList.forEach { item ->
//                        val isSelected = selectedUsers.contains(item.userId)
//                        userDirectoryUserItem {
//                            id(item.userId)
//                            selected(isSelected)
//                            matrixItem(item.toMatrixItem())
//                            avatarRenderer(avatarRenderer)
//                            clickListener { _ ->
//                                callback?.onItemClick(item)
//                            }
//                        }
//                    }
//                }
//    }

    private fun buildDirectoryUsers(directoryUsers: List<User>, searchTerms: String, ignoreIds: List<String>) {
        val host = this
        val toDisplay = directoryUsers
                .filterNot { ignoreIds.contains(it.userId) || it.userId == session.myUserId }

        if (toDisplay.isEmpty() && searchTerms.isBlank()) {
            return
        }
        userListHeaderItem {
            id("directory_header")
            header(host.stringProvider.getString(R.string.user_directory_header))
        }
        if (toDisplay.isEmpty()) {
            renderEmptyState()
        } else {
            toDisplay.forEach { user ->
                userDirectoryUserItem {
                    id(user.userId)
                    selected(false)
                    matrixItem(user.toMatrixItem())
                    avatarRenderer(host.avatarRenderer)
                    clickListener { _ ->
                        host.callback?.onItemClick(user)
                    }
                }
            }
        }
    }

    private fun renderLoading() {
        loadingItem {
            id("loading")
        }
    }

    private fun renderEmptyState() {
        val host = this
        noResultItem {
            id("noResult")
            text(host.stringProvider.getString(R.string.no_result_placeholder))
        }
    }

    private fun renderFailure(failure: Throwable) {
        val host = this
        errorWithRetryItem {
            id("error")
            text(host.errorFormatter.toHumanReadable(failure))
        }
    }

    interface Callback {
        fun onItemClick(user: User)
        fun onMatrixIdClick(matrixId: String)
        fun onContactSearchClick()
    }
}
