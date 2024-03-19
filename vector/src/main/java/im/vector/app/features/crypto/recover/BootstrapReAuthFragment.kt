/*
 * Copyright (c) 2020 New Vector Ltd
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
