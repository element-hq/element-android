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
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.associateContentStateWith
import im.vector.app.core.extensions.clearErrorOnChange
import im.vector.app.core.extensions.content
import im.vector.app.core.extensions.editText
import im.vector.app.core.extensions.realignPercentagesToParent
import im.vector.app.core.extensions.setOnImeDoneListener
import im.vector.app.core.extensions.showKeyboard
import im.vector.app.core.extensions.toReducedUrl
import im.vector.app.core.utils.ensureProtocol
import im.vector.app.core.utils.ensureTrailingSlash
import im.vector.app.core.utils.openUrlInExternalBrowser
import im.vector.app.databinding.FragmentFtueServerSelectionCombinedBinding
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.onboarding.OnboardingFlow
import im.vector.app.features.onboarding.OnboardingViewEvents
import im.vector.app.features.onboarding.OnboardingViewState
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.failure.isHomeserverUnavailable

@AndroidEntryPoint
class FtueAuthCombinedServerSelectionFragment :
        AbstractFtueAuthFragment<FragmentFtueServerSelectionCombinedBinding>() {

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
        views.chooseServerInput.associateContentStateWith(button = views.chooseServerSubmit, enabledPredicate = { canSubmit(it) })
        views.chooseServerInput.setOnImeDoneListener {
            if (canSubmit(views.chooseServerInput.content())) {
                updateServerUrl()
            }
        }
        views.chooseServerGetInTouch.debouncedClicks { openUrlInExternalBrowser(requireContext(), getString(im.vector.app.config.R.string.ftue_ems_url)) }
        views.chooseServerSubmit.debouncedClicks { updateServerUrl() }
        views.chooseServerInput.clearErrorOnChange(viewLifecycleOwner)
    }

    private fun canSubmit(url: String) = url.isNotEmpty()

    private fun updateServerUrl() {
        viewModel.handle(OnboardingAction.HomeServerChange.EditHomeServer(views.chooseServerInput.content().ensureProtocol().ensureTrailingSlash()))
    }

    override fun resetViewModel() {
        // do nothing
    }

    override fun updateWithState(state: OnboardingViewState) {
        views.chooseServerHeaderSubtitle.setText(
                when (state.onboardingFlow) {
                    OnboardingFlow.SignIn -> CommonStrings.ftue_auth_choose_server_sign_in_subtitle
                    OnboardingFlow.SignUp -> CommonStrings.ftue_auth_choose_server_subtitle
                    else -> throw IllegalStateException("Invalid flow state")
                }
        )

        if (views.chooseServerInput.content().isEmpty()) {
            val userUrlInput = state.selectedHomeserver.userFacingUrl?.toReducedUrlKeepingSchemaIfInsecure() ?: viewModel.getDefaultHomeserverUrl()
            views.chooseServerInput.editText().setText(userUrlInput)
        }

        views.chooseServerInput.editText().selectAll()
        views.chooseServerInput.editText().showKeyboard(true)
    }

    override fun onError(throwable: Throwable) {
        views.chooseServerInput.error = when {
            throwable.isHomeserverUnavailable() -> getString(CommonStrings.login_error_homeserver_not_found)
            else -> errorFormatter.toHumanReadable(throwable)
        }
    }

    private fun String.toReducedUrlKeepingSchemaIfInsecure() = toReducedUrl(keepSchema = this.startsWith("http://"))
}
