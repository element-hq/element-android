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

package im.vector.app.features.onboarding.ftueauth

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.app.BuildConfig
import im.vector.app.R
import im.vector.app.databinding.FragmentFtueAuthSplashBinding
import im.vector.app.features.VectorFeatures
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.onboarding.OnboardingFlow
import im.vector.app.features.settings.VectorPreferences
import org.matrix.android.sdk.api.failure.Failure
import java.net.UnknownHostException
import javax.inject.Inject

/**
 * In this screen, the user is viewing an introduction to what he can do with this application
 */
class FtueAuthSplashFragment @Inject constructor(
        private val vectorPreferences: VectorPreferences,
        private val vectorFeatures: VectorFeatures
) : AbstractFtueAuthFragment<FragmentFtueAuthSplashBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentFtueAuthSplashBinding {
        return FragmentFtueAuthSplashBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        val isAlreadyHaveAccountEnabled = vectorFeatures.isOnboardingAlreadyHaveAccountSplashEnabled()
        views.loginSplashSubmit.apply {
            setText(if (isAlreadyHaveAccountEnabled) R.string.login_splash_create_account else R.string.login_splash_submit)
            debouncedClicks { splashSubmit(isAlreadyHaveAccountEnabled) }
        }
        views.loginSplashAlreadyHaveAccount.apply {
            isVisible = vectorFeatures.isOnboardingAlreadyHaveAccountSplashEnabled()
            debouncedClicks { alreadyHaveAnAccount() }
        }

        if (BuildConfig.DEBUG || vectorPreferences.developerMode()) {
            views.loginSplashVersion.isVisible = true
            @SuppressLint("SetTextI18n")
            views.loginSplashVersion.text = "Version : ${BuildConfig.VERSION_NAME}\n" +
                    "Branch: ${BuildConfig.GIT_BRANCH_NAME}\n" +
                    "Build: ${BuildConfig.BUILD_NUMBER}"
            views.loginSplashVersion.debouncedClicks { navigator.openDebug(requireContext()) }
        }
    }

    private fun splashSubmit(isAlreadyHaveAccountEnabled: Boolean) {
        val getStartedFlow = if (isAlreadyHaveAccountEnabled) OnboardingFlow.SignUp else OnboardingFlow.SignInSignUp
        viewModel.handle(OnboardingAction.OnGetStarted(resetLoginConfig = false, onboardingFlow = getStartedFlow))
    }

    private fun alreadyHaveAnAccount() {
        viewModel.handle(OnboardingAction.OnIAlreadyHaveAnAccount(resetLoginConfig = false, onboardingFlow = OnboardingFlow.SignIn))
    }

    override fun resetViewModel() {
        // Nothing to do
    }

    override fun onError(throwable: Throwable) {
        if (throwable is Failure.NetworkConnection &&
                throwable.ioException is UnknownHostException) {
            // Invalid homeserver from URL config
            val url = viewModel.getInitialHomeServerUrl().orEmpty()
            MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(R.string.dialog_title_error)
                    .setMessage(getString(R.string.login_error_homeserver_from_url_not_found, url))
                    .setPositiveButton(R.string.login_error_homeserver_from_url_not_found_enter_manual) { _, _ ->
                        val flow = withState(viewModel) { it.onboardingFlow } ?: OnboardingFlow.SignInSignUp
                        viewModel.handle(OnboardingAction.OnGetStarted(resetLoginConfig = true, flow))
                    }
                    .setNegativeButton(R.string.action_cancel, null)
                    .show()
        } else {
            super.onError(throwable)
        }
    }
}
