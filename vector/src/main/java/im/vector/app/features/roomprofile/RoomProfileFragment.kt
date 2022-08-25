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
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.animations.AppBarStateChangeListener
import im.vector.app.core.animations.MatrixItemAppBarStateChangeListener
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.extensions.copyOnLongClick
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.platform.VectorMenuProvider
import im.vector.app.core.utils.copyToClipboard
import im.vector.app.core.utils.startSharePlainTextIntent
import im.vector.app.databinding.FragmentMatrixProfileBinding
import im.vector.app.databinding.ViewStubRoomProfileHeaderBinding
import im.vector.app.features.analytics.plan.Interaction
import im.vector.app.features.analytics.plan.MobileScreen
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.RoomDetailPendingAction
import im.vector.app.features.home.room.detail.RoomDetailPendingActionStore
import im.vector.app.features.home.room.detail.upgrade.MigrateRoomBottomSheet
import im.vector.app.features.home.room.list.actions.RoomListQuickActionsSharedAction
import im.vector.app.features.home.room.list.actions.RoomListQuickActionsSharedActionViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.session.room.notification.RoomNotificationState
import org.matrix.android.sdk.api.util.toMatrixItem
import timber.log.Timber
import javax.inject.Inject

@Parcelize
data class RoomProfileArgs(
        val roomId: String
) : Parcelable

@AndroidEntryPoint
class RoomProfileFragment :
        VectorBaseFragment<FragmentMatrixProfileBinding>(),
        RoomProfileController.Callback,
        VectorMenuProvider {

    @Inject lateinit var roomProfileController: RoomProfileController
    @Inject lateinit var avatarRenderer: AvatarRenderer
    @Inject lateinit var roomDetailPendingActionStore: RoomDetailPendingActionStore

    private lateinit var headerViews: ViewStubRoomProfileHeaderBinding

    private val roomProfileArgs: RoomProfileArgs by args()
    private lateinit var roomListQuickActionsSharedActionViewModel: RoomListQuickActionsSharedActionViewModel
    private lateinit var roomProfileSharedActionViewModel: RoomProfileSharedActionViewModel
    private val roomProfileViewModel: RoomProfileViewModel by fragmentViewModel()

    private var appBarStateChangeListener: AppBarStateChangeListener? = null

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentMatrixProfileBinding {
        return FragmentMatrixProfileBinding.inflate(inflater, container, false)
    }

    override fun getMenuRes() = R.menu.vector_room_profile

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        analyticsScreenName = MobileScreen.ScreenName.RoomDetails
        setFragmentResultListener(MigrateRoomBottomSheet.REQUEST_KEY) { _, bundle ->
            bundle.getString(MigrateRoomBottomSheet.BUNDLE_KEY_REPLACEMENT_ROOM)?.let { replacementRoomId ->
                roomDetailPendingActionStore.data = RoomDetailPendingAction.OpenRoom(replacementRoomId, closeCurrentRoom = true)
                vectorBaseActivity.finish()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        roomListQuickActionsSharedActionViewModel = activityViewModelProvider.get(RoomListQuickActionsSharedActionViewModel::class.java)
        roomProfileSharedActionViewModel = activityViewModelProvider.get(RoomProfileSharedActionViewModel::class.java)
        val headerView = views.matrixProfileHeaderView.let {
            it.layoutResource = R.layout.view_stub_room_profile_header
            it.inflate()
        }
        headerViews = ViewStubRoomProfileHeaderBinding.bind(headerView)
        setupWaitingView()
        setupToolbar(views.matrixProfileToolbar)
                .allowBack()
        setupRecyclerView()
        appBarStateChangeListener = MatrixItemAppBarStateChangeListener(
                headerView,
                listOf(
                        views.matrixProfileToolbarAvatarImageView,
                        views.matrixProfileToolbarTitleView,
                        views.matrixProfileDecorationToolbarAvatarImageView
                )
        )
        views.matrixProfileAppBarLayout.addOnOffsetChangedListener(appBarStateChangeListener)
        roomProfileViewModel.observeViewEvents {
            when (it) {
                is RoomProfileViewEvents.Loading -> showLoading(it.message)
                is RoomProfileViewEvents.Failure -> showFailure(it.throwable)
                is RoomProfileViewEvents.ShareRoomProfile -> onShareRoomProfile(it.permalink)
                is RoomProfileViewEvents.OnShortcutReady -> addShortcut(it)
                RoomProfileViewEvents.DismissLoading -> dismissLoadingDialog()
            }
        }
        roomListQuickActionsSharedActionViewModel
                .stream()
                .onEach { handleQuickActions(it) }
                .launchIn(viewLifecycleOwner.lifecycleScope)
        setupClicks()
        setupLongClicks()
    }

    private fun setupWaitingView() {
        views.waitingView.waitingStatusText.setText(R.string.please_wait)
        views.waitingView.waitingStatusText.isVisible = true
    }

    private fun setupClicks() {
        // Shortcut to room settings
        setOf(
                headerViews.roomProfileNameView,
                views.matrixProfileToolbarTitleView
        ).forEach {
            it.debouncedClicks {
                roomProfileSharedActionViewModel.post(RoomProfileSharedAction.OpenRoomSettings)
            }
        }
        // Shortcut to room alias
        headerViews.roomProfileAliasView.debouncedClicks {
            roomProfileSharedActionViewModel.post(RoomProfileSharedAction.OpenRoomAliasesSettings)
        }
        // Open Avatar
        setOf(
                headerViews.roomProfileAvatarView,
                views.matrixProfileToolbarAvatarImageView
        ).forEach { view ->
            view.debouncedClicks { onAvatarClicked(view) }
        }
    }

    private fun setupLongClicks() {
        headerViews.roomProfileNameView.copyOnLongClick()
        headerViews.roomProfileAliasView.copyOnLongClick()
    }

    override fun handleMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.roomProfileShareAction -> {
                roomProfileViewModel.handle(RoomProfileAction.ShareRoomProfile)
                true
            }
            else -> false
        }
    }

    private fun handleQuickActions(action: RoomListQuickActionsSharedAction) = when (action) {
        is RoomListQuickActionsSharedAction.NotificationsAllNoisy -> {
            roomProfileViewModel.handle(RoomProfileAction.ChangeRoomNotificationState(RoomNotificationState.ALL_MESSAGES_NOISY))
        }
        is RoomListQuickActionsSharedAction.NotificationsAll -> {
            roomProfileViewModel.handle(RoomProfileAction.ChangeRoomNotificationState(RoomNotificationState.ALL_MESSAGES))
        }
        is RoomListQuickActionsSharedAction.NotificationsMentionsOnly -> {
            roomProfileViewModel.handle(RoomProfileAction.ChangeRoomNotificationState(RoomNotificationState.MENTIONS_ONLY))
        }
        is RoomListQuickActionsSharedAction.NotificationsMute -> {
            roomProfileViewModel.handle(RoomProfileAction.ChangeRoomNotificationState(RoomNotificationState.MUTE))
        }
        else -> Timber.v("$action not handled")
    }

    private fun setupRecyclerView() {
        roomProfileController.callback = this
        views.matrixProfileRecyclerView.configureWith(roomProfileController, hasFixedSize = true, disableItemAnimation = true)
    }

    override fun onDestroyView() {
        views.matrixProfileAppBarLayout.removeOnOffsetChangedListener(appBarStateChangeListener)
        views.matrixProfileRecyclerView.cleanup()
        appBarStateChangeListener = null
        super.onDestroyView()
    }

    override fun invalidate() = withState(roomProfileViewModel) { state ->
        views.waitingView.root.isVisible = state.isLoading

        state.roomSummary()?.let {
            if (it.membership.isLeft()) {
                Timber.w("The room has been left")
                activity?.finish()
            } else {
                headerViews.roomProfileNameView.text = it.displayName
                views.matrixProfileToolbarTitleView.text = it.displayName
                headerViews.roomProfileAliasView.setTextOrHide(it.canonicalAlias)
                val matrixItem = it.toMatrixItem()
                avatarRenderer.render(matrixItem, headerViews.roomProfileAvatarView)
                avatarRenderer.render(matrixItem, views.matrixProfileToolbarAvatarImageView)
                headerViews.roomProfileDecorationImageView.render(it.roomEncryptionTrustLevel)
                views.matrixProfileDecorationToolbarAvatarImageView.render(it.roomEncryptionTrustLevel)
                headerViews.roomProfilePresenceImageView.render(it.isDirect, it.directUserPresence)
                headerViews.roomProfilePublicImageView.isVisible = it.isPublic && !it.isDirect
            }
        }
        roomProfileController.setData(state)
    }

    // RoomProfileController.Callback

    override fun onLearnMoreClicked() {
        vectorBaseActivity.notImplemented()
    }

    override fun onEnableEncryptionClicked() {
        MaterialAlertDialogBuilder(requireActivity())
                .setTitle(R.string.room_settings_enable_encryption_dialog_title)
                .setMessage(R.string.room_settings_enable_encryption_dialog_content)
                .setNegativeButton(R.string.action_cancel, null)
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
        roomProfileSharedActionViewModel.post(RoomProfileSharedAction.OpenRoomNotificationSettings)
    }

    override fun onUploadsClicked() {
        roomProfileSharedActionViewModel.post(RoomProfileSharedAction.OpenRoomUploads)
    }

    override fun createShortcut() {
        // Ask the view model to prepare it...
        roomProfileViewModel.handle(RoomProfileAction.CreateShortcut)
        analyticsTracker.capture(
                Interaction(
                        index = null,
                        interactionType = null,
                        name = Interaction.Name.MobileRoomAddHome
                )
        )
    }

    private fun addShortcut(onShortcutReady: RoomProfileViewEvents.OnShortcutReady) {
        // ... and propose the user to add it
        ShortcutManagerCompat.requestPinShortcut(requireContext(), onShortcutReady.shortcutInfo, null)
    }

    override fun onLeaveRoomClicked() {
        val isPublicRoom = roomProfileViewModel.isPublicRoom()
        val message = buildString {
            append(getString(R.string.room_participants_leave_prompt_msg))
            if (!isPublicRoom) {
                append("\n\n")
                append(getString(R.string.room_participants_leave_private_warning))
            }
        }
        MaterialAlertDialogBuilder(requireContext(), if (isPublicRoom) 0 else R.style.ThemeOverlay_Vector_MaterialAlertDialog_Destructive)
                .setTitle(R.string.room_participants_leave_prompt_title)
                .setMessage(message)
                .setPositiveButton(R.string.action_leave) { _, _ ->
                    roomProfileViewModel.handle(RoomProfileAction.LeaveRoom)
                }
                .setNegativeButton(R.string.action_cancel, null)
                .show()
    }

    override fun onRoomAliasesClicked() {
        roomProfileSharedActionViewModel.post(RoomProfileSharedAction.OpenRoomAliasesSettings)
    }

    override fun onRoomPermissionsClicked() {
        roomProfileSharedActionViewModel.post(RoomProfileSharedAction.OpenRoomPermissionsSettings)
    }

    override fun restoreEncryptionState() {
        roomProfileViewModel.handle(RoomProfileAction.RestoreEncryptionState)
    }

    override fun onRoomIdClicked() {
        copyToClipboard(requireContext(), roomProfileArgs.roomId)
    }

    override fun onRoomDevToolsClicked() {
        navigator.openDevTools(requireContext(), roomProfileArgs.roomId)
    }

    override fun onUrlInTopicLongClicked(url: String) {
        copyToClipboard(requireContext(), url, true)
    }

    override fun doMigrateToVersion(newVersion: String) {
        MigrateRoomBottomSheet.newInstance(roomProfileArgs.roomId, newVersion)
                .show(parentFragmentManager, "migrate")
    }

    private fun onShareRoomProfile(permalink: String) {
        startSharePlainTextIntent(
                context = requireContext(),
                activityResultLauncher = null,
                chooserTitle = null,
                text = permalink
        )
    }

    private fun onAvatarClicked(view: View) = withState(roomProfileViewModel) { state ->
        state.roomSummary()?.toMatrixItem()?.let { matrixItem ->
            navigator.openBigImageViewer(requireActivity(), view, matrixItem)
        }
    }
}
