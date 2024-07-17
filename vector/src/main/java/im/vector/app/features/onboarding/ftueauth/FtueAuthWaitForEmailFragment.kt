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

package im.vector.app.features.onboarding.ftueauth

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import com.airbnb.mvrx.args
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.utils.colorTerminatingFullStop
import im.vector.app.databinding.FragmentFtueWaitForEmailVerificationBinding
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.onboarding.RegisterAction
import im.vector.app.features.themes.ThemeProvider
import im.vector.app.features.themes.ThemeUtils
import im.vector.lib.strings.CommonStrings
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@Parcelize
data class FtueAuthWaitForEmailFragmentArgument(
        val email: String,
        val isRestoredSession: Boolean,
) : Parcelable

/**
 * In this screen, the user is asked to check their emails.
 */
@AndroidEntryPoint
class FtueAuthWaitForEmailFragment :
        AbstractFtueAuthFragment<FragmentFtueWaitForEmailVerificationBinding>() {

    @Inject lateinit var themeProvider: ThemeProvider

    private val params: FtueAuthWaitForEmailFragmentArgument by args()
    private var inferHasLeftAndReturnedToScreen = false

    override fun backIsHardExit() = params.isRestoredSession

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentFtueWaitForEmailVerificationBinding {
        return FragmentFtueWaitForEmailVerificationBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUi()
    }

    private fun setupUi() {
        views.emailVerificationGradientContainer.setBackgroundResource(themeProvider.ftueBreakerBackground())
        views.emailVerificationTitle.text = getString(CommonStrings.ftue_auth_email_verification_title)
                .colorTerminatingFullStop(ThemeUtils.getColor(requireContext(), com.google.android.material.R.attr.colorSecondary))
        views.emailVerificationSubtitle.text = getString(CommonStrings.ftue_auth_email_verification_subtitle, params.email)
        views.emailVerificationResendEmail.debouncedClicks {
            hideWaitingForVerificationLoading()
            viewModel.handle(OnboardingAction.PostRegisterAction(RegisterAction.SendAgainThreePid))
        }
    }

    override fun onResume() {
        super.onResume()
        showLoadingIfReturningToScreen()
        viewModel.handle(OnboardingAction.PostRegisterAction(RegisterAction.CheckIfEmailHasBeenValidated(0)))
    }

    private fun showLoadingIfReturningToScreen() {
        when (inferHasLeftAndReturnedToScreen) {
            true -> showWaitingForVerificationLoading()
            false -> {
                inferHasLeftAndReturnedToScreen = true
            }
        }
    }

    private fun hideWaitingForVerificationLoading() {
        views.emailVerificationWaiting.isInvisible = true
    }

    private fun showWaitingForVerificationLoading() {
        views.emailVerificationWaiting.isInvisible = false
    }

    override fun onPause() {
        super.onPause()
        viewModel.handle(OnboardingAction.StopEmailValidationCheck)
    }

    override fun resetViewModel() {
        when {
            backIsHardExit() -> viewModel.handle(OnboardingAction.ResetAuthenticationAttempt)
            else -> {
                // delegate to the previous step
            }
        }
    }
}
