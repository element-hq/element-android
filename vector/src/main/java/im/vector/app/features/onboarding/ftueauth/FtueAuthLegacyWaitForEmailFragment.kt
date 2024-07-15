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
