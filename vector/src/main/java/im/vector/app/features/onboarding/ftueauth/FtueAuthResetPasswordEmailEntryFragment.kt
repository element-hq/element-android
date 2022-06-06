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
import im.vector.app.core.extensions.associateContentStateWith
import im.vector.app.core.extensions.content
import im.vector.app.core.extensions.editText
import im.vector.app.core.extensions.isEmail
import im.vector.app.core.extensions.setOnImeDoneListener
import im.vector.app.databinding.FragmentFtueResetPasswordEmailInputBinding
import im.vector.app.features.onboarding.OnboardingAction
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import reactivecircus.flowbinding.android.widget.textChanges

@AndroidEntryPoint
class FtueAuthResetPasswordEmailEntryFragment : AbstractFtueAuthFragment<FragmentFtueResetPasswordEmailInputBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentFtueResetPasswordEmailInputBinding {
        return FragmentFtueResetPasswordEmailInputBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        views.emailEntryInput.associateContentStateWith(button = views.emailEntrySubmit)
        views.emailEntryInput.setOnImeDoneListener { startPasswordReset() }
        views.emailEntrySubmit.debouncedClicks { startPasswordReset() }

        views.emailEntryInput.editText().textChanges()
                .onEach {
                    views.emailEntryInput.error = null
                    views.emailEntrySubmit.isEnabled = it.isEmail()
                }
                .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun startPasswordReset() {
        val email = views.emailEntryInput.content()
        viewModel.handle(OnboardingAction.ResetPassword(email = email, newPassword = null))
    }

    override fun onError(throwable: Throwable) {
        views.emailEntryInput.error = errorFormatter.toHumanReadable(throwable)
    }

    override fun resetViewModel() {
        viewModel.handle(OnboardingAction.ResetResetPassword)
    }
}
