/*
 * Copyright 2021 New Vector Ltd
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

package fr.gouv.tchap.features.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import im.vector.app.databinding.FragmentTchapFirstWelcomeBinding
import im.vector.app.features.login.SignMode

import javax.inject.Inject

/**
 * In this screen, the user is asked to sign up or to sign in to the homeserver
 */
class TchapFirstWelcomeFragment @Inject constructor() : TchapAbstractLoginFragment<FragmentTchapFirstWelcomeBinding>() {

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentTchapFirstWelcomeBinding {
        return FragmentTchapFirstWelcomeBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupHomeServer()
        setupViews()
    }

    private fun setupViews() {
        views.fragmentTchapFirstWelcomeRegisterButton.setOnClickListener { signUp() }
        views.fragmentTchapFirstWelcomeLoginButton.setOnClickListener { signIn() }
    }

    private fun signUp() {
        loginViewModel.handle(TchapLoginAction.UpdateSignMode(SignMode.SignUp))
    }

    private fun signIn() {
        loginViewModel.handle(TchapLoginAction.UpdateSignMode(SignMode.SignIn))
    }

    private fun setupHomeServer() {
        //TODO add tchap homeservers from config by flavour
        loginViewModel.handle(TchapLoginAction.UpdateHomeServer("https://matrix.i.tchap.gouv.fr"))
    }

    override fun resetViewModel() {
        loginViewModel.handle(TchapLoginAction.ResetSignMode)
    }
}
