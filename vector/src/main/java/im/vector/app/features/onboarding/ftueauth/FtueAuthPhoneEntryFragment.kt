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
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.associateContentStateWith
import im.vector.app.core.extensions.autofillPhoneNumber
import im.vector.app.core.extensions.content
import im.vector.app.core.extensions.editText
import im.vector.app.core.extensions.setOnImeDoneListener
import im.vector.app.core.extensions.toReducedUrl
import im.vector.app.databinding.FragmentFtuePhoneInputBinding
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.onboarding.OnboardingViewState
import im.vector.app.features.onboarding.RegisterAction
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.matrix.android.sdk.api.auth.registration.RegisterThreePid
import reactivecircus.flowbinding.android.widget.textChanges
import javax.inject.Inject

@AndroidEntryPoint
class FtueAuthPhoneEntryFragment :
        AbstractFtueAuthFragment<FragmentFtuePhoneInputBinding>() {

    @Inject lateinit var phoneNumberParser: PhoneNumberParser

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentFtuePhoneInputBinding {
        return FragmentFtuePhoneInputBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        views.phoneEntryInput.associateContentStateWith(button = views.phoneEntrySubmit)
        views.phoneEntryInput.setOnImeDoneListener { updatePhoneNumber() }
        views.phoneEntrySubmit.debouncedClicks { updatePhoneNumber() }

        views.phoneEntryInput.editText().textChanges()
                .onEach {
                    views.phoneEntryInput.error = null
                    views.phoneEntrySubmit.isEnabled = it.isNotBlank()
                }
                .launchIn(viewLifecycleOwner.lifecycleScope)

        views.phoneEntryInput.autofillPhoneNumber()
    }

    private fun updatePhoneNumber() {
        val number = views.phoneEntryInput.content()

        when (val result = phoneNumberParser.parseInternationalNumber(number)) {
            PhoneNumberParser.Result.ErrorInvalidNumber -> views.phoneEntryInput.error = getString(R.string.login_msisdn_error_other)
            PhoneNumberParser.Result.ErrorMissingInternationalCode -> views.phoneEntryInput.error = getString(R.string.login_msisdn_error_not_international)
            is PhoneNumberParser.Result.Success -> {
                val (countryCode, phoneNumber) = result
                viewModel.handle(OnboardingAction.PostRegisterAction(RegisterAction.AddThreePid(RegisterThreePid.Msisdn(phoneNumber, countryCode))))
            }
        }
    }

    override fun updateWithState(state: OnboardingViewState) {
        views.phoneEntryHeaderSubtitle.text = getString(R.string.ftue_auth_phone_subtitle, state.selectedHomeserver.userFacingUrl.toReducedUrl())
    }

    override fun onError(throwable: Throwable) {
        views.phoneEntryInput.error = errorFormatter.toHumanReadable(throwable)
    }

    override fun resetViewModel() {
        viewModel.handle(OnboardingAction.ResetAuthenticationAttempt)
    }
}
