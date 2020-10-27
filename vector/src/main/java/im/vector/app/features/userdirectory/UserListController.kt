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
import im.vector.app.core.contacts.MappedContact
import im.vector.app.core.epoxy.checkBoxItem
import im.vector.app.core.epoxy.errorWithRetryItem
import im.vector.app.core.epoxy.loadingItem
import im.vector.app.core.epoxy.noResultItem
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.home.AvatarRenderer
import org.matrix.android.sdk.api.MatrixPatterns
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.api.session.user.model.User
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

class UserListController @Inject constructor(private val session: Session,
                                             private val avatarRenderer: AvatarRenderer,
                                             private val stringProvider: StringProvider,
                                             private val errorFormatter: ErrorFormatter) : EpoxyController() {

    private var state: UserListViewState? = null
    private var selectedUsers: List<String> = emptyList()

    var callback: Callback? = null

    fun setData(state: UserListViewState) {
        this.state = state
        requestModelBuild()
    }

    override fun buildModels() {
        val currentState = state ?: return

        when (currentState.knownUsers) {
            is Uninitialized -> renderEmptyState()
            is Loading       -> renderLoading()
            is Fail          -> renderFailure(currentState.knownUsers.error)
            is Success       -> buildKnownUsers(currentState)
        }

        when (currentState.mappedContacts) {
            is Uninitialized -> renderEmptyState()
            is Loading       -> renderLoading()
            is Fail          -> renderFailure(currentState.mappedContacts.error)
            is Success       -> buildContacts(currentState.filteredMappedContacts, currentState.onlyBoundContacts)
        }

        when (val asyncUsers = currentState.directoryUsers) {
            is Uninitialized -> renderEmptyState()
            is Loading       -> renderLoading()
            is Fail          -> renderFailure(asyncUsers.error)
            is Success       -> buildDirectoryUsers(computeUsersList(asyncUsers(), currentState.searchTerm), currentState.getSelectedMatrixId())
        }
    }

    private fun buildKnownUsers(currentState: UserListViewState) {
        currentState.knownUsers()?.let { userList ->
            userListHeaderItem {
                id("recent")
                header(stringProvider.getString(R.string.direct_room_user_list_recent_title))
            }

            if (userList.isEmpty()) {
                renderEmptyState()
                return
            }
            userList.forEach { item ->
                val isSelected = selectedUsers.contains(item.userId)
                userDirectoryUserItem {
                    id(item.userId)
                    selected(isSelected)
                    matrixItem(item.toMatrixItem())
                    avatarRenderer(avatarRenderer)
                    clickListener { _ ->
                        callback?.onItemClick(item)
                    }
                }
            }
        }
    }

    private fun buildContacts(mappedContacts: List<MappedContact>, onlyBoundContacts: Boolean) {
        userListHeaderItem {
            id("contacts")
            header(stringProvider.getString(R.string.direct_room_user_list_contacts_title))
        }
        if (state?.isBoundRetrieved == true) {
            checkBoxItem {
                id("onlyBoundContacts")
                title(stringProvider.getString(R.string.matrix_only_filter))
                checkChangeListener { _, isChecked ->
                    callback?.onOnlyBoundContactsCheckChanged(isChecked)
                }
            }
        }
        if (mappedContacts.isEmpty()) {
            renderEmptyState()
        } else {
            mappedContacts.forEach { mappedContact ->
                contactItem {
                    id(mappedContact.id)
                    mappedContact(mappedContact)
                    avatarRenderer(avatarRenderer)
                }
                mappedContact.emails
                        .forEachIndexed { index, it ->
                            if (onlyBoundContacts && it.matrixId == null) return@forEachIndexed

                            contactDetailItem {
                                id("${mappedContact.id}-e-$index-${it.email}")
                                threePid(it.email)
                                matrixId(it.matrixId)
                                clickListener {
                                    if (it.matrixId != null) {
                                        callback?.onMatrixIdClick(it.matrixId)
                                    } else {
                                        callback?.onThreePidClick(ThreePid.Email(it.email))
                                    }
                                }
                            }
                        }
                mappedContact.msisdns
                        .forEachIndexed { index, it ->
                            if (onlyBoundContacts && it.matrixId == null) return@forEachIndexed

                            contactDetailItem {
                                id("${mappedContact.id}-m-$index-${it.phoneNumber}")
                                threePid(it.phoneNumber)
                                matrixId(it.matrixId)
                                clickListener {
                                    if (it.matrixId != null) {
                                        callback?.onMatrixIdClick(it.matrixId)
                                    } else {
                                        callback?.onThreePidClick(ThreePid.Msisdn(it.phoneNumber))
                                    }
                                }
                            }
                        }
            }
        }
    }

    private fun buildDirectoryUsers(directoryUsers: List<User>, selectedUsers: List<String>) {
        userListHeaderItem {
            id("suggestions")
            header(stringProvider.getString(R.string.direct_room_user_list_suggestions_title))
        }
        if (directoryUsers.isEmpty()) {
            renderEmptyState()
        } else {
            directoryUsers.forEach { user ->
                if (user.userId != session.myUserId) {
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

    private fun renderEmptyState() {
        noResultItem {
            id("noResult")
            text(stringProvider.getString(R.string.no_result_placeholder))
        }
    }

    private fun renderFailure(failure: Throwable) {
        errorWithRetryItem {
            id("error")
            text(errorFormatter.toHumanReadable(failure))
        }
    }

    interface Callback {
        fun onItemClick(user: User)
        fun onMatrixIdClick(matrixId: String)
        fun onThreePidClick(threePid: ThreePid)
        fun onOnlyBoundContactsCheckChanged(isChecked: Boolean)
    }
}
