/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.onboarding.ftueauth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import im.vector.app.R
import im.vector.app.core.extensions.content
import im.vector.app.core.extensions.editText
import im.vector.app.core.extensions.realignPercentagesToParent
import im.vector.app.core.extensions.toReducedUrl
import im.vector.app.core.utils.ensureProtocol
import im.vector.app.core.utils.ensureTrailingSlash
import im.vector.app.core.utils.openUrlInExternalBrowser
import im.vector.app.databinding.FragmentFtueServerSelectionCombinedBinding
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.onboarding.OnboardingViewEvents
import im.vector.app.features.onboarding.OnboardingViewState
import javax.inject.Inject

class FtueAuthCombinedServerSelectionFragment @Inject constructor() : AbstractFtueAuthFragment<FragmentFtueServerSelectionCombinedBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentFtueServerSelectionCombinedBinding {
        return FragmentFtueServerSelectionCombinedBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        views.chooseServerRoot.realignPercentagesToParent()
        views.chooseServerToolbar.setNavigationOnClickListener {
            viewModel.handle(OnboardingAction.PostViewEvent(OnboardingViewEvents.OnBack))
        }
        views.chooseServerInput.editText?.setOnEditorActionListener { _, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    updateServerUrl()
                }
            }
            false
        }
        views.chooseServerGetInTouch.debouncedClicks { openUrlInExternalBrowser(requireContext(), getString(R.string.ftue_ems_url)) }
        views.chooseServerSubmit.debouncedClicks { updateServerUrl() }
    }

    private fun updateServerUrl() {
        viewModel.handle(OnboardingAction.HomeServerChange.EditHomeServer(views.chooseServerInput.content().ensureProtocol().ensureTrailingSlash()))
    }

    override fun resetViewModel() {
        // do nothing
    }

    override fun updateWithState(state: OnboardingViewState) {
        if (views.chooseServerInput.content().isEmpty()) {
            val userUrlInput = state.selectedHomeserver.userFacingUrl?.toReducedUrlKeepingSchemaIfInsecure()
            views.chooseServerInput.editText().setText(userUrlInput)
        }
    }

    private fun String.toReducedUrlKeepingSchemaIfInsecure() = toReducedUrl(keepSchema = this.startsWith("http://"))
}
