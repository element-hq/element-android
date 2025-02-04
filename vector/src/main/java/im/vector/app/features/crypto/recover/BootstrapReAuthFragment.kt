/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.crypto.recover

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.airbnb.mvrx.parentFragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentBootstrapReauthBinding

@AndroidEntryPoint
class BootstrapReAuthFragment :
        VectorBaseFragment<FragmentBootstrapReauthBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentBootstrapReauthBinding {
        return FragmentBootstrapReauthBinding.inflate(inflater, container, false)
    }

    val sharedViewModel: BootstrapSharedViewModel by parentFragmentViewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        views.bootstrapRetryButton.debouncedClicks { submit() }
        views.bootstrapCancelButton.debouncedClicks { cancel() }

        val viewModel = ViewModelProvider(this).get(BootstrapReAuthViewModel::class.java)
        if (!viewModel.isFirstSubmitDone) {
            viewModel.isFirstSubmitDone = true
            submit()
        }
    }

    private fun submit() = withState(sharedViewModel) { state ->
        if (state.step !is BootstrapStep.AccountReAuth) {
            return@withState
        }
        if (state.passphrase != null) {
            sharedViewModel.handle(BootstrapActions.DoInitialize(state.passphrase))
        } else {
            sharedViewModel.handle(BootstrapActions.DoInitializeGeneratedKey)
        }
    }

    private fun cancel() = withState(sharedViewModel) { state ->
        if (state.step !is BootstrapStep.AccountReAuth) {
            return@withState
        }
        sharedViewModel.handle(BootstrapActions.GoBack)
    }

    override fun invalidate() = withState(sharedViewModel) { state ->
        if (state.step !is BootstrapStep.AccountReAuth) {
            return@withState
        }
        val failure = state.step.failure
        views.reAuthFailureText.setTextOrHide(failure)
        if (failure == null) {
            views.waitingProgress.isVisible = true
            views.bootstrapCancelButton.isVisible = false
            views.bootstrapRetryButton.isVisible = false
        } else {
            views.waitingProgress.isVisible = false
            views.bootstrapCancelButton.isVisible = true
            views.bootstrapRetryButton.isVisible = true
        }
        views.bootstrapDescriptionText.giveAccessibilityFocusOnce()
    }
}
