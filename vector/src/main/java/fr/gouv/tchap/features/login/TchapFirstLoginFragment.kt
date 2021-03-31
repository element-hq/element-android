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
import com.airbnb.mvrx.fragmentViewModel
import fr.gouv.tchap.android.sdk.internal.services.threepidplatformdiscover.model.Platform
import fr.gouv.tchap.features.platform.PlatformAction
import fr.gouv.tchap.features.platform.PlatformViewEvents
import fr.gouv.tchap.features.platform.PlatformViewModel
import fr.gouv.tchap.features.platform.PlatformViewState
import im.vector.app.R
import im.vector.app.core.extensions.exhaustive
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
class TchapFirstLoginFragment @Inject constructor(
        private val platformViewModelFactory: PlatformViewModel.Factory
) : TchapAbstractLoginFragment<FragmentTchapFirstLoginBinding>(), PlatformViewModel.Factory {

    private val viewModel: PlatformViewModel by fragmentViewModel()
    private var isSignupMode = false
    private lateinit var login: String
    private lateinit var password: String

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentTchapFirstLoginBinding {
        return FragmentTchapFirstLoginBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar(views.toolbar)
        views.toolbar.setTitle(R.string.tchap_connection_title)

        viewModel.observeViewEvents {
            when (it) {
                is PlatformViewEvents.Loading -> showLoading(it.message)
                is PlatformViewEvents.Failure -> {
                    // Dialog is displayed by the Activity
                }
                is PlatformViewEvents.Success -> updateHomeServer(it.platform)
            }.exhaustive
        }

        loginViewModel.observeViewEvents {
            when (it) {
                is TchapLoginViewEvents.Failure                -> TODO()
                is TchapLoginViewEvents.Loading                -> TODO()
                TchapLoginViewEvents.OnForgetPasswordClicked   -> TODO()
                TchapLoginViewEvents.OnLoginFlowRetrieved      -> loginViewModel.handle(TchapLoginAction.LoginOrRegister(login, password, getString(R.string.login_default_session_public_name)))
                is TchapLoginViewEvents.OnSendEmailSuccess     -> TODO()
                is TchapLoginViewEvents.OnSignModeSelected     -> TODO()
                TchapLoginViewEvents.OutdatedHomeserver        -> TODO()
                is TchapLoginViewEvents.RegistrationFlowResult -> TODO()
            }.exhaustive
        }
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

        login = views.tchapFirstLoginEmail.text.toString()
        password = views.tchapFirstLoginPassword.text.toString()

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
            viewModel.handle(PlatformAction.DiscoverTchapPlatform(login))
        }
    }

    private fun cleanupUi() {
//        views.loginSubmit.hideKeyboard()
        views.tchapFirstLoginEmail.error = null
        views.tchapFirstLoginPassword.error = null
    }

    private fun updateHomeServer(platform: Platform) {
        loginViewModel.handle(TchapLoginAction.UpdateHomeServer(getString(R.string.server_url_prefix) + platform.hs))
    }

    override fun resetViewModel() {
        loginViewModel.handle(TchapLoginAction.ResetLogin)
    }

    override fun create(initialState: PlatformViewState): PlatformViewModel {
        return platformViewModelFactory.create(initialState)
    }
}
