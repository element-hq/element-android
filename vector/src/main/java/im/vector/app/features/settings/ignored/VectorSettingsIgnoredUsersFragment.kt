/*
 * Copyright 2019 New Vector Ltd
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
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentGenericRecyclerBinding
import im.vector.app.features.MainActivity
import im.vector.app.features.MainActivityArgs
import im.vector.app.features.analytics.plan.MobileScreen
import javax.inject.Inject

class VectorSettingsIgnoredUsersFragment @Inject constructor(
        private val ignoredUsersController: IgnoredUsersController
) : VectorBaseFragment<FragmentGenericRecyclerBinding>(),
        IgnoredUsersController.Callback {

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

        views.waitingView.waitingStatusText.setText(R.string.please_wait)
        views.waitingView.waitingStatusText.isVisible = true
        ignoredUsersController.callback = this
        views.genericRecyclerView.configureWith(ignoredUsersController)
        viewModel.observeViewEvents {
            when (it) {
                is IgnoredUsersViewEvents.Loading -> showLoading(it.message)
                is IgnoredUsersViewEvents.Failure -> showFailure(it.throwable)
                IgnoredUsersViewEvents.Success    -> handleSuccess()
            }
        }
    }

    private fun handleSuccess() {
        // A user has been un-ignored, perform a initial sync
        MainActivity.restartApp(requireActivity(), MainActivityArgs(clearCache = true))
    }

    override fun onDestroyView() {
        ignoredUsersController.callback = null
        views.genericRecyclerView.cleanup()
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()

        (activity as? AppCompatActivity)?.supportActionBar?.setTitle(R.string.settings_ignored_users)
    }

    override fun onUserIdClicked(userId: String) {
        MaterialAlertDialogBuilder(requireActivity())
                .setTitle(R.string.room_participants_action_unignore_title)
                .setMessage(getString(R.string.settings_unignore_user, userId))
                .setPositiveButton(R.string.unignore) { _, _ ->
                    viewModel.handle(IgnoredUsersAction.UnIgnore(userId))
                }
                .setNegativeButton(R.string.action_cancel, null)
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
