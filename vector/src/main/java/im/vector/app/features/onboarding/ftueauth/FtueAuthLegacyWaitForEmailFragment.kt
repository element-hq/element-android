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
import com.airbnb.mvrx.args
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.databinding.FragmentLoginWaitForEmailBinding
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.onboarding.RegisterAction
import im.vector.lib.strings.CommonStrings

/**
 * In this screen, the user is asked to check their emails.
 */
@AndroidEntryPoint
class FtueAuthLegacyWaitForEmailFragment :
        AbstractFtueAuthFragment<FragmentLoginWaitForEmailBinding>() {

    private val params: FtueAuthWaitForEmailFragmentArgument by args()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLoginWaitForEmailBinding {
        return FragmentLoginWaitForEmailBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUi()
    }

    override fun onResume() {
        super.onResume()
        viewModel.handle(OnboardingAction.PostRegisterAction(RegisterAction.CheckIfEmailHasBeenValidated(0)))
    }

    override fun onPause() {
        super.onPause()
        viewModel.handle(OnboardingAction.StopEmailValidationCheck)
    }

    private fun setupUi() {
        views.loginWaitForEmailNotice.text = getString(CommonStrings.login_wait_for_email_notice, params.email)
    }

    override fun resetViewModel() {
        viewModel.handle(OnboardingAction.ResetAuthenticationAttempt)
    }
}
