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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import im.vector.app.databinding.FragmentLoginResetPasswordSuccessBinding
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.onboarding.OnboardingViewEvents
import javax.inject.Inject

/**
 * In this screen, we confirm to the user that his password has been reset
 */
class FtueAuthResetPasswordSuccessFragment @Inject constructor() : AbstractFtueAuthFragment<FragmentLoginResetPasswordSuccessBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLoginResetPasswordSuccessBinding {
        return FragmentLoginResetPasswordSuccessBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        views.resetPasswordSuccessSubmit.setOnClickListener { submit() }
    }

    private fun submit() {
        viewModel.handle(OnboardingAction.PostViewEvent(OnboardingViewEvents.OnResetPasswordMailConfirmationSuccessDone))
    }

    override fun resetViewModel() {
        viewModel.handle(OnboardingAction.ResetResetPassword)
    }
}
