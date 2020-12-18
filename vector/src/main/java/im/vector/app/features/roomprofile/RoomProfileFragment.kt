/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package im.vector.app.features.roomprofile

import android.os.Bundle
import android.os.Parcelable
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.app.R
import im.vector.app.core.animations.AppBarStateChangeListener
import im.vector.app.core.animations.MatrixItemAppBarStateChangeListener
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.extensions.copyOnLongClick
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.utils.copyToClipboard
import im.vector.app.core.utils.startSharePlainTextIntent
import im.vector.app.features.crypto.util.toImageRes
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.list.actions.RoomListActionsArgs
import im.vector.app.features.home.room.list.actions.RoomListQuickActionsBottomSheet
import im.vector.app.features.home.room.list.actions.RoomListQuickActionsSharedAction
import im.vector.app.features.home.room.list.actions.RoomListQuickActionsSharedActionViewModel
import im.vector.app.features.media.BigImageViewerActivity
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_matrix_profile.*
import kotlinx.android.synthetic.main.merge_overlay_waiting_view.*
import kotlinx.android.synthetic.main.view_stub_room_profile_header.*
import org.matrix.android.sdk.api.session.room.notification.RoomNotificationState
import org.matrix.android.sdk.api.util.MatrixItem
import org.matrix.android.sdk.api.util.toMatrixItem
import timber.log.Timber
import javax.inject.Inject

@Parcelize
data class RoomProfileArgs(
        val roomId: String
) : Parcelable

class RoomProfileFragment @Inject constructor(
        private val roomProfileController: RoomProfileController,
        private val avatarRenderer: AvatarRenderer,
        val roomProfileViewModelFactory: RoomProfileViewModel.Factory
) : VectorBaseFragment(),
        RoomProfileController.Callback {

    private val roomProfileArgs: RoomProfileArgs by args()
    private lateinit var roomListQuickActionsSharedActionViewModel: RoomListQuickActionsSharedActionViewModel
    private lateinit var roomProfileSharedActionViewModel: RoomProfileSharedActionViewModel
    private val roomProfileViewModel: RoomProfileViewModel by fragmentViewModel()

    private var appBarStateChangeListener: AppBarStateChangeListener? = null

    override fun getLayoutResId() = R.layout.fragment_matrix_profile

    override fun getMenuRes() = R.menu.vector_room_profile

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        roomListQuickActionsSharedActionViewModel = activityViewModelProvider.get(RoomListQuickActionsSharedActionViewModel::class.java)
        roomProfileSharedActionViewModel = activityViewModelProvider.get(RoomProfileSharedActionViewModel::class.java)
        val headerView = matrixProfileHeaderView.let {
            it.layoutResource = R.layout.view_stub_room_profile_header
            it.inflate()
        }
        setupWaitingView()
        setupToolbar(matrixProfileToolbar)
        setupRecyclerView()
        appBarStateChangeListener = MatrixItemAppBarStateChangeListener(
                headerView,
                listOf(matrixProfileToolbarAvatarImageView,
                        matrixProfileToolbarTitleView,
                        matrixProfileDecorationToolbarAvatarImageView)
        )
        matrixProfileAppBarLayout.addOnOffsetChangedListener(appBarStateChangeListener)
        roomProfileViewModel.observeViewEvents {
            when (it) {
                is RoomProfileViewEvents.Loading          -> showLoading(it.message)
                is RoomProfileViewEvents.Failure          -> showFailure(it.throwable)
                is RoomProfileViewEvents.ShareRoomProfile -> onShareRoomProfile(it.permalink)
                is RoomProfileViewEvents.OnShortcutReady  -> addShortcut(it)
            }.exhaustive
        }
        roomListQuickActionsSharedActionViewModel
                .observe()
                .subscribe { handleQuickActions(it) }
                .disposeOnDestroyView()
        setupLongClicks()
    }

    private fun setupWaitingView() {
        waiting_view_status_text.setText(R.string.please_wait)
        waiting_view_status_text.isVisible = true
    }

    private fun setupLongClicks() {
        roomProfileNameView.copyOnLongClick()
        roomProfileAliasView.copyOnLongClick()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.roomProfileShareAction -> {
                roomProfileViewModel.handle(RoomProfileAction.ShareRoomProfile)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun handleQuickActions(action: RoomListQuickActionsSharedAction) = when (action) {
        is RoomListQuickActionsSharedAction.NotificationsAllNoisy     -> {
            roomProfileViewModel.handle(RoomProfileAction.ChangeRoomNotificationState(RoomNotificationState.ALL_MESSAGES_NOISY))
        }
        is RoomListQuickActionsSharedAction.NotificationsAll          -> {
            roomProfileViewModel.handle(RoomProfileAction.ChangeRoomNotificationState(RoomNotificationState.ALL_MESSAGES))
        }
        is RoomListQuickActionsSharedAction.NotificationsMentionsOnly -> {
            roomProfileViewModel.handle(RoomProfileAction.ChangeRoomNotificationState(RoomNotificationState.MENTIONS_ONLY))
        }
        is RoomListQuickActionsSharedAction.NotificationsMute         -> {
            roomProfileViewModel.handle(RoomProfileAction.ChangeRoomNotificationState(RoomNotificationState.MUTE))
        }
        else                                                          -> Timber.v("$action not handled")
    }

    private fun setupRecyclerView() {
        roomProfileController.callback = this
        matrixProfileRecyclerView.configureWith(roomProfileController, hasFixedSize = true, disableItemAnimation = true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        matrixProfileAppBarLayout.removeOnOffsetChangedListener(appBarStateChangeListener)
        matrixProfileRecyclerView.cleanup()
        appBarStateChangeListener = null
    }

    override fun invalidate() = withState(roomProfileViewModel) { state ->
        waiting_view.isVisible = state.isLoading

        state.roomSummary()?.also {
            if (it.membership.isLeft()) {
                Timber.w("The room has been left")
                activity?.finish()
            } else {
                roomProfileNameView.text = it.displayName
                matrixProfileToolbarTitleView.text = it.displayName
                roomProfileAliasView.setTextOrHide(it.canonicalAlias)
                val matrixItem = it.toMatrixItem()
                avatarRenderer.render(matrixItem, roomProfileAvatarView)
                avatarRenderer.render(matrixItem, matrixProfileToolbarAvatarImageView)
                roomProfileDecorationImageView.isVisible = it.roomEncryptionTrustLevel != null
                roomProfileDecorationImageView.setImageResource(it.roomEncryptionTrustLevel.toImageRes())
                matrixProfileDecorationToolbarAvatarImageView.setImageResource(it.roomEncryptionTrustLevel.toImageRes())

                roomProfileAvatarView.setOnClickListener { view ->
                    onAvatarClicked(view, matrixItem)
                }
                matrixProfileToolbarAvatarImageView.setOnClickListener { view ->
                    onAvatarClicked(view, matrixItem)
                }
            }
        }
        roomProfileController.setData(state)
    }

    // RoomProfileController.Callback

    override fun onLearnMoreClicked() {
        vectorBaseActivity.notImplemented()
    }

    override fun onEnableEncryptionClicked() {
        AlertDialog.Builder(requireActivity())
                .setTitle(R.string.room_settings_enable_encryption_dialog_title)
                .setMessage(R.string.room_settings_enable_encryption_dialog_content)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.room_settings_enable_encryption_dialog_submit) { _, _ ->
                    roomProfileViewModel.handle(RoomProfileAction.EnableEncryption)
                }
                .show()
    }

    override fun onMemberListClicked() {
        roomProfileSharedActionViewModel.post(RoomProfileSharedAction.OpenRoomMembers)
    }

    override fun onBannedMemberListClicked() {
        roomProfileSharedActionViewModel.post(RoomProfileSharedAction.OpenBannedRoomMembers)
    }

    override fun onSettingsClicked() {
        roomProfileSharedActionViewModel.post(RoomProfileSharedAction.OpenRoomSettings)
    }

    override fun onNotificationsClicked() {
        RoomListQuickActionsBottomSheet
                .newInstance(roomProfileArgs.roomId, RoomListActionsArgs.Mode.NOTIFICATIONS)
                .show(childFragmentManager, "ROOM_PROFILE_NOTIFICATIONS")
    }

    override fun onUploadsClicked() {
        roomProfileSharedActionViewModel.post(RoomProfileSharedAction.OpenRoomUploads)
    }

    override fun createShortcut() {
        // Ask the view model to prepare it...
        roomProfileViewModel.handle(RoomProfileAction.CreateShortcut)
    }

    private fun addShortcut(onShortcutReady: RoomProfileViewEvents.OnShortcutReady) {
        // ... and propose the user to add it
        ShortcutManagerCompat.requestPinShortcut(requireContext(), onShortcutReady.shortcutInfo, null)
    }

    override fun onLeaveRoomClicked() {
        AlertDialog.Builder(requireContext())
                .setTitle(R.string.room_participants_leave_prompt_title)
                .setMessage(R.string.room_participants_leave_prompt_msg)
                .setPositiveButton(R.string.leave) { _, _ ->
                    roomProfileViewModel.handle(RoomProfileAction.LeaveRoom)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
    }

    override fun onRoomIdClicked() {
        copyToClipboard(requireContext(), roomProfileArgs.roomId)
    }

    private fun onShareRoomProfile(permalink: String) {
        startSharePlainTextIntent(
                fragment = this,
                activityResultLauncher = null,
                chooserTitle = null,
                text = permalink
        )
    }

    private fun onAvatarClicked(view: View, matrixItem: MatrixItem.RoomItem) = withState(roomProfileViewModel) {
        matrixItem.avatarUrl
                ?.takeIf { it.isNotEmpty() }
                ?.let { avatarUrl ->
                    val intent = BigImageViewerActivity.newIntent(requireContext(), matrixItem.getBestName(), avatarUrl)
                    val options = ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), view, ViewCompat.getTransitionName(view) ?: "")
                    startActivity(intent, options.toBundle())
                }
    }
}
