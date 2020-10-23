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

import android.app.Activity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ScrollView
import android.widget.Toast
import androidx.core.view.forEach
import androidx.core.view.isVisible
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.chip.Chip
import com.jakewharton.rxbinding3.widget.textChanges
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.extensions.setupAsSearch
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.utils.DimensionConverter
import im.vector.app.core.utils.PERMISSIONS_FOR_TAKING_PHOTO
import im.vector.app.core.utils.checkPermissions
import im.vector.app.core.utils.registerForPermissionsResult
import im.vector.app.features.createdirect.CreateDirectRoomAction
import im.vector.app.features.createdirect.CreateDirectRoomViewModel
import im.vector.app.features.homeserver.HomeServerCapabilitiesViewModel
import im.vector.app.features.qrcode.QrCodeScannerActivity
import kotlinx.android.synthetic.main.fragment_known_users.*
import org.matrix.android.sdk.api.session.user.model.User
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.permalinks.PermalinkData
import org.matrix.android.sdk.api.session.permalinks.PermalinkParser
import javax.inject.Inject

class KnownUsersFragment @Inject constructor(
        val userDirectoryViewModelFactory: UserDirectoryViewModel.Factory,
        private val knownUsersController: KnownUsersController,
        private val dimensionConverter: DimensionConverter,
        private val session: Session,
        val homeServerCapabilitiesViewModelFactory: HomeServerCapabilitiesViewModel.Factory
) : VectorBaseFragment(), KnownUsersController.Callback {

    private val args: KnownUsersFragmentArgs by args()

    override fun getLayoutResId() = R.layout.fragment_known_users

    override fun getMenuRes() = args.menuResId

    private val userDirViewModel: UserDirectoryViewModel by activityViewModel()
    private val createDirectRoomViewModel: CreateDirectRoomViewModel by activityViewModel()
    private val homeServerCapabilitiesViewModel: HomeServerCapabilitiesViewModel by fragmentViewModel()

    private lateinit var sharedActionViewModel: UserDirectorySharedActionViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedActionViewModel = activityViewModelProvider.get(UserDirectorySharedActionViewModel::class.java)

        knownUsersTitle.text = args.title
        vectorBaseActivity.setSupportActionBar(knownUsersToolbar)
        setupRecyclerView()
        setupFilterView()
        setupAddByMatrixIdView()
        setupAddByQrCodeView()
        setupAddFromPhoneBookView()
        setupCloseView()

        homeServerCapabilitiesViewModel.subscribe {
            knownUsersE2EbyDefaultDisabled.isVisible = !it.isE2EByDefault
        }

        userDirViewModel.selectSubscribe(this, UserDirectoryViewState::pendingInvitees) {
            renderSelectedUsers(it)
        }
    }

    override fun onDestroyView() {
        knownUsersController.callback = null
        recyclerView.cleanup()
        super.onDestroyView()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        withState(userDirViewModel) {
            val showMenuItem = it.pendingInvitees.isNotEmpty()
            menu.forEach { menuItem ->
                menuItem.isVisible = showMenuItem
            }
        }
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = withState(userDirViewModel) {
        sharedActionViewModel.post(UserDirectorySharedAction.OnMenuItemSelected(item.itemId, it.pendingInvitees))
        return@withState true
    }

    private fun setupAddByMatrixIdView() {
        addByMatrixId.debouncedClicks {
            sharedActionViewModel.post(UserDirectorySharedAction.OpenUsersDirectory)
        }
    }

    private fun setupAddByQrCodeView() {
        val qrStartForActivityResult = registerStartForActivityResult { activityResult ->
            if (activityResult.resultCode == Activity.RESULT_OK) {
                val result = QrCodeScannerActivity.getResultText(activityResult.data)!!
                val mxid = (PermalinkParser.parse(result) as? PermalinkData.UserLink)?.userId

                if (mxid === null) {
                    Toast.makeText(requireContext(), R.string.invalid_qr_code_uri, Toast.LENGTH_SHORT).show()
                } else {
                    val existingDm = session.getExistingDirectRoomWithUser(mxid)

                    if (existingDm === null) {
                        // The following assumes MXIDs are case insensitive
                        if (mxid.equals(other = session.myUserId, ignoreCase = true)) {
                            Toast.makeText(requireContext(), R.string.cannot_dm_self, Toast.LENGTH_SHORT).show()
                        } else {
                            // Try to get user from known users and fall back to creating a User object from MXID
                            val qrInvitee = if (session.getUser(mxid) != null) session.getUser(mxid)!! else User(mxid, null, null)

                            createDirectRoomViewModel.handle(
                                    CreateDirectRoomAction.CreateRoomAndInviteSelectedUsers(setOf(PendingInvitee.UserPendingInvitee(qrInvitee)))
                            )
                        }
                    } else {
                        navigator.openRoom(requireContext(), existingDm.roomId, null, false)
                        requireActivity().finish()
                    }
                }
            } else {
                Toast.makeText(requireContext(), R.string.qr_code_not_scanned, Toast.LENGTH_SHORT).show()
            }
        }
        val openCameraActivityResultLauncher = registerForPermissionsResult { allGranted ->
            if (allGranted) {
                QrCodeScannerActivity.startForResult(requireActivity(), qrStartForActivityResult)
            } else {
                Toast.makeText(requireContext(), R.string.missing_permissions_error, Toast.LENGTH_SHORT).show()
            }
        }
        addByQrCode.debouncedClicks {
            if (checkPermissions(PERMISSIONS_FOR_TAKING_PHOTO, requireActivity(), openCameraActivityResultLauncher)) {
                QrCodeScannerActivity.startForResult(requireActivity(), qrStartForActivityResult)
            }
        }
    }

    private fun setupAddFromPhoneBookView() {
        addFromPhoneBook.debouncedClicks {
            // TODO handle Permission first
            sharedActionViewModel.post(UserDirectorySharedAction.OpenPhoneBook)
        }
    }

    private fun setupRecyclerView() {
        knownUsersController.callback = this
        // Don't activate animation as we might have way to much item animation when filtering
        recyclerView.configureWith(knownUsersController, disableItemAnimation = true)
    }

    private fun setupFilterView() {
        knownUsersFilter
                .textChanges()
                .startWith(knownUsersFilter.text)
                .subscribe { text ->
                    val filterValue = text.trim()
                    val action = if (filterValue.isBlank()) {
                        UserDirectoryAction.ClearFilterKnownUsers
                    } else {
                        UserDirectoryAction.FilterKnownUsers(filterValue.toString())
                    }
                    userDirViewModel.handle(action)
                }
                .disposeOnDestroyView()

        knownUsersFilter.setupAsSearch()
        knownUsersFilter.requestFocus()
    }

    private fun setupCloseView() {
        knownUsersClose.debouncedClicks {
            requireActivity().finish()
        }
    }

    override fun invalidate() = withState(userDirViewModel) {
        knownUsersController.setData(it)
    }

    private fun renderSelectedUsers(invitees: Set<PendingInvitee>) {
        invalidateOptionsMenu()

        val currentNumberOfChips = chipGroup.childCount
        val newNumberOfChips = invitees.size

        chipGroup.removeAllViews()
        invitees.forEach { addChipToGroup(it) }

        // Scroll to the bottom when adding chips. When removing chips, do not scroll
        if (newNumberOfChips >= currentNumberOfChips) {
            chipGroupScrollView.post {
                chipGroupScrollView.fullScroll(ScrollView.FOCUS_DOWN)
            }
        }
    }

    private fun addChipToGroup(pendingInvitee: PendingInvitee) {
        val chip = Chip(requireContext())
        chip.setChipBackgroundColorResource(android.R.color.transparent)
        chip.chipStrokeWidth = dimensionConverter.dpToPx(1).toFloat()
        chip.text = pendingInvitee.getBestName()
        chip.isClickable = true
        chip.isCheckable = false
        chip.isCloseIconVisible = true
        chipGroup.addView(chip)
        chip.setOnCloseIconClickListener {
            userDirViewModel.handle(UserDirectoryAction.RemovePendingInvitee(pendingInvitee))
        }
    }

    override fun onItemClick(user: User) {
        view?.hideKeyboard()
        userDirViewModel.handle(UserDirectoryAction.SelectPendingInvitee(PendingInvitee.UserPendingInvitee(user)))
    }
}
