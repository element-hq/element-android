/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.ignored

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentGenericRecyclerBinding
import im.vector.app.features.analytics.plan.MobileScreen
import im.vector.lib.strings.CommonStrings
import javax.inject.Inject

@AndroidEntryPoint
class VectorSettingsIgnoredUsersFragment :
        VectorBaseFragment<FragmentGenericRecyclerBinding>(),
        IgnoredUsersController.Callback {

    @Inject lateinit var ignoredUsersController: IgnoredUsersController

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentGenericRecyclerBinding {
        return FragmentGenericRecyclerBinding.inflate(inflater, container, false)
    }

    private val viewModel: IgnoredUsersViewModel by fragmentViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        analyticsScreenName = MobileScreen.ScreenName.SettingsIgnoredUsers
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        views.waitingView.waitingStatusText.setText(CommonStrings.please_wait)
        views.waitingView.waitingStatusText.isVisible = true
        ignoredUsersController.callback = this
        views.genericRecyclerView.configureWith(ignoredUsersController)
        viewModel.observeViewEvents {
            when (it) {
                is IgnoredUsersViewEvents.Loading -> showLoading(it.message)
                is IgnoredUsersViewEvents.Failure -> showFailure(it.throwable)
                IgnoredUsersViewEvents.Success -> Unit
            }
        }
    }

    override fun onDestroyView() {
        ignoredUsersController.callback = null
        views.genericRecyclerView.cleanup()
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()

        (activity as? AppCompatActivity)?.supportActionBar?.setTitle(CommonStrings.settings_ignored_users)
    }

    override fun onUserIdClicked(userId: String) {
        MaterialAlertDialogBuilder(requireActivity())
                .setTitle(CommonStrings.room_participants_action_unignore_title)
                .setMessage(getString(CommonStrings.settings_unignore_user, userId))
                .setPositiveButton(CommonStrings.unignore) { _, _ ->
                    viewModel.handle(IgnoredUsersAction.UnIgnore(userId))
                }
                .setNegativeButton(CommonStrings.action_cancel, null)
                .show()
    }

    // ==============================================================================================================
    // ignored users list management
    // ==============================================================================================================

    override fun invalidate() = withState(viewModel) { state ->
        ignoredUsersController.update(state)
        views.waitingView.root.isVisible = state.isLoading
    }
}
