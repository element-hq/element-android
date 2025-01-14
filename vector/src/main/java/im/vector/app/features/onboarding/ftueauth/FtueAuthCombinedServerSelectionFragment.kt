/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.onboarding.ftueauth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.config.Config
import im.vector.app.config.SunsetConfig
import im.vector.app.core.extensions.associateContentStateWith
import im.vector.app.core.extensions.clearErrorOnChange
import im.vector.app.core.extensions.content
import im.vector.app.core.extensions.editText
import im.vector.app.core.extensions.realignPercentagesToParent
import im.vector.app.core.extensions.setOnImeDoneListener
import im.vector.app.core.extensions.showKeyboard
import im.vector.app.core.extensions.toReducedUrl
import im.vector.app.core.resources.BuildMeta
import im.vector.app.core.utils.ensureProtocol
import im.vector.app.core.utils.ensureTrailingSlash
import im.vector.app.core.utils.openApplicationStore
import im.vector.app.core.utils.openUrlInChromeCustomTab
import im.vector.app.core.utils.openUrlInExternalBrowser
import im.vector.app.databinding.FragmentFtueServerSelectionCombinedBinding
import im.vector.app.features.onboarding.MasSupportRequiredException
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.onboarding.OnboardingFlow
import im.vector.app.features.onboarding.OnboardingViewEvents
import im.vector.app.features.onboarding.OnboardingViewState
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.failure.isHomeserverUnavailable
import javax.inject.Inject

@AndroidEntryPoint
class FtueAuthCombinedServerSelectionFragment :
        AbstractFtueAuthFragment<FragmentFtueServerSelectionCombinedBinding>() {
    @Inject lateinit var buildMeta: BuildMeta

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
        (Config.sunsetConfig as? SunsetConfig.Enabled)?.let { config ->
            views.chooseServerCardDownloadReplacementApp.debouncedClicks {
                openApplicationStore(
                        activity = requireActivity(),
                        buildMeta = buildMeta,
                        appId = config.replacementApplicationId,
                )
            }
            views.chooseServerCardDownloadReplacementApp.findViewById<View>(R.id.view_download_replacement_app_learn_more)?.debouncedClicks {
                openUrlInChromeCustomTab(
                        context = requireContext(),
                        session = null,
                        url = config.learnMoreLink,
                )
            }
        }
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
        val isMasSupportRequiredException = throwable is MasSupportRequiredException
        views.chooseServerInput.error = when {
            throwable.isHomeserverUnavailable() -> getString(CommonStrings.login_error_homeserver_not_found)
            isMasSupportRequiredException -> " "
            else -> errorFormatter.toHumanReadable(throwable)
        }
        views.chooseServerCardErrorMas.isVisible = isMasSupportRequiredException
        views.chooseServerCardDownloadReplacementApp.isVisible = isMasSupportRequiredException
        if (isMasSupportRequiredException) {
            views.chooseServerSubmit.isEnabled = false
        }
        val config = Config.sunsetConfig
        if (throwable is MasSupportRequiredException && config is SunsetConfig.Enabled) {
            views.chooseServerCardErrorMas.findViewById<TextView>(R.id.view_card_error_title).text =
                    getString(CommonStrings.error_mas_not_supported_title, views.chooseServerInput.content())
            views.chooseServerCardErrorMas.findViewById<TextView>(R.id.view_card_error_subtitle).text =
                    getString(
                            CommonStrings.error_mas_not_supported_subtitle,
                            config.replacementApplicationName,
                            views.chooseServerInput.content(),
                    )
            views.chooseServerCardDownloadReplacementApp.findViewById<TextView>(R.id.view_download_replacement_app_title).text =
                    getString(CommonStrings.view_download_replacement_app_title, config.replacementApplicationName)
        }
    }

    private fun String.toReducedUrlKeepingSchemaIfInsecure() = toReducedUrl(keepSchema = this.startsWith("http://"))
}
