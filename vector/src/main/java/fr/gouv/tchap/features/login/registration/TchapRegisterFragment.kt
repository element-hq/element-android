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

package fr.gouv.tchap.features.login.registration

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.text.isDigitsOnly
import com.airbnb.mvrx.fragmentViewModel
import fr.gouv.tchap.android.sdk.internal.services.threepidplatformdiscover.model.Platform
import fr.gouv.tchap.features.login.TchapAbstractLoginFragment
import fr.gouv.tchap.features.login.TchapLoginAction
import fr.gouv.tchap.features.login.TchapLoginViewEvents
import fr.gouv.tchap.features.platform.PlatformAction
import fr.gouv.tchap.features.platform.PlatformViewEvents
import fr.gouv.tchap.features.platform.PlatformViewModel
import fr.gouv.tchap.features.platform.PlatformViewState
import im.vector.app.R
import im.vector.app.core.extensions.exhaustive
import im.vector.app.databinding.FragmentTchapRegisterBinding
import javax.inject.Inject

/**
 * In this screen:
 * In signin mode:
 * - the user is asked for login (or email) and password to sign in to a homeserver.
 * - He also can reset his password
 * In signup mode:
 * - the user is asked for login and password
 */
class TchapRegisterFragment @Inject constructor(private val platformViewModelFactory: PlatformViewModel.Factory
) : TchapAbstractLoginFragment<FragmentTchapRegisterBinding>(), PlatformViewModel.Factory {

    // Temporary patch for https://github.com/vector-im/riotX-android/issues/1410,
    // waiting for https://github.com/matrix-org/synapse/issues/7576
    private var isNumericOnlyUserIdForbidden = false

    private val viewModel: PlatformViewModel by fragmentViewModel()
    private lateinit var login: String
    private lateinit var password: String

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentTchapRegisterBinding {
        return FragmentTchapRegisterBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar(views.toolbar)
        views.toolbar.setTitle(R.string.tchap_register_title)

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

        login = views.tchapRegisterEmail.text.toString()
        password = views.tchapRegisterPassword.text.toString()

        // This can be called by the IME action, so deal with empty cases
        var error = 0
        if (login.isEmpty()) {
            views.tchapRegisterEmail.error = getString(R.string.error_empty_field_choose_user_name)
            error++
        }
        if (isNumericOnlyUserIdForbidden && login.isDigitsOnly()) {
            views.tchapRegisterEmail.error = "The homeserver does not accept username with only digits."
            error++
        }
        if (password.isEmpty()) {
            views.tchapRegisterPassword.error = getString(R.string.error_empty_field_choose_password)
            error++
        }

        if (password != views.tchapRegisterPasswordConfirm.text.toString()) {
            views.tchapRegisterPasswordConfirm.error = getString(R.string.auth_password_dont_match)
            error++
        }

        if (error == 0) {
            viewModel.handle(PlatformAction.DiscoverTchapPlatform(login))
        }
    }

    private fun cleanupUi() {
//        views.loginSubmit.hideKeyboard()
        views.tchapRegisterEmail.error = null
        views.tchapRegisterPassword.error = null
    }

    private fun updateHomeServer(platform: Platform) {
        loginViewModel.handle(TchapLoginAction.UpdateHomeServer(getString(R.string.server_url_prefix) + platform.hs))
    }

    override fun resetViewModel() {
        loginViewModel.handle(TchapLoginAction.ResetLogin)
    }

//    override fun onError(throwable: Throwable) {
//        // Show M_WEAK_PASSWORD error in the password field
//        if (throwable is Failure.ServerError
//                && throwable.error.code == MatrixError.M_WEAK_PASSWORD) {
//            views.passwordFieldTil.error = errorFormatter.toHumanReadable(throwable)
//        } else {
//            views.loginFieldTil.error = errorFormatter.toHumanReadable(throwable)
//        }
//    }
//
//    override fun updateWithState(state: LoginViewState) {
//        isSignupMode = state.signMode == SignMode.SignUp
//        isNumericOnlyUserIdForbidden = state.serverType == ServerType.MatrixOrg
//
//        setupUi(state)
//        setupAutoFill(state)
//        setupButtons(state)
//
//        when (state.asyncLoginAction) {
//            is Loading -> {
//                // Ensure password is hidden
//                passwordShown = false
//                renderPasswordField()
//            }
//            is Fail    -> {
//                val error = state.asyncLoginAction.error
//                if (error is Failure.ServerError
//                        && error.error.code == MatrixError.M_FORBIDDEN
//                        && error.error.message.isEmpty()) {
//                    // Login with email, but email unknown
//                    views.loginFieldTil.error = getString(R.string.login_login_with_email_error)
//                } else {
//                    // Trick to display the error without text.
//                    views.loginFieldTil.error = " "
//                    if (error.isInvalidPassword() && spaceInPassword()) {
//                        views.passwordFieldTil.error = getString(R.string.auth_invalid_login_param_space_in_password)
//                    } else {
//                        views.passwordFieldTil.error = errorFormatter.toHumanReadable(error)
//                    }
//                }
//            }
//            // Success is handled by the LoginActivity
//            is Success -> Unit
//        }
//
//        when (state.asyncRegistration) {
//            is Loading -> {
//                // Ensure password is hidden
//                passwordShown = false
//                renderPasswordField()
//            }
//            // Success is handled by the LoginActivity
//            is Success -> Unit
//        }
//    }

    /**
     * Detect if password ends or starts with spaces
     */
//    private fun spaceInPassword() = views.passwordField.text.toString().let { it.trim() != it }

    override fun create(initialState: PlatformViewState): PlatformViewModel {
        return platformViewModelFactory.create(initialState)
    }
}
