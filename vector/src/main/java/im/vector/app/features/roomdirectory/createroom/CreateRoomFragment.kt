/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomdirectory.createroom

import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.dialogs.GalleryOrCameraDialogHelper
import im.vector.app.core.dialogs.GalleryOrCameraDialogHelperFactory
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.OnBackPressed
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentCreateRoomBinding
import im.vector.app.features.analytics.plan.ViewRoom
import im.vector.app.features.navigation.Navigator
import im.vector.app.features.roomdirectory.RoomDirectorySharedAction
import im.vector.app.features.roomdirectory.RoomDirectorySharedActionViewModel
import im.vector.app.features.roomprofile.settings.joinrule.RoomJoinRuleBottomSheet
import im.vector.app.features.roomprofile.settings.joinrule.RoomJoinRuleSharedActionViewModel
import im.vector.app.features.roomprofile.settings.joinrule.toOption
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.session.room.failure.CreateRoomFailure
import org.matrix.android.sdk.api.session.room.model.RoomJoinRules
import javax.inject.Inject

@Parcelize
data class CreateRoomArgs(
        val initialName: String,
        val parentSpaceId: String? = null,
        val isSpace: Boolean = false,
        val openAfterCreate: Boolean = true
) : Parcelable

@AndroidEntryPoint
class CreateRoomFragment :
        VectorBaseFragment<FragmentCreateRoomBinding>(),
        CreateRoomController.Listener,
        GalleryOrCameraDialogHelper.Listener,
        OnBackPressed {

    @Inject lateinit var createRoomController: CreateRoomController
    @Inject lateinit var createSpaceController: CreateSubSpaceController
    @Inject lateinit var galleryOrCameraDialogHelperFactory: GalleryOrCameraDialogHelperFactory

    private lateinit var sharedActionViewModel: RoomDirectorySharedActionViewModel
    private val viewModel: CreateRoomViewModel by fragmentViewModel()
    private val args: CreateRoomArgs by args()

    private lateinit var roomJoinRuleSharedActionViewModel: RoomJoinRuleSharedActionViewModel

    private lateinit var galleryOrCameraDialogHelper: GalleryOrCameraDialogHelper

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentCreateRoomBinding {
        return FragmentCreateRoomBinding.inflate(inflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        galleryOrCameraDialogHelper = galleryOrCameraDialogHelperFactory.create(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedActionViewModel = activityViewModelProvider.get(RoomDirectorySharedActionViewModel::class.java)
        setupRoomJoinRuleSharedActionViewModel()
        setupWaitingView()
        setupRecyclerView()
        setupToolbar(views.createRoomToolbar)
                .setTitle(if (args.isSpace) CommonStrings.create_new_space else CommonStrings.create_new_room)
                .allowBack(useCross = true)
        viewModel.observeViewEvents {
            when (it) {
                CreateRoomViewEvents.Quit -> {
                    @Suppress("DEPRECATION")
                    vectorBaseActivity.onBackPressed()
                }
                is CreateRoomViewEvents.Failure -> showFailure(it.throwable)
            }
        }
    }

    private fun setupRoomJoinRuleSharedActionViewModel() {
        roomJoinRuleSharedActionViewModel = activityViewModelProvider.get(RoomJoinRuleSharedActionViewModel::class.java)
        roomJoinRuleSharedActionViewModel
                .stream()
                .onEach { action ->
                    viewModel.handle(CreateRoomAction.SetVisibility(action.roomJoinRule))
                }
                .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    override fun showFailure(throwable: Throwable) {
        // Note: RoomAliasError are displayed directly in the form
        if (throwable !is CreateRoomFailure.AliasError) {
            super.showFailure(throwable)
        }
    }

    private fun setupWaitingView() {
        views.waitingView.waitingStatusText.isVisible = true
        views.waitingView.waitingStatusText.setText(
                if (args.isSpace) CommonStrings.create_space_in_progress else CommonStrings.create_room_in_progress
        )
    }

    override fun onDestroyView() {
        views.createRoomForm.cleanup()
        createRoomController.listener = null
        createSpaceController.listener = null
        super.onDestroyView()
    }

    private fun setupRecyclerView() {
        if (args.isSpace) {
            views.createRoomForm.configureWith(createSpaceController)
            createSpaceController.listener = this
        } else {
            views.createRoomForm.configureWith(createRoomController)
            createRoomController.listener = this
        }
    }

    override fun onAvatarDelete() {
        viewModel.handle(CreateRoomAction.SetAvatar(null))
    }

    override fun onAvatarChange() {
        galleryOrCameraDialogHelper.show()
    }

    override fun onImageReady(uri: Uri?) {
        viewModel.handle(CreateRoomAction.SetAvatar(uri))
    }

    override fun onNameChange(newName: String) {
        viewModel.handle(CreateRoomAction.SetName(newName))
    }

    override fun onTopicChange(newTopic: String) {
        viewModel.handle(CreateRoomAction.SetTopic(newTopic))
    }

    override fun selectVisibility() = withState(viewModel) { state ->
        // If restricted is supported and the user is in the context of a parent space
        // then show restricted option.
        val allowed = if (state.supportsRestricted && state.parentSpaceId != null) {
            listOf(RoomJoinRules.INVITE, RoomJoinRules.PUBLIC, RoomJoinRules.RESTRICTED)
        } else {
            listOf(RoomJoinRules.INVITE, RoomJoinRules.PUBLIC)
        }
        RoomJoinRuleBottomSheet.newInstance(
                state.roomJoinRules,
                allowed.map { it.toOption(false) },
                state.isSubSpace,
                state.parentSpaceSummary?.displayName
        )
                .show(childFragmentManager, "RoomJoinRuleBottomSheet")
    }
//    override fun setIsPublic(isPublic: Boolean) {
//        viewModel.handle(CreateRoomAction.SetIsPublic(isPublic))
//    }

    override fun setAliasLocalPart(aliasLocalPart: String) {
        viewModel.handle(CreateRoomAction.SetRoomAliasLocalPart(aliasLocalPart))
    }

    override fun setIsEncrypted(isEncrypted: Boolean) {
        viewModel.handle(CreateRoomAction.SetIsEncrypted(isEncrypted))
    }

    override fun toggleShowAdvanced() {
        viewModel.handle(CreateRoomAction.ToggleShowAdvanced)
    }

    override fun setDisableFederation(disableFederation: Boolean) {
        viewModel.handle(CreateRoomAction.DisableFederation(disableFederation))
    }

    override fun submit() {
        viewModel.handle(CreateRoomAction.Create)
    }

    override fun onBackPressed(toolbarButton: Boolean): Boolean {
        return withState(viewModel) {
            return@withState if (!it.isEmpty()) {
                MaterialAlertDialogBuilder(requireContext())
                        .setTitle(CommonStrings.dialog_title_warning)
                        .setMessage(CommonStrings.warning_room_not_created_yet)
                        .setPositiveButton(CommonStrings.yes) { _, _ ->
                            viewModel.handle(CreateRoomAction.Reset)
                        }
                        .setNegativeButton(CommonStrings.no, null)
                        .show()
                true
            } else {
                false
            }
        }
    }

    override fun invalidate() = withState(viewModel) { state ->
        val async = state.asyncCreateRoomRequest
        views.waitingView.root.isVisible = async is Loading
        if (async is Success) {
            val roomId = async()
            // Navigate to freshly created room
            if (state.openAfterCreate) {
                if (state.isSubSpace) {
                    navigator.switchToSpace(
                            requireContext(),
                            roomId,
                            Navigator.PostSwitchSpaceAction.None
                    )
                } else {
                    navigator.openRoom(
                            context = requireActivity(),
                            roomId = roomId,
                            trigger = ViewRoom.Trigger.Created
                    )
                }
            }

            sharedActionViewModel.post(RoomDirectorySharedAction.CreateRoomSuccess(roomId))
            sharedActionViewModel.post(RoomDirectorySharedAction.Close)
        } else {
            // Populate list with Epoxy
            if (args.isSpace) {
                createSpaceController.setData(state)
            } else {
                createRoomController.setData(state)
            }
        }
    }
}
