/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.core.extensions.replaceFragment
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivitySimpleLoadingBinding
import im.vector.app.features.spaces.SpaceBottomSheetSettingsArgs
import im.vector.lib.core.utils.compat.getParcelableExtraCompat
import im.vector.lib.strings.CommonStrings

@AndroidEntryPoint
class SpaceLeaveAdvancedActivity : VectorBaseActivity<ActivitySimpleLoadingBinding>() {

    override fun getBinding(): ActivitySimpleLoadingBinding = ActivitySimpleLoadingBinding.inflate(layoutInflater)

    private val leaveViewModel: SpaceLeaveAdvancedViewModel by viewModel()

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

        val args = intent?.getParcelableExtraCompat<SpaceBottomSheetSettingsArgs>(Mavericks.KEY_ARG)

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
                is Fail -> {
                    hideWaitingView()
                    MaterialAlertDialogBuilder(this)
                            .setTitle(CommonStrings.dialog_title_error)
                            .setMessage(errorFormatter.toHumanReadable(state.leaveState.error))
                            .setPositiveButton(CommonStrings.ok) { _, _ ->
                                leaveViewModel.handle(SpaceLeaveAdvanceViewAction.ClearError)
                            }
                            .show()
                }
                else -> {
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
