/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.permissions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.core.utils.toast
import im.vector.app.databinding.FragmentRoomSettingGenericBinding
import im.vector.app.features.analytics.plan.MobileScreen
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.roommemberprofile.powerlevel.EditPowerLevelDialogs
import im.vector.app.features.roomprofile.RoomProfileArgs
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.session.room.powerlevels.Role
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

@AndroidEntryPoint
class RoomPermissionsFragment :
        VectorBaseFragment<FragmentRoomSettingGenericBinding>(),
        RoomPermissionsController.Callback {

    @Inject lateinit var controller: RoomPermissionsController
    @Inject lateinit var avatarRenderer: AvatarRenderer

    private val viewModel: RoomPermissionsViewModel by fragmentViewModel()

    private val roomProfileArgs: RoomProfileArgs by args()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentRoomSettingGenericBinding {
        return FragmentRoomSettingGenericBinding.inflate(inflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        analyticsScreenName = MobileScreen.ScreenName.RoomPermissions
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        controller.callback = this
        setupToolbar(views.roomSettingsToolbar)
                .allowBack()
        views.roomSettingsRecyclerView.configureWith(controller, hasFixedSize = true)
        views.waitingView.waitingStatusText.setText(CommonStrings.please_wait)
        views.waitingView.waitingStatusText.isVisible = true

        viewModel.observeViewEvents {
            when (it) {
                is RoomPermissionsViewEvents.Failure -> showFailure(it.throwable)
                RoomPermissionsViewEvents.Success -> showSuccess()
            }
        }
    }

    private fun showSuccess() {
        activity?.toast(CommonStrings.room_settings_save_success)
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

    private fun renderRoomSummary(state: RoomPermissionsViewState) {
        state.roomSummary()?.let {
            views.roomSettingsToolbarTitleView.text = it.displayName
            avatarRenderer.render(it.toMatrixItem(), views.roomSettingsToolbarAvatarImageView)
            views.roomSettingsDecorationToolbarAvatarImageView.render(it.roomEncryptionTrustLevel)
        }
    }

    override fun onEditPermission(editablePermission: EditablePermission, currentRole: Role) {
        EditPowerLevelDialogs.showChoice(requireActivity(), editablePermission.labelResId, currentRole) { newPowerLevel ->
            viewModel.handle(RoomPermissionsAction.UpdatePermission(editablePermission, newPowerLevel))
        }
    }

    override fun toggleShowAllPermissions() {
        viewModel.handle(RoomPermissionsAction.ToggleShowAllPermissions)
    }
}
