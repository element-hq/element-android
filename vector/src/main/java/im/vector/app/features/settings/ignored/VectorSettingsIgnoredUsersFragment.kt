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
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import im.vector.app.R
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.core.platform.VectorBaseFragment
import kotlinx.android.synthetic.main.fragment_generic_recycler.*
import kotlinx.android.synthetic.main.merge_overlay_waiting_view.*
import javax.inject.Inject

class VectorSettingsIgnoredUsersFragment @Inject constructor(
        val ignoredUsersViewModelFactory: IgnoredUsersViewModel.Factory,
        private val ignoredUsersController: IgnoredUsersController
) : VectorBaseFragment(), IgnoredUsersController.Callback {

    override fun getLayoutResId() = R.layout.fragment_generic_recycler

    private val viewModel: IgnoredUsersViewModel by fragmentViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        waiting_view_status_text.setText(R.string.please_wait)
        waiting_view_status_text.isVisible = true
        ignoredUsersController.callback = this
        recyclerView.configureWith(ignoredUsersController)
        viewModel.observeViewEvents {
            when (it) {
                is IgnoredUsersViewEvents.Loading -> showLoading(it.message)
                is IgnoredUsersViewEvents.Failure -> showFailure(it.throwable)
            }.exhaustive
        }
    }

    override fun onDestroyView() {
        ignoredUsersController.callback = null
        recyclerView.cleanup()
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()

        (activity as? VectorBaseActivity)?.supportActionBar?.setTitle(R.string.settings_ignored_users)
    }

    override fun onUserIdClicked(userId: String) {
        AlertDialog.Builder(requireActivity())
                .setMessage(getString(R.string.settings_unignore_user, userId))
                .setPositiveButton(R.string.yes) { _, _ ->
                    viewModel.handle(IgnoredUsersAction.UnIgnore(userId))
                }
                .setNegativeButton(R.string.no, null)
                .show()
    }

    // ==============================================================================================================
    // ignored users list management
    // ==============================================================================================================

    override fun invalidate() = withState(viewModel) { state ->
        ignoredUsersController.update(state)

        handleUnIgnoreRequestStatus(state.unIgnoreRequest)
    }

    private fun handleUnIgnoreRequestStatus(unIgnoreRequest: Async<Unit>) {
        when (unIgnoreRequest) {
            is Loading -> waiting_view.isVisible = true
            else       -> waiting_view.isVisible = false
        }
    }
}
