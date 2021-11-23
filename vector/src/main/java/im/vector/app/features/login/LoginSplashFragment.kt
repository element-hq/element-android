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

package im.vector.app.features.login

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.airbnb.mvrx.fragmentViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.app.BuildConfig
import im.vector.app.R
import im.vector.app.databinding.FragmentLoginSplashBinding
import im.vector.app.features.analytics.ui.consent.AnalyticsConsentViewActions
import im.vector.app.features.analytics.ui.consent.AnalyticsConsentViewModel
import im.vector.app.features.analytics.ui.consent.AnalyticsConsentViewState
import im.vector.app.features.settings.VectorPreferences
import org.matrix.android.sdk.api.failure.Failure
import java.net.UnknownHostException
import javax.inject.Inject

/**
 * In this screen, the user is viewing an introduction to what he can do with this application
 */
class LoginSplashFragment @Inject constructor(
        private val vectorPreferences: VectorPreferences
) : AbstractLoginFragment<FragmentLoginSplashBinding>() {

    private val analyticsConsentViewModel: AnalyticsConsentViewModel by fragmentViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLoginSplashBinding {
        return FragmentLoginSplashBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
        observeAnalyticsState()
    }

    private fun observeAnalyticsState() {
        analyticsConsentViewModel.onEach(AnalyticsConsentViewState::shouldCheckTheBox) {
            views.loginSplashAnalyticsConsent.isChecked = it
        }
    }

    private fun setupViews() {
        views.loginSplashSubmit.debouncedClicks { getStarted() }
        // setOnCheckedChangeListener is to annoying since it does not distinguish user changes and code changes
        views.loginSplashAnalyticsConsent.setOnClickListener {
            analyticsConsentViewModel.handle(AnalyticsConsentViewActions.SetUserConsent(
                    views.loginSplashAnalyticsConsent.isChecked
            ))
        }

        if (BuildConfig.DEBUG || vectorPreferences.developerMode()) {
            views.loginSplashVersion.isVisible = true
            @SuppressLint("SetTextI18n")
            views.loginSplashVersion.text = "Version : ${BuildConfig.VERSION_NAME}\n" +
                    "Branch: ${BuildConfig.GIT_BRANCH_NAME}\n" +
                    "Build: ${BuildConfig.BUILD_NUMBER}"
        }
    }

    private fun getStarted() {
        analyticsConsentViewModel.handle(AnalyticsConsentViewActions.OnGetStarted)
        loginViewModel.handle(LoginAction.OnGetStarted(resetLoginConfig = false))
    }

    override fun resetViewModel() {
        // Nothing to do
    }

    override fun onError(throwable: Throwable) {
        if (throwable is Failure.NetworkConnection &&
                throwable.ioException is UnknownHostException) {
            // Invalid homeserver from URL config
            val url = loginViewModel.getInitialHomeServerUrl().orEmpty()
            MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(R.string.dialog_title_error)
                    .setMessage(getString(R.string.login_error_homeserver_from_url_not_found, url))
                    .setPositiveButton(R.string.login_error_homeserver_from_url_not_found_enter_manual) { _, _ ->
                        loginViewModel.handle(LoginAction.OnGetStarted(resetLoginConfig = true))
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
        } else {
            super.onError(throwable)
        }
    }
}
