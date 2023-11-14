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
import com.airbnb.mvrx.args
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.associateContentStateWith
import im.vector.app.core.extensions.autofillEmail
import im.vector.app.core.extensions.clearErrorOnChange
import im.vector.app.core.extensions.content
import im.vector.app.core.extensions.editText
import im.vector.app.core.extensions.hasContent
import im.vector.app.core.extensions.setOnImeDoneListener
import im.vector.app.core.extensions.toReducedUrl
import im.vector.app.databinding.FragmentFtueEmailInputBinding
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.onboarding.OnboardingViewState
import im.vector.app.features.onboarding.RegisterAction
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.auth.registration.RegisterThreePid
import org.matrix.android.sdk.api.extensions.isEmail

@Parcelize
data class FtueAuthEmailEntryFragmentArgument(
        val mandatory: Boolean,
) : Parcelable

@AndroidEntryPoint
class FtueAuthEmailEntryFragment : AbstractFtueAuthFragment<FragmentFtueEmailInputBinding>() {

    private val params: FtueAuthEmailEntryFragmentArgument by args()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentFtueEmailInputBinding {
        return FragmentFtueEmailInputBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        views.emailEntryInput.hint = getString(if (params.mandatory) R.string.ftue_auth_email_entry_title else R.string.login_set_email_optional_hint)
        views.emailEntryInput.associateContentStateWith(
                button = views.emailEntrySubmit,
                enabledPredicate = { it.isEmail() || it.isEmptyAndOptional() },
        )
        views.emailEntryInput.setOnImeDoneListener { updateEmail() }
        views.emailEntryInput.clearErrorOnChange(viewLifecycleOwner)
        views.emailEntrySubmit.debouncedClicks { updateEmail() }
        views.emailEntryInput.autofillEmail()
    }

    private fun updateEmail() {
        val email = views.emailEntryInput.content()
        when {
            email.isEmptyAndOptional() -> viewModel.handle(OnboardingAction.PostRegisterAction(RegisterAction.RegisterDummy))
            else -> viewModel.handle(OnboardingAction.PostRegisterAction(RegisterAction.AddThreePid(RegisterThreePid.Email(email))))
        }
    }

    private fun String.isEmptyAndOptional() = isEmpty() && !params.mandatory

    override fun updateWithState(state: OnboardingViewState) {
        views.emailEntryHeaderSubtitle.text = getString(R.string.ftue_auth_email_subtitle, state.selectedHomeserver.userFacingUrl.toReducedUrl())

        if (!views.emailEntryInput.hasContent()) {
            views.emailEntryInput.editText().setText(state.registrationState.email)
        }
    }

    override fun onError(throwable: Throwable) {
        views.emailEntryInput.error = errorFormatter.toHumanReadable(throwable)
    }

    override fun resetViewModel() {
        viewModel.handle(OnboardingAction.ResetAuthenticationAttempt)
    }
}
