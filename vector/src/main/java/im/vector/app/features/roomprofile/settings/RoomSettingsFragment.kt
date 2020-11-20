/*
 * Copyright 2020 New Vector Ltd
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
 */

package im.vector.app.features.roomprofile.settings

import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.app.R
import im.vector.app.core.dialogs.GalleryOrCameraDialogHelper
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.intent.getFilenameFromUri
import im.vector.app.core.platform.OnBackPressed
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.utils.toast
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.timeline.format.RoomHistoryVisibilityFormatter
import im.vector.app.features.roomprofile.RoomProfileArgs
import kotlinx.android.synthetic.main.fragment_room_setting_generic.*
import kotlinx.android.synthetic.main.merge_overlay_waiting_view.*
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibility
import org.matrix.android.sdk.api.session.room.model.RoomHistoryVisibilityContent
import org.matrix.android.sdk.api.util.toMatrixItem
import java.util.UUID
import javax.inject.Inject

class RoomSettingsFragment @Inject constructor(
        val viewModelFactory: RoomSettingsViewModel.Factory,
        private val controller: RoomSettingsController,
        private val roomHistoryVisibilityFormatter: RoomHistoryVisibilityFormatter,
        colorProvider: ColorProvider,
        private val avatarRenderer: AvatarRenderer
) :
        VectorBaseFragment(),
        RoomSettingsController.Callback,
        OnBackPressed,
        GalleryOrCameraDialogHelper.Listener {

    private val viewModel: RoomSettingsViewModel by fragmentViewModel()
    private val roomProfileArgs: RoomProfileArgs by args()
    private val galleryOrCameraDialogHelper = GalleryOrCameraDialogHelper(this, colorProvider)

    override fun getLayoutResId() = R.layout.fragment_room_setting_generic

    override fun getMenuRes() = R.menu.vector_room_settings

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        controller.callback = this
        setupToolbar(roomSettingsToolbar)
        roomSettingsRecyclerView.configureWith(controller, hasFixedSize = true)
        waiting_view_status_text.setText(R.string.please_wait)
        waiting_view_status_text.isVisible = true

        viewModel.observeViewEvents {
            when (it) {
                is RoomSettingsViewEvents.Failure -> showFailure(it.throwable)
                RoomSettingsViewEvents.Success    -> showSuccess()
                RoomSettingsViewEvents.GoBack     -> {
                    ignoreChanges = true
                    vectorBaseActivity.onBackPressed()
                }
            }.exhaustive
        }
    }

    private fun showSuccess() {
        activity?.toast(R.string.room_settings_save_success)
    }

    override fun onDestroyView() {
        controller.callback = null
        roomSettingsRecyclerView.cleanup()
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

    override fun invalidate() = withState(viewModel) { viewState ->
        controller.setData(viewState)
        renderRoomSummary(viewState)
    }

    private fun renderRoomSummary(state: RoomSettingsViewState) {
        waiting_view.isVisible = state.isLoading

        state.roomSummary()?.let {
            roomSettingsToolbarTitleView.text = it.displayName
            avatarRenderer.render(it.toMatrixItem(), roomSettingsToolbarAvatarImageView)
        }

        invalidateOptionsMenu()
    }

    override fun onNameChanged(name: String) {
        viewModel.handle(RoomSettingsAction.SetRoomName(name))
    }

    override fun onTopicChanged(topic: String) {
        viewModel.handle(RoomSettingsAction.SetRoomTopic(topic))
    }

    override fun onHistoryVisibilityClicked() = withState(viewModel) { state ->
        val historyVisibilities = arrayOf(
                RoomHistoryVisibility.SHARED,
                RoomHistoryVisibility.INVITED,
                RoomHistoryVisibility.JOINED,
                RoomHistoryVisibility.WORLD_READABLE
        )
        val currentHistoryVisibility =
                state.newHistoryVisibility ?: state.historyVisibilityEvent?.getClearContent().toModel<RoomHistoryVisibilityContent>()?.historyVisibility
        val currentHistoryVisibilityIndex = historyVisibilities.indexOf(currentHistoryVisibility)

        AlertDialog.Builder(requireContext()).apply {
            setTitle(R.string.room_settings_room_read_history_rules_pref_title)
            setSingleChoiceItems(
                    historyVisibilities
                            .map { roomHistoryVisibilityFormatter.format(it) }
                            .toTypedArray(),
                    currentHistoryVisibilityIndex) { dialog, which ->
                if (which != currentHistoryVisibilityIndex) {
                    viewModel.handle(RoomSettingsAction.SetRoomHistoryVisibility(historyVisibilities[which]))
                }
                dialog.cancel()
            }
            show()
        }
        return@withState
    }

    override fun onAliasChanged(alias: String) {
        viewModel.handle(RoomSettingsAction.SetRoomCanonicalAlias(alias))
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
                    Unit
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

    private var ignoreChanges = false

    override fun onBackPressed(toolbarButton: Boolean): Boolean {
        if (ignoreChanges) return false

        return withState(viewModel) {
            return@withState if (it.showSaveAction) {
                AlertDialog.Builder(requireContext())
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
}
