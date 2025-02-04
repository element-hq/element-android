/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.invite

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.airbnb.mvrx.Mavericks
import com.airbnb.mvrx.viewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.extensions.addFragmentToBackstack
import im.vector.app.core.platform.SimpleFragmentActivity
import im.vector.app.core.platform.WaitingViewData
import im.vector.app.core.utils.PERMISSIONS_FOR_MEMBERS_SEARCH
import im.vector.app.core.utils.checkPermissions
import im.vector.app.core.utils.onPermissionDeniedSnackbar
import im.vector.app.core.utils.registerForPermissionsResult
import im.vector.app.core.utils.toast
import im.vector.app.features.contactsbook.ContactsBookFragment
import im.vector.app.features.userdirectory.PendingSelection
import im.vector.app.features.userdirectory.UserListFragment
import im.vector.app.features.userdirectory.UserListFragmentArgs
import im.vector.app.features.userdirectory.UserListSharedAction
import im.vector.app.features.userdirectory.UserListSharedActionViewModel
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.failure.Failure
import java.net.HttpURLConnection

@Parcelize
data class InviteUsersToRoomArgs(val roomId: String) : Parcelable

@AndroidEntryPoint
class InviteUsersToRoomActivity : SimpleFragmentActivity() {

    private val viewModel: InviteUsersToRoomViewModel by viewModel()
    private lateinit var sharedActionViewModel: UserListSharedActionViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        views.toolbar.visibility = View.GONE

        sharedActionViewModel = viewModelProvider.get(UserListSharedActionViewModel::class.java)
        sharedActionViewModel
                .stream()
                .onEach { sharedAction ->
                    @Suppress("DEPRECATION")
                    when (sharedAction) {
                        UserListSharedAction.Close -> finish()
                        UserListSharedAction.GoBack -> onBackPressed()
                        is UserListSharedAction.OnMenuItemSubmitClick -> handleOnMenuItemSubmitClick(sharedAction)
                        UserListSharedAction.OpenPhoneBook -> openPhoneBook()
                        // not exhaustive because it's a sharedAction
                        else -> Unit
                    }
                }
                .launchIn(lifecycleScope)
        if (isFirstCreation()) {
            addFragment(
                    views.container,
                    UserListFragment::class.java,
                    UserListFragmentArgs(
                            title = getString(CommonStrings.invite_users_to_room_title),
                            menuResId = R.menu.vector_invite_users_to_room,
                            submitMenuItemId = R.id.action_invite_users_to_room_invite,
                            excludedUserIds = viewModel.getUserIdsOfRoomMembers(),
                            showInviteActions = false
                    )
            )
        }

        viewModel.observeViewEvents { renderInviteEvents(it) }
    }

    private fun handleOnMenuItemSubmitClick(action: UserListSharedAction.OnMenuItemSubmitClick) {
        val unknownUsers = action.selections.filter { it is PendingSelection.UserPendingSelection && it.isUnknownUser }
        if (unknownUsers.isEmpty()) {
            viewModel.handle(InviteUsersToRoomAction.InviteSelectedUsers(action.selections))
        } else {
            MaterialAlertDialogBuilder(this)
                    .setTitle(CommonStrings.dialog_title_confirmation)
                    .setMessage(getString(CommonStrings.invite_unknown_users_dialog_content, unknownUsers.joinToString("\n • ", " • ") { it.getMxId() }))
                    .setPositiveButton(CommonStrings.invite_unknown_users_dialog_submit) { _, _ ->
                        viewModel.handle(InviteUsersToRoomAction.InviteSelectedUsers(action.selections))
                    }
                    .setNegativeButton(CommonStrings.action_cancel, null)
                    .show()
        }
    }

    private fun openPhoneBook() {
        // Check permission first
        if (checkPermissions(PERMISSIONS_FOR_MEMBERS_SEARCH, this, permissionContactLauncher)) {
            addFragmentToBackstack(views.container, ContactsBookFragment::class.java)
        }
    }

    private val permissionContactLauncher = registerForPermissionsResult { allGranted, deniedPermanently ->
        if (allGranted) {
            doOnPostResume { addFragmentToBackstack(views.container, ContactsBookFragment::class.java) }
        } else if (deniedPermanently) {
            onPermissionDeniedSnackbar(CommonStrings.permissions_denied_add_contact)
        }
    }

    private fun renderInviteEvents(viewEvent: InviteUsersToRoomViewEvents) {
        when (viewEvent) {
            is InviteUsersToRoomViewEvents.Loading -> renderInviteLoading()
            is InviteUsersToRoomViewEvents.Success -> renderInvitationSuccess(viewEvent.successMessage)
            is InviteUsersToRoomViewEvents.Failure -> renderInviteFailure(viewEvent.throwable)
        }
    }

    private fun renderInviteLoading() {
        updateWaitingView(WaitingViewData(getString(CommonStrings.inviting_users_to_room)))
    }

    private fun renderInviteFailure(error: Throwable) {
        hideWaitingView()
        val message = if (error is Failure.ServerError && error.httpCode == HttpURLConnection.HTTP_INTERNAL_ERROR /*500*/) {
            // This error happen if the invited userId does not exist.
            getString(CommonStrings.invite_users_to_room_failure)
        } else {
            errorFormatter.toHumanReadable(error)
        }
        MaterialAlertDialogBuilder(this)
                .setMessage(message)
                .setPositiveButton(CommonStrings.ok, null)
                .show()
    }

    private fun renderInvitationSuccess(successMessage: String) {
        toast(successMessage)
        finish()
    }

    companion object {

        fun getIntent(context: Context, roomId: String): Intent {
            return Intent(context, InviteUsersToRoomActivity::class.java).also {
                it.putExtra(Mavericks.KEY_ARG, InviteUsersToRoomArgs(roomId))
            }
        }
    }
}
