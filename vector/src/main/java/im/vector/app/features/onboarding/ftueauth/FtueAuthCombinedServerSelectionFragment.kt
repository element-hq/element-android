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
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import im.vector.app.R
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.extensions.associateContentStateWith
import im.vector.app.core.extensions.clearErrorOnChange
import im.vector.app.core.extensions.content
import im.vector.app.core.extensions.editText
import im.vector.app.core.extensions.realignPercentagesToParent
import im.vector.app.core.extensions.setOnImeDoneListener
import im.vector.app.core.extensions.toReducedUrl
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.utils.ensureProtocol
import im.vector.app.core.utils.ensureTrailingSlash
import im.vector.app.core.utils.openUrlInExternalBrowser
import im.vector.app.databinding.FragmentFtueServerSelectionCombinedBinding
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.onboarding.OnboardingFlow
import im.vector.app.features.onboarding.OnboardingViewEvents
import im.vector.app.features.onboarding.OnboardingViewState
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.matrix.android.sdk.api.failure.isHomeserverUnavailable
import reactivecircus.flowbinding.android.view.clicks
import javax.inject.Inject

class FtueAuthCombinedServerSelectionFragment @Inject constructor(
        private val stringProvider: StringProvider,
) : AbstractFtueAuthFragment<FragmentFtueServerSelectionCombinedBinding>() {

    private lateinit var controller: Controller

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentFtueServerSelectionCombinedBinding {
        return FragmentFtueServerSelectionCombinedBinding.inflate(inflater, container, false).also {
            controller = Controller(viewLifecycleOwner, it, stringProvider, errorFormatter)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        controller.listener = object : Controller.Listener {
            override fun onNavigationClicked() {
                viewModel.handle(OnboardingAction.PostViewEvent(OnboardingViewEvents.OnBack))
            }

            override fun updateServerUrl() {
                viewModel.handle(OnboardingAction.HomeServerChange.EditHomeServer(views.chooseServerInput.content().ensureProtocol().ensureTrailingSlash()))
            }

            override fun getInTouchClicked() {
                openUrlInExternalBrowser(requireContext(), getString(R.string.ftue_ems_url))
            }
        }
    }

    override fun resetViewModel() {
        // do nothing
    }

    override fun updateWithState(state: OnboardingViewState) {
        controller.setData(state)
    }

    override fun onError(throwable: Throwable) {
        controller.setError(throwable)
    }

    class Controller(
            lifecycleOwner: LifecycleOwner,
            private val views: FragmentFtueServerSelectionCombinedBinding,
            private val stringProvider: StringProvider,
            private val errorFormatter: ErrorFormatter,
    ) {

        var listener: Listener? = null

        init {
            views.chooseServerRoot.realignPercentagesToParent()
            views.chooseServerToolbar.setNavigationOnClickListener { listener?.onNavigationClicked() }
            views.chooseServerInput.associateContentStateWith(button = views.chooseServerSubmit, enabledPredicate = { canSubmit(it) })
            views.chooseServerInput.setOnImeDoneListener {
                if (canSubmit(views.chooseServerInput.content())) {
                    listener?.updateServerUrl()
                }
            }
            views.chooseServerGetInTouch.debouncedClicks(lifecycleOwner) { listener?.getInTouchClicked() }
            views.chooseServerSubmit.debouncedClicks(lifecycleOwner) { listener?.updateServerUrl() }
            views.chooseServerInput.clearErrorOnChange(lifecycleOwner)
        }

        private fun canSubmit(url: String) = url.isNotEmpty()

        fun setData(state: OnboardingViewState) {
            views.chooseServerHeaderSubtitle.setText(
                    when (state.onboardingFlow) {
                        OnboardingFlow.SignIn -> R.string.ftue_auth_choose_server_sign_in_subtitle
                        OnboardingFlow.SignUp -> R.string.ftue_auth_choose_server_subtitle
                        else -> throw IllegalStateException("Invalid flow state")
                    }
            )

            if (views.chooseServerInput.content().isEmpty()) {
                val userUrlInput = state.selectedHomeserver.userFacingUrl?.toReducedUrlKeepingSchemaIfInsecure()
                views.chooseServerInput.editText().setText(userUrlInput)
            }
        }

        fun setError(throwable: Throwable) {
            views.chooseServerInput.error = when {
                throwable.isHomeserverUnavailable() -> stringProvider.getString(R.string.login_error_homeserver_not_found)
                else -> errorFormatter.toHumanReadable(throwable)
            }
            println(views.chooseServerInput.error)
        }

        private fun String.toReducedUrlKeepingSchemaIfInsecure() = toReducedUrl(keepSchema = this.startsWith("http://"))

        interface Listener {
            fun onNavigationClicked()
            fun updateServerUrl()
            fun getInTouchClicked()
        }
    }
}

private fun View.debouncedClicks(lifecycleOwner: LifecycleOwner, onClicked: () -> Unit) {
    clicks()
            .onEach { onClicked() }
            .launchIn(lifecycleOwner.lifecycleScope)
}
