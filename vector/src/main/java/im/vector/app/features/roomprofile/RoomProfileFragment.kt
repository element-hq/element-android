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

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.yalantis.ucrop.UCrop
import im.vector.app.R
import im.vector.app.core.animations.AppBarStateChangeListener
import im.vector.app.core.animations.MatrixItemAppBarStateChangeListener
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.extensions.copyOnLongClick
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.core.intent.getFilenameFromUri
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.utils.PERMISSIONS_FOR_TAKING_PHOTO
import im.vector.app.core.utils.checkPermissions
import im.vector.app.core.utils.copyToClipboard
import im.vector.app.core.utils.registerForPermissionsResult
import im.vector.app.core.utils.startSharePlainTextIntent
import im.vector.app.features.crypto.util.toImageRes
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.list.actions.RoomListActionsArgs
import im.vector.app.features.home.room.list.actions.RoomListQuickActionsBottomSheet
import im.vector.app.features.home.room.list.actions.RoomListQuickActionsSharedAction
import im.vector.app.features.home.room.list.actions.RoomListQuickActionsSharedActionViewModel
import im.vector.app.features.media.BigImageViewerActivity
import im.vector.app.features.media.createUCropWithDefaultSettings
import im.vector.lib.multipicker.MultiPicker
import im.vector.lib.multipicker.entity.MultiPickerImageType
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_matrix_profile.*
import kotlinx.android.synthetic.main.view_stub_room_profile_header.*
import org.matrix.android.sdk.api.session.room.notification.RoomNotificationState
import org.matrix.android.sdk.api.util.MatrixItem
import org.matrix.android.sdk.api.util.toMatrixItem
import timber.log.Timber
import java.io.File
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
                is RoomProfileViewEvents.ShareRoomProfile   -> onShareRoomProfile(it.permalink)
                RoomProfileViewEvents.OnChangeAvatarSuccess -> dismissLoadingDialog()
                is RoomProfileViewEvents.OnShortcutReady    -> addShortcut(it)
            }.exhaustive
        }
        roomListQuickActionsSharedActionViewModel
                .observe()
                .subscribe { handleQuickActions(it) }
                .disposeOnDestroyView()
        setupLongClicks()
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
        if (matrixItem.avatarUrl?.isNotEmpty() == true) {
            val intent = BigImageViewerActivity.newIntent(requireContext(), matrixItem.getBestName(), matrixItem.avatarUrl!!, it.canChangeAvatar)
            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), view, ViewCompat.getTransitionName(view) ?: "")
            bigImageStartForActivityResult.launch(intent, options)
        } else if (it.canChangeAvatar) {
            showAvatarSelector()
        }
    }

    private fun showAvatarSelector() {
        AlertDialog.Builder(requireContext())
                .setItems(arrayOf(
                        getString(R.string.attachment_type_camera),
                        getString(R.string.attachment_type_gallery)
                )) { dialog, which ->
                    dialog.cancel()
                    onAvatarTypeSelected(isCamera = (which == 0))
                }
                .show()
    }

    private val takePhotoPermissionActivityResultLauncher = registerForPermissionsResult { allGranted ->
        if (allGranted) {
            onAvatarTypeSelected(true)
        }
    }

    private var avatarCameraUri: Uri? = null
    private fun onAvatarTypeSelected(isCamera: Boolean) {
        if (isCamera) {
            if (checkPermissions(PERMISSIONS_FOR_TAKING_PHOTO, requireActivity(), takePhotoPermissionActivityResultLauncher)) {
                avatarCameraUri = MultiPicker.get(MultiPicker.CAMERA).startWithExpectingFile(requireActivity(), takePhotoActivityResultLauncher)
            }
        } else {
            MultiPicker.get(MultiPicker.IMAGE).single().startWith(pickImageActivityResultLauncher)
        }
    }

    private fun onRoomAvatarSelected(image: MultiPickerImageType) {
        val destinationFile = File(requireContext().cacheDir, "${image.displayName}_edited_image_${System.currentTimeMillis()}")
        val uri = image.contentUri
        createUCropWithDefaultSettings(requireContext(), uri, destinationFile.toUri(), image.displayName)
                .apply { withAspectRatio(1f, 1f) }
                .start(requireContext(), this)
    }

    private val takePhotoActivityResultLauncher = registerStartForActivityResult { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            avatarCameraUri?.let { uri ->
                MultiPicker.get(MultiPicker.CAMERA)
                        .getTakenPhoto(requireContext(), uri)
                        ?.let {
                            onRoomAvatarSelected(it)
                        }
            }
        }
    }

    private val pickImageActivityResultLauncher = registerStartForActivityResult { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            MultiPicker
                    .get(MultiPicker.IMAGE)
                    .getSelectedFiles(requireContext(), activityResult.data)
                    .firstOrNull()?.let {
                        onRoomAvatarSelected(it)
                    }
        }
    }

    private val bigImageStartForActivityResult = registerStartForActivityResult { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            activityResult.data?.let { onAvatarCropped(it.data) }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // TODO handle this one (Ucrop lib)
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                UCrop.REQUEST_CROP -> data?.let { onAvatarCropped(UCrop.getOutput(it)) }
            }
        }
    }

    private fun onAvatarCropped(uri: Uri?) {
        if (uri != null) {
            roomProfileViewModel.handle(RoomProfileAction.ChangeRoomAvatar(uri, getFilenameFromUri(context, uri)))
        } else {
            Toast.makeText(requireContext(), "Cannot retrieve cropped value", Toast.LENGTH_SHORT).show()
        }
    }
}
