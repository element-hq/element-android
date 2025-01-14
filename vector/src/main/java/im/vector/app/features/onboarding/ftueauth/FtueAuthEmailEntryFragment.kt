/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.onboarding.ftueauth

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.mvrx.args
import dagger.hilt.android.AndroidEntryPoint
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
import im.vector.lib.strings.CommonStrings
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
        views.emailEntryInput.hint = getString(if (params.mandatory) CommonStrings.ftue_auth_email_entry_title else CommonStrings.login_set_email_optional_hint)
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
        views.emailEntryHeaderSubtitle.text = getString(CommonStrings.ftue_auth_email_subtitle, state.selectedHomeserver.userFacingUrl.toReducedUrl())

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
