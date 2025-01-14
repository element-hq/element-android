/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.login

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.resources.BuildMeta
import im.vector.app.databinding.FragmentLoginSplashBinding
import im.vector.app.features.analytics.plan.MobileScreen
import im.vector.app.features.settings.VectorPreferences
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.failure.Failure
import java.net.UnknownHostException
import javax.inject.Inject

/**
 * In this screen, the user is viewing an introduction to what he can do with this application.
 */
@AndroidEntryPoint
class LoginSplashFragment :
        AbstractLoginFragment<FragmentLoginSplashBinding>() {

    @Inject lateinit var vectorPreferences: VectorPreferences
    @Inject lateinit var buildMeta: BuildMeta

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLoginSplashBinding {
        return FragmentLoginSplashBinding.inflate(inflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        analyticsScreenName = MobileScreen.ScreenName.Welcome
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
    }

    private fun setupViews() {
        views.loginSplashSubmit.debouncedClicks { getStarted() }

        if (buildMeta.isDebug || vectorPreferences.developerMode()) {
            views.loginSplashVersion.isVisible = true
            @SuppressLint("SetTextI18n")
            views.loginSplashVersion.text = "Version : ${buildMeta.versionName}\n" +
                    "Branch: ${buildMeta.gitBranchName} ${buildMeta.gitRevision}"
            views.loginSplashVersion.debouncedClicks { navigator.openDebug(requireContext()) }
        }
    }

    private fun getStarted() {
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
                    .setTitle(CommonStrings.dialog_title_error)
                    .setMessage(getString(CommonStrings.login_error_homeserver_from_url_not_found, url))
                    .setPositiveButton(CommonStrings.login_error_homeserver_from_url_not_found_enter_manual) { _, _ ->
                        loginViewModel.handle(LoginAction.OnGetStarted(resetLoginConfig = true))
                    }
                    .setNegativeButton(CommonStrings.action_cancel, null)
                    .show()
        } else {
            super.onError(throwable)
        }
    }
}
