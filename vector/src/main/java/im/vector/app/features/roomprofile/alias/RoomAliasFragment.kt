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

package im.vector.app.features.roomprofile.alias

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.utils.shareText
import im.vector.app.core.utils.toast
import im.vector.app.databinding.FragmentRoomSettingGenericBinding
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.roomprofile.RoomProfileArgs
import im.vector.app.features.roomprofile.alias.detail.RoomAliasBottomSheet
import im.vector.app.features.roomprofile.alias.detail.RoomAliasBottomSheetSharedAction
import im.vector.app.features.roomprofile.alias.detail.RoomAliasBottomSheetSharedActionViewModel

import org.matrix.android.sdk.api.session.room.alias.RoomAliasError
import org.matrix.android.sdk.api.session.room.model.RoomDirectoryVisibility
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

class RoomAliasFragment @Inject constructor(
        val viewModelFactory: RoomAliasViewModel.Factory,
        private val controller: RoomAliasController,
        private val avatarRenderer: AvatarRenderer
) :
        VectorBaseFragment<FragmentRoomSettingGenericBinding>(),
        RoomAliasController.Callback {

    private val viewModel: RoomAliasViewModel by fragmentViewModel()
    private lateinit var sharedActionViewModel: RoomAliasBottomSheetSharedActionViewModel

    private val roomProfileArgs: RoomProfileArgs by args()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentRoomSettingGenericBinding {
        return FragmentRoomSettingGenericBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedActionViewModel = activityViewModelProvider.get(RoomAliasBottomSheetSharedActionViewModel::class.java)

        controller.callback = this
        setupToolbar(views.roomSettingsToolbar)
        views.roomSettingsRecyclerView.configureWith(controller, hasFixedSize = true)
        views.waitingView.waitingStatusText.setText(R.string.please_wait)
        views.waitingView.waitingStatusText.isVisible = true

        viewModel.observeViewEvents {
            when (it) {
                is RoomAliasViewEvents.Failure -> showFailure(it.throwable)
                RoomAliasViewEvents.Success    -> showSuccess()
            }.exhaustive
        }

        sharedActionViewModel
                .observe()
                .subscribe { handleAliasAction(it) }
                .disposeOnDestroyView()
    }

    private fun handleAliasAction(action: RoomAliasBottomSheetSharedAction?) {
        when (action) {
            is RoomAliasBottomSheetSharedAction.ShareAlias     -> shareAlias(action.matrixTo)
            is RoomAliasBottomSheetSharedAction.PublishAlias   -> viewModel.handle(RoomAliasAction.PublishAlias(action.alias))
            is RoomAliasBottomSheetSharedAction.UnPublishAlias -> unpublishAlias(action.alias)
            is RoomAliasBottomSheetSharedAction.DeleteAlias    -> removeLocalAlias(action.alias)
            is RoomAliasBottomSheetSharedAction.SetMainAlias   -> viewModel.handle(RoomAliasAction.SetCanonicalAlias(action.alias))
            RoomAliasBottomSheetSharedAction.UnsetMainAlias    -> viewModel.handle(RoomAliasAction.SetCanonicalAlias(canonicalAlias = null))
            null                                               -> Unit
        }
    }

    private fun shareAlias(matrixTo: String) {
        shareText(requireContext(), matrixTo)
    }

    override fun showFailure(throwable: Throwable) {
        if (throwable !is RoomAliasError) {
            super.showFailure(throwable)
        }
    }

    private fun showSuccess() {
        activity?.toast(R.string.room_settings_save_success)
    }

    override fun onDestroyView() {
        controller.callback = null
        views.roomSettingsRecyclerView.cleanup()
        super.onDestroyView()
    }

    override fun invalidate() = withState(viewModel) { state ->
        views.waitingView.root.isVisible = state.isLoading
        controller.setData(state)
        renderRoomSummary(state)
    }

    private fun renderRoomSummary(state: RoomAliasViewState) {
        state.roomSummary()?.let {
            views.roomSettingsToolbarTitleView.text = it.displayName
            avatarRenderer.render(it.toMatrixItem(), views.roomSettingsToolbarAvatarImageView)
            views.roomSettingsDecorationToolbarAvatarImageView.render(it.roomEncryptionTrustLevel)
        }
    }

    private fun unpublishAlias(alias: String) {
        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_Vector_MaterialAlertDialog_Destructive)
                .setTitle(R.string.dialog_title_confirmation)
                .setMessage(getString(R.string.room_alias_unpublish_confirmation, alias))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.action_unpublish) { _, _ ->
                    viewModel.handle(RoomAliasAction.UnpublishAlias(alias))
                }
                .show()
    }

    override fun toggleManualPublishForm() {
        viewModel.handle(RoomAliasAction.ToggleManualPublishForm)
    }

    override fun setNewAlias(alias: String) {
        viewModel.handle(RoomAliasAction.SetNewAlias(alias))
    }

    override fun addAlias() {
        viewModel.handle(RoomAliasAction.ManualPublishAlias)
    }

    override fun setRoomDirectoryVisibility(roomDirectoryVisibility: RoomDirectoryVisibility) {
        viewModel.handle(RoomAliasAction.SetRoomDirectoryVisibility(roomDirectoryVisibility))
    }

    override fun toggleLocalAliasForm() {
        viewModel.handle(RoomAliasAction.ToggleAddLocalAliasForm)
    }

    override fun setNewLocalAliasLocalPart(aliasLocalPart: String) {
        viewModel.handle(RoomAliasAction.SetNewLocalAliasLocalPart(aliasLocalPart))
    }

    override fun addLocalAlias() {
        viewModel.handle(RoomAliasAction.AddLocalAlias)
    }

    override fun openAliasDetail(alias: String) = withState(viewModel) { state ->
        RoomAliasBottomSheet
                .newInstance(
                        alias = alias,
                        isPublished = alias in state.allPublishedAliases,
                        isMainAlias = alias == state.canonicalAlias,
                        isLocal = alias in state.localAliases().orEmpty(),
                        canEditCanonicalAlias = state.actionPermissions.canChangeCanonicalAlias
                )
                .show(childFragmentManager, "ROOM_ALIAS_ACTIONS")
    }

    override fun retry() {
        viewModel.handle(RoomAliasAction.Retry)
    }

    private fun removeLocalAlias(alias: String) {
        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_Vector_MaterialAlertDialog_Destructive)
                .setTitle(R.string.dialog_title_confirmation)
                .setMessage(getString(R.string.room_alias_delete_confirmation, alias))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete) { _, _ ->
                    viewModel.handle(RoomAliasAction.RemoveLocalAlias(alias))
                }
                .show()
    }
}
