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
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.airbnb.mvrx.args
import im.vector.app.R
import im.vector.app.core.extensions.associateContentStateWith
import im.vector.app.core.extensions.content
import im.vector.app.core.extensions.editText
import im.vector.app.core.extensions.setOnImeDoneListener
import im.vector.app.databinding.FragmentFtuePhoneConfirmationBinding
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.onboarding.RegisterAction
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.failure.Failure
import reactivecircus.flowbinding.android.widget.textChanges
import javax.inject.Inject

@Parcelize
data class FtueAuthPhoneConfirmationFragmentArgument(
        val msisdn: String
) : Parcelable

class FtueAuthPhoneConfirmationFragment @Inject constructor() : AbstractFtueAuthFragment<FragmentFtuePhoneConfirmationBinding>() {

    private val params: FtueAuthPhoneConfirmationFragmentArgument by args()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentFtuePhoneConfirmationBinding {
        return FragmentFtuePhoneConfirmationBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        views.phoneConfirmationHeaderSubtitle.text = getString(R.string.ftue_auth_phone_confirmation_subtitle, params.msisdn)
        views.phoneConfirmationInput.associateContentStateWith(button = views.phoneConfirmationSubmit)
        views.phoneConfirmationInput.setOnImeDoneListener { submitConfirmationCode() }
        views.phoneConfirmationSubmit.debouncedClicks { submitConfirmationCode() }

        views.phoneConfirmationInput.editText().textChanges()
                .onEach {
                    views.phoneConfirmationInput.error = null
                    views.phoneConfirmationSubmit.isEnabled = it.isNotBlank()
                }
                .launchIn(viewLifecycleOwner.lifecycleScope)

        views.phoneConfirmationResend.debouncedClicks { viewModel.handle(OnboardingAction.PostRegisterAction(RegisterAction.SendAgainThreePid)) }
    }

    private fun submitConfirmationCode() {
        val code = views.phoneConfirmationInput.content()
        viewModel.handle(OnboardingAction.PostRegisterAction(RegisterAction.ValidateThreePid(code)))
    }

    override fun onError(throwable: Throwable) {
        when (throwable) {
            // The entered code is not correct
            is Failure.SuccessError -> views.phoneConfirmationInput.error = getString(R.string.login_validation_code_is_not_correct)
            else                    -> views.phoneConfirmationInput.error = errorFormatter.toHumanReadable(throwable)
        }
    }

    override fun resetViewModel() {
        viewModel.handle(OnboardingAction.ResetAuthenticationAttempt)
    }
}
