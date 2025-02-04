/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.databinding.FragmentLoginResetPasswordSuccessBinding

/**
 * In this screen, we confirm to the user that his password has been reset.
 */
@AndroidEntryPoint
class LoginResetPasswordSuccessFragment :
        AbstractLoginFragment<FragmentLoginResetPasswordSuccessBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentLoginResetPasswordSuccessBinding {
        return FragmentLoginResetPasswordSuccessBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        views.resetPasswordSuccessSubmit.debouncedClicks { submit() }
    }

    private fun submit() {
        loginViewModel.handle(LoginAction.PostViewEvent(LoginViewEvents.OnResetPasswordMailConfirmationSuccessDone))
    }

    override fun resetViewModel() {
        loginViewModel.handle(LoginAction.ResetResetPassword)
    }
}
