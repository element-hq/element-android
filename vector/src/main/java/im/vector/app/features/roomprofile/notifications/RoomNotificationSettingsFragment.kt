/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentRoomSettingGenericBinding
import im.vector.app.features.analytics.plan.MobileScreen
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.settings.VectorSettingsActivity
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.session.room.notification.RoomNotificationState
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

@AndroidEntryPoint
class RoomNotificationSettingsFragment :
        VectorBaseFragment<FragmentRoomSettingGenericBinding>(),
        RoomNotificationSettingsController.Callback {

    @Inject lateinit var viewModelFactory: RoomNotificationSettingsViewModel.Factory
    @Inject lateinit var roomNotificationSettingsController: RoomNotificationSettingsController
    @Inject lateinit var avatarRenderer: AvatarRenderer

    private val viewModel: RoomNotificationSettingsViewModel by fragmentViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentRoomSettingGenericBinding {
        return FragmentRoomSettingGenericBinding.inflate(inflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        analyticsScreenName = MobileScreen.ScreenName.RoomNotifications
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar(views.roomSettingsToolbar)
                .allowBack()
        roomNotificationSettingsController.callback = this
        views.roomSettingsRecyclerView.configureWith(roomNotificationSettingsController, hasFixedSize = true)
        setupWaitingView()
        observeViewEvents()
    }

    override fun onDestroyView() {
        views.roomSettingsRecyclerView.cleanup()
        roomNotificationSettingsController.callback = null
        super.onDestroyView()
    }

    private fun setupWaitingView() {
        views.waitingView.waitingStatusText.setText(CommonStrings.please_wait)
        views.waitingView.waitingStatusText.isVisible = true
    }

    private fun observeViewEvents() {
        viewModel.observeViewEvents {
            when (it) {
                is RoomNotificationSettingsViewEvents.Failure -> displayErrorDialog(it.throwable)
            }
        }
    }

    override fun invalidate() = withState(viewModel) { viewState ->
        roomNotificationSettingsController.setData(viewState)
        views.waitingView.root.isVisible = viewState.isLoading
        renderRoomSummary(viewState)
    }

    override fun didSelectRoomNotificationState(roomNotificationState: RoomNotificationState) {
        viewModel.handle(RoomNotificationSettingsAction.SelectNotificationState(roomNotificationState))
    }

    override fun didSelectAccountSettingsLink() {
        navigator.openSettings(requireContext(), VectorSettingsActivity.EXTRA_DIRECT_ACCESS_NOTIFICATIONS)
    }

    private fun renderRoomSummary(state: RoomNotificationSettingsViewState) {
        state.roomSummary()?.let {
            views.roomSettingsToolbarTitleView.text = it.displayName
            avatarRenderer.render(it.toMatrixItem(), views.roomSettingsToolbarAvatarImageView)
            views.roomSettingsDecorationToolbarAvatarImageView.render(it.roomEncryptionTrustLevel)
        }
    }
}
