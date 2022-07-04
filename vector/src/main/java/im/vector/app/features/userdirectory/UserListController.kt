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
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.ui.list.genericPillItem
import im.vector.app.features.displayname.getBestName
import im.vector.app.features.home.AvatarRenderer
import im.vector.lib.core.utils.epoxy.charsequence.toEpoxyCharSequence
import me.gujun.android.span.span
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.identity.IdentityServiceError
import org.matrix.android.sdk.api.session.identity.ThreePid
import org.matrix.android.sdk.api.session.user.model.User
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

class UserListController @Inject constructor(
        private val session: Session,
        private val avatarRenderer: AvatarRenderer,
        private val stringProvider: StringProvider,
        private val colorProvider: ColorProvider,
        private val errorFormatter: ErrorFormatter
) : EpoxyController() {

    private var state: UserListViewState? = null

    var callback: Callback? = null

    fun setData(state: UserListViewState) {
        this.state = state
        requestModelBuild()
    }

    override fun buildModels() {
        val currentState = state ?: return
        val host = this

        // Build generic items
        if (currentState.searchTerm.isBlank()) {
            if (currentState.showInviteActions()) {
                actionItem {
                    id(R.drawable.ic_share)
                    title(host.stringProvider.getString(R.string.invite_friends))
                    actionIconRes(R.drawable.ic_share)
                    clickAction {
                        host.callback?.onInviteFriendClick()
                    }
                }
            }
            if (currentState.showContactBookAction) {
                actionItem {
                    id(R.drawable.ic_baseline_perm_contact_calendar_24)
                    title(host.stringProvider.getString(R.string.contacts_book_title))
                    actionIconRes(R.drawable.ic_baseline_perm_contact_calendar_24)
                    clickAction {
                        host.callback?.onContactBookClick()
                    }
                }
            }
            if (currentState.showInviteActions()) {
                actionItem {
                    id(R.drawable.ic_qr_code_add)
                    title(host.stringProvider.getString(R.string.qr_code))
                    actionIconRes(R.drawable.ic_qr_code_add)
                    clickAction {
                        host.callback?.onUseQRCode()
                    }
                }
            }
        }

        when (val matchingEmail = currentState.matchingEmail) {
            is Success -> {
                matchingEmail()?.let { threePidUser ->
                    userListHeaderItem {
                        id("identity_server_result_header")
                        header(host.stringProvider.getString(R.string.discovery_section, currentState.configuredIdentityServer ?: ""))
                    }
                    val isSelected = currentState.pendingSelections.any { pendingSelection ->
                        when (pendingSelection) {
                            is PendingSelection.ThreePidPendingSelection -> {
                                when (pendingSelection.threePid) {
                                    is ThreePid.Email -> pendingSelection.threePid.email == threePidUser.email
                                    is ThreePid.Msisdn -> false
                                }
                            }
                            is PendingSelection.UserPendingSelection -> {
                                threePidUser.user != null && threePidUser.user.userId == pendingSelection.user.userId
                            }
                        }
                    }
                    if (threePidUser.user == null) {
                        inviteByEmailItem {
                            id("email_${threePidUser.email}")
                            foundItem(threePidUser)
                            selected(isSelected)
                            clickListener {
                                host.callback?.onThreePidClick(ThreePid.Email(threePidUser.email))
                            }
                        }
                    } else {
                        userDirectoryUserItem {
                            id(threePidUser.user.userId)
                            selected(isSelected)
                            matrixItem(threePidUser.user.toMatrixItem().let {
                                it.copy(
                                        displayName = "${it.getBestName()} [${threePidUser.email}]"
                                )
                            })
                            avatarRenderer(host.avatarRenderer)
                            clickListener {
                                host.callback?.onItemClick(threePidUser.user)
                            }
                        }
                    }
                }
            }
            is Fail -> {
                when (matchingEmail.error) {
                    is IdentityServiceError.UserConsentNotProvided -> {
                        genericPillItem {
                            id("consent_not_given")
                            text(
                                    span {
                                        span {
                                            text = host.stringProvider.getString(R.string.settings_discovery_consent_notice_off_2)
                                        }
                                        +"\n"
                                        span {
                                            text = host.stringProvider.getString(R.string.settings_discovery_consent_action_give_consent)
                                            textStyle = "bold"
                                            textColor = host.colorProvider.getColorFromAttribute(R.attr.colorPrimary)
                                        }
                                    }.toEpoxyCharSequence()
                            )
                            itemClickAction {
                                host.callback?.giveIdentityServerConsent()
                            }
                        }
                    }
                    is IdentityServiceError.NoIdentityServerConfigured -> {
                        genericPillItem {
                            id("no_IDS")
                            imageRes(R.drawable.ic_info)
                            text(
                                    span {
                                        span {
                                            text = host.stringProvider.getString(R.string.finish_setting_up_discovery)
                                            textColor = host.colorProvider.getColorFromAttribute(R.attr.vctr_content_primary)
                                        }
                                        +"\n"
                                        span {
                                            text = host.stringProvider.getString(R.string.discovery_invite)
                                            textColor = host.colorProvider.getColorFromAttribute(R.attr.vctr_content_secondary)
                                        }
                                        +"\n"
                                        span {
                                            text = host.stringProvider.getString(R.string.finish_setup)
                                            textStyle = "bold"
                                            textColor = host.colorProvider.getColorFromAttribute(R.attr.colorPrimary)
                                        }
                                    }.toEpoxyCharSequence()
                            )
                            itemClickAction {
                                host.callback?.onSetupDiscovery()
                            }
                        }
                    }
                }
            }
            is Loading -> {
                userListHeaderItem {
                    id("identity_server_result_header_loading")
                    header(host.stringProvider.getString(R.string.discovery_section, currentState.configuredIdentityServer ?: ""))
                }
                loadingItem {
                    id("is_loading")
                }
            }
            else -> {
                // nop
            }
        }

        when (currentState.knownUsers) {
            is Uninitialized -> renderEmptyState()
            is Loading -> renderLoading()
            is Fail -> renderFailure(currentState.knownUsers.error)
            is Success -> buildKnownUsers(currentState, currentState.getSelectedMatrixId())
        }

        when (val asyncUsers = currentState.directoryUsers) {
            is Uninitialized -> {
            }
            is Loading -> renderLoading()
            is Fail -> renderFailure(asyncUsers.error)
            is Success -> buildDirectoryUsers(
                    asyncUsers(),
                    currentState.getSelectedMatrixId(),
                    currentState.searchTerm,
                    // to avoid showing twice same user in known and suggestions
                    currentState.knownUsers.invoke()?.map { it.userId }.orEmpty()
            )
        }
    }

    private fun buildKnownUsers(currentState: UserListViewState, selectedUsers: List<String>) {
        val host = this
        currentState.knownUsers()
                ?.filter { it.userId != session.myUserId }
                ?.let { userList ->
                    userListHeaderItem {
                        id("known_header")
                        header(host.stringProvider.getString(R.string.direct_room_user_list_known_title))
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
                            avatarRenderer(host.avatarRenderer)
                            clickListener {
                                host.callback?.onItemClick(item)
                            }
                        }
                    }
                }
    }

    private fun buildDirectoryUsers(directoryUsers: List<User>, selectedUsers: List<String>, searchTerms: String, ignoreIds: List<String>) {
        val host = this
        val toDisplay = directoryUsers
                .filter { !ignoreIds.contains(it.userId) && it.userId != session.myUserId }

        if (toDisplay.isEmpty() && searchTerms.isBlank()) {
            return
        }
        userListHeaderItem {
            id("suggestions")
            header(host.stringProvider.getString(R.string.direct_room_user_list_suggestions_title))
        }
        if (toDisplay.isEmpty()) {
            renderEmptyState()
        } else {
            toDisplay.forEach { user ->
                val isSelected = selectedUsers.contains(user.userId)
                userDirectoryUserItem {
                    id(user.userId)
                    selected(isSelected)
                    matrixItem(user.toMatrixItem())
                    avatarRenderer(host.avatarRenderer)
                    clickListener {
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
        fun onInviteFriendClick()
        fun onContactBookClick()
        fun onUseQRCode()
        fun onItemClick(user: User)
        fun onMatrixIdClick(matrixId: String)
        fun onThreePidClick(threePid: ThreePid)
        fun onSetupDiscovery()
        fun giveIdentityServerConsent()
    }
}
