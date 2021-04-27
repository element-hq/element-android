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
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.fragmentViewModel
import fr.gouv.tchap.android.sdk.internal.services.threepidplatformdiscover.model.Platform
import fr.gouv.tchap.features.platform.PlatformAction
import fr.gouv.tchap.features.platform.PlatformViewEvents
import fr.gouv.tchap.features.platform.PlatformViewModel
import fr.gouv.tchap.features.platform.PlatformViewState
import im.vector.app.R
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.extensions.isEmail
import im.vector.app.databinding.FragmentTchapLoginBinding
import im.vector.app.features.login.LoginViewState
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.failure.MatrixError
import org.matrix.android.sdk.api.failure.isInvalidPassword
import javax.inject.Inject

/**
 * In this screen:
 * - the user is asked for email and password to sign in to a homeserver.
 * - He also can reset his password
 */
class TchapLoginFragment @Inject constructor(
        private val platformViewModelFactory: PlatformViewModel.Factory
) : TchapAbstractLoginFragment<FragmentTchapLoginBinding>(), PlatformViewModel.Factory {

    private val viewModel: PlatformViewModel by fragmentViewModel()
    private lateinit var login: String
    private lateinit var password: String

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentTchapLoginBinding {
        return FragmentTchapLoginBinding.inflate(inflater, container, false)
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
                TchapLoginViewEvents.OnLoginFlowRetrieved -> loginViewModel.handle(TchapLoginAction.LoginOrRegister(login, password, getString(R.string.login_default_session_public_name)))
                else                                      ->
                    // This is handled by the Activity
                    Unit
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

        login = views.tchapLoginEmail.text.toString()
        password = views.tchapLoginPassword.text.toString()

        // This can be called by the IME action, so deal with empty cases
        var error = 0
        if (login.isEmpty() || !login.isEmail()) {
            views.tchapLoginEmail.error = getString(R.string.auth_invalid_email)
            error++
        }
        if (password.isEmpty()) {
            views.tchapLoginPassword.error = getString(R.string.error_empty_field_your_password)
            error++
        }

        if (error == 0) {
            viewModel.handle(PlatformAction.DiscoverTchapPlatform(login))
        }
    }

    private fun cleanupUi() {
//        views.loginSubmit.hideKeyboard()
        views.tchapLoginEmail.error = null
        views.tchapLoginPassword.error = null
    }

    private fun updateHomeServer(platform: Platform) {
        loginViewModel.handle(TchapLoginAction.UpdateHomeServer(getString(R.string.server_url_prefix) + platform.hs))
    }

    override fun resetViewModel() {
        loginViewModel.handle(TchapLoginAction.ResetLogin)
    }

    override fun onError(throwable: Throwable) {
        views.tchapLoginEmail.error = errorFormatter.toHumanReadable(throwable)
    }

    override fun updateWithState(state: LoginViewState) {
        when (state.asyncLoginAction) {
            is Loading -> Unit
            is Fail    -> {
                val error = state.asyncLoginAction.error
                if (error is Failure.ServerError
                        && error.error.code == MatrixError.M_FORBIDDEN
                        && error.error.message.isEmpty()) {
                    // Login with email, but email unknown
                    views.tchapLoginEmail.error = getString(R.string.login_error_forbidden)
                } else {
                    if (error.isInvalidPassword() && spaceInPassword()) {
                        views.tchapLoginPassword.error = getString(R.string.auth_invalid_login_param_space_in_password)
                    } else {
                        views.tchapLoginPassword.error = errorFormatter.toHumanReadable(error)
                    }
                }
            }
            // Success is handled by the LoginActivity
            is Success -> Unit
        }
    }

    override fun create(initialState: PlatformViewState): PlatformViewModel {
        return platformViewModelFactory.create(initialState)
    }

    /**
     * Detect if password ends or starts with spaces
     */
    private fun spaceInPassword() = views.tchapLoginPassword.text.toString().let { it.trim() != it }
}
