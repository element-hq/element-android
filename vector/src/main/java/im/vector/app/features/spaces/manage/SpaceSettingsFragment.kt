/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.spaces.manage

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.app.R
import im.vector.app.core.dialogs.GalleryOrCameraDialogHelper
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.intent.getFilenameFromUri
import im.vector.app.core.platform.OnBackPressed
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.DrawableProvider
import im.vector.app.core.utils.toast
import im.vector.app.databinding.FragmentRoomSettingGenericBinding
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.roomprofile.RoomProfileArgs
import im.vector.app.features.roomprofile.settings.RoomSettingsAction
import im.vector.app.features.roomprofile.settings.RoomSettingsViewEvents
import im.vector.app.features.roomprofile.settings.RoomSettingsViewModel
import im.vector.app.features.roomprofile.settings.RoomSettingsViewState
import im.vector.app.features.roomprofile.settings.joinrule.RoomJoinRuleBottomSheet
import im.vector.app.features.roomprofile.settings.joinrule.RoomJoinRuleSharedActionViewModel
import org.matrix.android.sdk.api.session.room.model.GuestAccess
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibility
import org.matrix.android.sdk.api.session.room.model.RoomJoinRules
import org.matrix.android.sdk.api.util.toMatrixItem
import java.util.UUID
import javax.inject.Inject

class SpaceSettingsFragment @Inject constructor(
        private val epoxyController: SpaceSettingsController,
        private val colorProvider: ColorProvider,
        val viewModelFactory: RoomSettingsViewModel.Factory,
        private val avatarRenderer: AvatarRenderer,
        private val drawableProvider: DrawableProvider
) : VectorBaseFragment<FragmentRoomSettingGenericBinding>(),
        RoomSettingsViewModel.Factory,
        SpaceSettingsController.Callback,
        GalleryOrCameraDialogHelper.Listener,
        OnBackPressed {

    private val viewModel: RoomSettingsViewModel by fragmentViewModel()
    private val sharedViewModel: SpaceManageSharedViewModel by activityViewModel()

    private lateinit var roomJoinRuleSharedActionViewModel: RoomJoinRuleSharedActionViewModel

    private val galleryOrCameraDialogHelper = GalleryOrCameraDialogHelper(this, colorProvider)

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?) = FragmentRoomSettingGenericBinding.inflate(inflater)

    private val roomProfileArgs: RoomProfileArgs by args()

    override fun getMenuRes() = R.menu.vector_room_settings

    override fun create(initialState: RoomSettingsViewState): RoomSettingsViewModel {
        return viewModelFactory.create(initialState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar(views.roomSettingsToolbar)
        // roomProfileSharedActionViewModel = activityViewModelProvider.get(RoomProfileSharedActionViewModel::class.java)
//        setupRoomHistoryVisibilitySharedActionViewModel()
        setupRoomJoinRuleSharedActionViewModel()
        epoxyController.callback = this
        views.roomSettingsRecyclerView.configureWith(epoxyController, hasFixedSize = true)
        views.waitingView.waitingStatusText.setText(R.string.please_wait)
        views.waitingView.waitingStatusText.isVisible = true

        viewModel.observeViewEvents {
            when (it) {
                is RoomSettingsViewEvents.Failure -> showFailure(it.throwable)
                RoomSettingsViewEvents.Success -> showSuccess()
                RoomSettingsViewEvents.GoBack -> {
                    ignoreChanges = true
                    vectorBaseActivity.onBackPressed()
                }
            }.exhaustive
        }
    }

    override fun onDestroyView() {
        epoxyController.callback = null
        views.roomSettingsRecyclerView.cleanup()
        super.onDestroyView()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        withState(viewModel) { state ->
            menu.findItem(R.id.roomSettingsSaveAction).isVisible = state.showSaveAction
        }
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.roomSettingsSaveAction) {
            viewModel.handle(RoomSettingsAction.Save)
        }
        return super.onOptionsItemSelected(item)
    }

    private fun renderRoomSummary(state: RoomSettingsViewState) {
        views.waitingView.root.isVisible = state.isLoading

        state.roomSummary()?.let {
            views.roomSettingsToolbarTitleView.text = it.displayName
            views.roomSettingsToolbarTitleView.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    null,
                    drawableProvider.getDrawable(R.drawable.ic_beta_pill),
                    null
            )
            avatarRenderer.render(it.toMatrixItem(), views.roomSettingsToolbarAvatarImageView)
            views.roomSettingsDecorationToolbarAvatarImageView.render(it.roomEncryptionTrustLevel)
        }

        invalidateOptionsMenu()
    }

    override fun invalidate() = withState(viewModel) { state ->
        epoxyController.setData(state)
        renderRoomSummary(state)
    }

    private fun setupRoomJoinRuleSharedActionViewModel() {
        roomJoinRuleSharedActionViewModel = activityViewModelProvider.get(RoomJoinRuleSharedActionViewModel::class.java)
        roomJoinRuleSharedActionViewModel
                .observe()
                .subscribe { action ->
                    viewModel.handle(RoomSettingsAction.SetRoomJoinRule(action.roomJoinRule))
                }
                .disposeOnDestroyView()
    }

    private var ignoreChanges = false

    override fun onBackPressed(toolbarButton: Boolean): Boolean {
        if (ignoreChanges) return false

        return withState(viewModel) {
            return@withState if (it.showSaveAction) {
                MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.dialog_title_warning)
                        .setMessage(R.string.warning_unsaved_change)
                        .setPositiveButton(R.string.warning_unsaved_change_discard) { _, _ ->
                            viewModel.handle(RoomSettingsAction.Cancel)
                        }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
                true
            } else {
                false
            }
        }
    }

    private fun showSuccess() {
        activity?.toast(R.string.room_settings_save_success)
    }

    override fun onNameChanged(name: String) {
        viewModel.handle(RoomSettingsAction.SetRoomName(name))
    }

    override fun onTopicChanged(topic: String) {
        viewModel.handle(RoomSettingsAction.SetRoomTopic(topic))
    }

    override fun onHistoryVisibilityClicked() {
        // N/A for space settings screen
    }

    override fun onJoinRuleClicked() = withState(viewModel) { state ->
        val currentJoinRule = state.newRoomJoinRules.newJoinRules ?: state.currentRoomJoinRules
        RoomJoinRuleBottomSheet.newInstance(currentJoinRule)
                .show(childFragmentManager, "RoomJoinRuleBottomSheet")
    }

    override fun onToggleGuestAccess() = withState(viewModel) { state ->
        val currentGuestAccess = state.newRoomJoinRules.newGuestAccess ?: state.currentGuestAccess
        val toggled = if (currentGuestAccess == GuestAccess.Forbidden) GuestAccess.CanJoin else GuestAccess.Forbidden
        viewModel.handle(RoomSettingsAction.SetRoomGuestAccess(toggled))
    }

    override fun onDevTools() = withState(viewModel) { state ->
        navigator.openDevTools(requireContext(), state.roomId)
    }

    override fun onDevRoomSettings() = withState(viewModel) { state ->
        navigator.openRoomProfile(requireContext(), state.roomId)
    }

    override fun onManageRooms() {
        sharedViewModel.handle(SpaceManagedSharedAction.ManageRooms)
    }

    override fun setIsPublic(public: Boolean) {
        if (public) {
            viewModel.handle(RoomSettingsAction.SetRoomJoinRule(RoomJoinRules.PUBLIC))
            viewModel.handle(RoomSettingsAction.SetRoomHistoryVisibility(RoomHistoryVisibility.WORLD_READABLE))
        } else {
            viewModel.handle(RoomSettingsAction.SetRoomJoinRule(RoomJoinRules.INVITE))
            viewModel.handle(RoomSettingsAction.SetRoomHistoryVisibility(RoomHistoryVisibility.INVITED))
        }
    }

    override fun onRoomAliasesClicked() {
        sharedViewModel.handle(SpaceManagedSharedAction.OpenSpaceAliasesSettings)
    }

    override fun onImageReady(uri: Uri?) {
        uri ?: return
        viewModel.handle(
                RoomSettingsAction.SetAvatarAction(
                        RoomSettingsViewState.AvatarAction.UpdateAvatar(
                                newAvatarUri = uri,
                                newAvatarFileName = getFilenameFromUri(requireContext(), uri) ?: UUID.randomUUID().toString())
                )
        )
    }

    override fun onAvatarDelete() {
        withState(viewModel) {
            when (it.avatarAction) {
                RoomSettingsViewState.AvatarAction.None -> {
                    viewModel.handle(RoomSettingsAction.SetAvatarAction(RoomSettingsViewState.AvatarAction.DeleteAvatar))
                }
                RoomSettingsViewState.AvatarAction.DeleteAvatar -> {
                    /* Should not happen */
                }
                is RoomSettingsViewState.AvatarAction.UpdateAvatar -> {
                    // Cancel the update of the avatar
                    viewModel.handle(RoomSettingsAction.SetAvatarAction(RoomSettingsViewState.AvatarAction.None))
                }
            }
        }
    }

    override fun onAvatarChange() {
        galleryOrCameraDialogHelper.show()
    }
}
