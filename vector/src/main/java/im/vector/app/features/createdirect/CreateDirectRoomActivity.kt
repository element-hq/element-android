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

package im.vector.app.features.createdirect

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
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
import im.vector.app.core.utils.PERMISSIONS_FOR_TAKING_PHOTO
import im.vector.app.core.utils.checkPermissions
import im.vector.app.core.utils.onPermissionDeniedSnackbar
import im.vector.app.core.utils.registerForPermissionsResult
import im.vector.app.features.analytics.plan.MobileScreen
import im.vector.app.features.contactsbook.ContactsBookFragment
import im.vector.app.features.qrcode.QrCodeScannerEvents
import im.vector.app.features.qrcode.QrCodeScannerFragment
import im.vector.app.features.qrcode.QrCodeScannerViewModel
import im.vector.app.features.qrcode.QrScannerArgs
import im.vector.app.features.userdirectory.UserListFragment
import im.vector.app.features.userdirectory.UserListFragmentArgs
import im.vector.app.features.userdirectory.UserListSharedAction
import im.vector.app.features.userdirectory.UserListSharedActionViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.session.room.failure.CreateRoomFailure
import java.net.HttpURLConnection
import javax.inject.Inject

@AndroidEntryPoint
class CreateDirectRoomActivity : SimpleFragmentActivity() {

    private val viewModel: CreateDirectRoomViewModel by viewModel()
    private val qrViewModel: QrCodeScannerViewModel by viewModel()

    private lateinit var sharedActionViewModel: UserListSharedActionViewModel
    @Inject lateinit var errorFormatter: ErrorFormatter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        analyticsScreenName = MobileScreen.ScreenName.StartChat
        views.toolbar.visibility = View.GONE

        sharedActionViewModel = viewModelProvider.get(UserListSharedActionViewModel::class.java)
        sharedActionViewModel
                .stream()
                .onEach { action ->
                    when (action) {
                        UserListSharedAction.Close                 -> finish()
                        UserListSharedAction.GoBack                -> onBackPressed()
                        is UserListSharedAction.OnMenuItemSelected -> onMenuItemSelected(action)
                        UserListSharedAction.OpenPhoneBook         -> openPhoneBook()
                        UserListSharedAction.AddByQrCode           -> openAddByQrCode()
                    }
                }
                .launchIn(lifecycleScope)
        if (isFirstCreation()) {
            addFragment(
                    views.container,
                    UserListFragment::class.java,
                    UserListFragmentArgs(
                            title = getString(R.string.fab_menu_create_chat),
                            menuResId = R.menu.vector_create_direct_room
                    )
            )
        }
        viewModel.onEach(CreateDirectRoomViewState::createAndInviteState) {
            renderCreateAndInviteState(it)
        }

        viewModel.observeViewEvents {
            when (it) {
                CreateDirectRoomViewEvents.InvalidCode -> {
                    Toast.makeText(this, R.string.invalid_qr_code_uri, Toast.LENGTH_SHORT).show()
                    finish()
                }
                CreateDirectRoomViewEvents.DmSelf      -> {
                    Toast.makeText(this, R.string.cannot_dm_self, Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }

        qrViewModel.observeViewEvents {
            when (it) {
                is QrCodeScannerEvents.CodeParsed  -> {
                    viewModel.handle(CreateDirectRoomAction.QrScannedAction(it.result))
                }
                is QrCodeScannerEvents.ParseFailed -> {
                    Toast.makeText(this, R.string.qr_code_not_scanned, Toast.LENGTH_SHORT).show()
                    finish()
                }
                else                               -> Unit
            }
        }
    }

    private fun openAddByQrCode() {
        if (checkPermissions(PERMISSIONS_FOR_TAKING_PHOTO, this, permissionCameraLauncher)) {
            val args = QrScannerArgs(showExtraButtons = false, R.string.add_by_qr_code)
            addFragment(views.container, QrCodeScannerFragment::class.java, args)
        }
    }

    private fun openPhoneBook() {
        // Check permission first
        if (checkPermissions(PERMISSIONS_FOR_MEMBERS_SEARCH, this, permissionReadContactLauncher)) {
            addFragmentToBackstack(views.container, ContactsBookFragment::class.java)
        }
    }

    private val permissionReadContactLauncher = registerForPermissionsResult { allGranted, deniedPermanently ->
        if (allGranted) {
            doOnPostResume { addFragmentToBackstack(views.container, ContactsBookFragment::class.java) }
        } else if (deniedPermanently) {
            onPermissionDeniedSnackbar(R.string.permissions_denied_add_contact)
        }
    }

    private val permissionCameraLauncher = registerForPermissionsResult { allGranted, deniedPermanently ->
        if (allGranted) {
            val args = QrScannerArgs(showExtraButtons = false, R.string.add_by_qr_code)
            addFragment(views.container, QrCodeScannerFragment::class.java, args)
        } else if (deniedPermanently) {
            onPermissionDeniedSnackbar(R.string.permissions_denied_qr_code)
        }
    }

    private fun onMenuItemSelected(action: UserListSharedAction.OnMenuItemSelected) {
        if (action.itemId == R.id.action_create_direct_room) {
            viewModel.handle(CreateDirectRoomAction.CreateRoomAndInviteSelectedUsers(action.selections))
        }
    }

    private fun renderCreateAndInviteState(state: Async<String>) {
        when (state) {
            is Loading -> renderCreationLoading()
            is Success -> renderCreationSuccess(state())
            is Fail    -> renderCreationFailure(state.error)
            else       -> Unit
        }
    }

    private fun renderCreationLoading() {
        updateWaitingView(WaitingViewData(getString(R.string.creating_direct_room)))
    }

    private fun renderCreationFailure(error: Throwable) {
        hideWaitingView()
        when (error) {
            is CreateRoomFailure.CreatedWithTimeout           -> {
                finish()
            }
            is CreateRoomFailure.CreatedWithFederationFailure -> {
                MaterialAlertDialogBuilder(this)
                        .setMessage(getString(R.string.create_room_federation_error, error.matrixError.message))
                        .setCancelable(false)
                        .setPositiveButton(R.string.ok) { _, _ -> finish() }
                        .show()
            }
            else                                              -> {
                val message = if (error is Failure.ServerError && error.httpCode == HttpURLConnection.HTTP_INTERNAL_ERROR /*500*/) {
                    // This error happen if the invited userId does not exist.
                    getString(R.string.create_room_dm_failure)
                } else {
                    errorFormatter.toHumanReadable(error)
                }
                MaterialAlertDialogBuilder(this)
                        .setMessage(message)
                        .setPositiveButton(R.string.ok, null)
                        .show()
            }
        }
    }

    private fun renderCreationSuccess(roomId: String) {
        navigator.openRoom(this, roomId)
        finish()
    }

    companion object {

        fun getIntent(context: Context): Intent {
            return Intent(context, CreateDirectRoomActivity::class.java)
        }
    }
}
