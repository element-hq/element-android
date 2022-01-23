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

package im.vector.app.features.spaces.leave

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Mavericks
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.viewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.core.extensions.replaceFragment
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivitySimpleLoadingBinding
import im.vector.app.features.spaces.SpaceBottomSheetSettingsArgs
import javax.inject.Inject

@AndroidEntryPoint
class SpaceLeaveAdvancedActivity : VectorBaseActivity<ActivitySimpleLoadingBinding>() {

    override fun getBinding(): ActivitySimpleLoadingBinding = ActivitySimpleLoadingBinding.inflate(layoutInflater)

    val leaveViewModel: SpaceLeaveAdvancedViewModel by viewModel()

    @Inject lateinit var errorFormatter: ErrorFormatter

    override fun showWaitingView(text: String?) {
        hideKeyboard()
        views.waitingView.waitingStatusText.isGone = views.waitingView.waitingStatusText.text.isNullOrBlank()
        super.showWaitingView(text)
    }

    override fun hideWaitingView() {
        views.waitingView.waitingStatusText.setTextOrHide(null)
        views.waitingView.waitingHorizontalProgress.progress = 0
        views.waitingView.waitingHorizontalProgress.isVisible = false
        super.hideWaitingView()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = intent?.getParcelableExtra<SpaceBottomSheetSettingsArgs>(Mavericks.KEY_ARG)

        if (isFirstCreation()) {
            replaceFragment(
                    views.simpleFragmentContainer,
                    SpaceLeaveAdvancedFragment::class.java,
                    args
            )
        }
    }

    override fun initUiAndData() {
        super.initUiAndData()
        waitingView = views.waitingView.waitingView
        leaveViewModel.onEach { state ->
            when (state.leaveState) {
                is Loading -> {
                    showWaitingView()
                }
                is Success -> {
                    setResult(RESULT_OK)
                    finish()
                }
                is Fail    -> {
                    hideWaitingView()
                    MaterialAlertDialogBuilder(this)
                            .setTitle(R.string.dialog_title_error)
                            .setMessage(errorFormatter.toHumanReadable(state.leaveState.error))
                            .setPositiveButton(R.string.ok) { _, _ ->
                                leaveViewModel.handle(SpaceLeaveAdvanceViewAction.ClearError)
                            }
                            .show()
                }
                else       -> {
                    hideWaitingView()
                }
            }
        }
    }

    companion object {
        fun newIntent(context: Context, spaceId: String): Intent {
            return Intent(context, SpaceLeaveAdvancedActivity::class.java).apply {
                putExtra(Mavericks.KEY_ARG, SpaceBottomSheetSettingsArgs(spaceId))
            }
        }
    }
}
