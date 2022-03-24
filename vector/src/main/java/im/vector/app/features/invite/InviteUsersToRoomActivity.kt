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
import im.vector.app.core.error.ErrorFormatter
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
import im.vector.app.features.userdirectory.UserListFragment
import im.vector.app.features.userdirectory.UserListFragmentArgs
import im.vector.app.features.userdirectory.UserListSharedAction
import im.vector.app.features.userdirectory.UserListSharedActionViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.failure.Failure
import java.net.HttpURLConnection
import javax.inject.Inject

@Parcelize
data class InviteUsersToRoomArgs(val roomId: String) : Parcelable

@AndroidEntryPoint
class InviteUsersToRoomActivity : SimpleFragmentActivity() {

    private val viewModel: InviteUsersToRoomViewModel by viewModel()
    private lateinit var sharedActionViewModel: UserListSharedActionViewModel
    @Inject lateinit var errorFormatter: ErrorFormatter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        views.toolbar.visibility = View.GONE

        sharedActionViewModel = viewModelProvider.get(UserListSharedActionViewModel::class.java)
        sharedActionViewModel
                .stream()
                .onEach { sharedAction ->
                    when (sharedAction) {
                        UserListSharedAction.Close                 -> finish()
                        UserListSharedAction.GoBack                -> onBackPressed()
                        is UserListSharedAction.OnMenuItemSelected -> onMenuItemSelected(sharedAction)
                        UserListSharedAction.OpenPhoneBook         -> openPhoneBook()
                        // not exhaustive because it's a sharedAction
                        else                                       -> Unit
                    }
                }
                .launchIn(lifecycleScope)
        if (isFirstCreation()) {
            addFragment(
                    views.container,
                    UserListFragment::class.java,
                    UserListFragmentArgs(
                            title = getString(R.string.invite_users_to_room_title),
                            menuResId = R.menu.vector_invite_users_to_room,
                            excludedUserIds = viewModel.getUserIdsOfRoomMembers(),
                            showInviteActions = false
                    )
            )
        }

        viewModel.observeViewEvents { renderInviteEvents(it) }
    }

    private fun onMenuItemSelected(action: UserListSharedAction.OnMenuItemSelected) {
        if (action.itemId == R.id.action_invite_users_to_room_invite) {
            viewModel.handle(InviteUsersToRoomAction.InviteSelectedUsers(action.selections))
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
            onPermissionDeniedSnackbar(R.string.permissions_denied_add_contact)
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
        updateWaitingView(WaitingViewData(getString(R.string.inviting_users_to_room)))
    }

    private fun renderInviteFailure(error: Throwable) {
        hideWaitingView()
        val message = if (error is Failure.ServerError && error.httpCode == HttpURLConnection.HTTP_INTERNAL_ERROR /*500*/) {
            // This error happen if the invited userId does not exist.
            getString(R.string.invite_users_to_room_failure)
        } else {
            errorFormatter.toHumanReadable(error)
        }
        MaterialAlertDialogBuilder(this)
                .setMessage(message)
                .setPositiveButton(R.string.ok, null)
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
