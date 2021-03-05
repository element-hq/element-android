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
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import im.vector.app.R
import im.vector.app.databinding.FragmentTchapFirstLoginBinding

import javax.inject.Inject

/**
 * In this screen:
 * In signin mode:
 * - the user is asked for login (or email) and password to sign in to a homeserver.
 * - He also can reset his password
 * In signup mode:
 * - the user is asked for login and password
 */
class TchapFirstLoginFragment @Inject constructor() : TchapAbstractLoginFragment<FragmentTchapFirstLoginBinding>() {

    private var isSignupMode = false

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentTchapFirstLoginBinding {
        return FragmentTchapFirstLoginBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar(views.toolbar)
        views.toolbar.setTitle(R.string.tchap_connection_title)
    }

    override fun getMenuRes() = R.menu.tchap_menu_next

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_next -> {
                submit()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun submit() {
        cleanupUi()

        val login = views.tchapFirstLoginEmail.text.toString()
        val password = views.tchapFirstLoginPassword.text.toString()

        // This can be called by the IME action, so deal with empty cases
        var error = 0
        if (login.isEmpty()) {
            views.tchapFirstLoginEmail.error = getString(if (isSignupMode) {
                R.string.error_empty_field_choose_user_name
            } else {
                R.string.error_empty_field_enter_user_name
            })
            error++
        }
        if (password.isEmpty()) {
            views.tchapFirstLoginPassword.error = getString(if (isSignupMode) {
                R.string.error_empty_field_choose_password
            } else {
                R.string.error_empty_field_your_password
            })
            error++
        }

        if (error == 0) {
            loginViewModel.handle(TchapLoginAction.LoginOrRegister(login, password, getString(R.string.login_default_session_public_name)))
        }
    }

    private fun cleanupUi() {
//        views.loginSubmit.hideKeyboard()
        views.tchapFirstLoginEmail.error = null
        views.tchapFirstLoginPassword.error = null
    }

    override fun resetViewModel() {
        loginViewModel.handle(TchapLoginAction.ResetLogin)
    }
}
