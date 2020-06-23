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

package im.vector.riotx.features.roomprofile

import android.os.Bundle
import android.os.Parcelable
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.matrix.android.api.session.room.notification.RoomNotificationState
import im.vector.matrix.android.api.util.MatrixItem
import im.vector.matrix.android.api.util.toMatrixItem
import im.vector.riotx.R
import im.vector.riotx.core.animations.AppBarStateChangeListener
import im.vector.riotx.core.animations.MatrixItemAppBarStateChangeListener
import im.vector.riotx.core.extensions.cleanup
import im.vector.riotx.core.extensions.configureWith
import im.vector.riotx.core.extensions.exhaustive
import im.vector.riotx.core.extensions.setTextOrHide
import im.vector.riotx.core.platform.VectorBaseFragment
import im.vector.riotx.core.utils.copyToClipboard
import im.vector.riotx.core.utils.startSharePlainTextIntent
import im.vector.riotx.features.crypto.util.toImageRes
import im.vector.riotx.features.home.AvatarRenderer
import im.vector.riotx.features.home.room.list.actions.RoomListActionsArgs
import im.vector.riotx.features.home.room.list.actions.RoomListQuickActionsBottomSheet
import im.vector.riotx.features.home.room.list.actions.RoomListQuickActionsSharedAction
import im.vector.riotx.features.home.room.list.actions.RoomListQuickActionsSharedActionViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_matrix_profile.*
import kotlinx.android.synthetic.main.view_stub_room_profile_header.*
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
) : VectorBaseFragment(), RoomProfileController.Callback {

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
                is RoomProfileViewEvents.Loading            -> showLoading(it.message)
                is RoomProfileViewEvents.Failure            -> showFailure(it.throwable)
                is RoomProfileViewEvents.OnLeaveRoomSuccess -> onLeaveRoom()
                is RoomProfileViewEvents.ShareRoomProfile   -> onShareRoomProfile(it.permalink)
            }.exhaustive
        }
        roomListQuickActionsSharedActionViewModel
                .observe()
                .subscribe { handleQuickActions(it) }
                .disposeOnDestroyView()
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
        is RoomListQuickActionsSharedAction.NotificationsAll              ->
            roomProfileViewModel.handle(RoomProfileAction.ChangeRoomNotificationState(RoomNotificationState.ALL_MESSAGES))
        is RoomListQuickActionsSharedAction.NotificationsMentionsKeywords ->
            roomProfileViewModel.handle(RoomProfileAction.ChangeRoomNotificationState(RoomNotificationState.MENTIONS_AND_KEYWORDS))
        is RoomListQuickActionsSharedAction.NotificationsNone             ->
            roomProfileViewModel.handle(RoomProfileAction.ChangeRoomNotificationState(RoomNotificationState.NONE))
        else                                                              ->
            Timber.v("$action not handled")
    }

    private fun onLeaveRoom() {
        vectorBaseActivity.finish()
    }

    private fun showError(throwable: Throwable) {
        showErrorInSnackbar(throwable)
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
        state.roomSummary()?.also {
            if (it.membership.isLeft()) {
                Timber.w("The room has been left")
                activity?.finish()
            } else {
                roomProfileNameView.text = it.displayName
                matrixProfileToolbarTitleView.text = it.displayName
                roomProfileAliasView.setTextOrHide(it.canonicalAlias)
                roomProfileTopicView.setTextOrHide(it.topic)
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

    override fun onMemberListClicked() {
        roomProfileSharedActionViewModel.post(RoomProfileSharedAction.OpenRoomMembers)
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
        startSharePlainTextIntent(fragment = this, chooserTitle = null, text = permalink)
    }

    private fun onAvatarClicked(view: View, matrixItem: MatrixItem.RoomItem) {
        navigator.openBigImageViewer(requireActivity(), view, matrixItem)
    }
}
