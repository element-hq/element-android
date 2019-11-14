/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.riotx.features.login

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.viewModel
import com.airbnb.mvrx.withState
import im.vector.riotx.R
import im.vector.riotx.core.di.ScreenComponent
import im.vector.riotx.core.extensions.addFragment
import im.vector.riotx.core.extensions.addFragmentToBackstack
import im.vector.riotx.core.platform.VectorBaseActivity
import im.vector.riotx.features.disclaimer.showDisclaimerDialog
import im.vector.riotx.features.home.HomeActivity
import kotlinx.android.synthetic.main.activity_login.*
import javax.inject.Inject

/**
 * The LoginActivity manages the fragment navigation and also display the loading View
 */
class LoginActivity : VectorBaseActivity() {

    private val loginViewModel: LoginViewModel by viewModel()
    private lateinit var loginSharedActionViewModel: LoginSharedActionViewModel

    @Inject lateinit var loginViewModelFactory: LoginViewModel.Factory

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun getLayoutRes() = R.layout.activity_login

    override fun initUiAndData() {
        if (isFirstCreation()) {
            addFragment(R.id.loginFragmentContainer, LoginSplashFragment::class.java)
        }

        // Get config extra
        val loginConfig = intent.getParcelableExtra<LoginConfig?>(EXTRA_CONFIG)
        if (loginConfig != null && isFirstCreation()) {
            // TODO Check this
            loginViewModel.handle(LoginAction.InitWith(loginConfig))
        }

        loginSharedActionViewModel = viewModelProvider.get(LoginSharedActionViewModel::class.java)
        loginSharedActionViewModel.observe()
                .subscribe {
                    when (it) {
                        is LoginNavigation.OpenServerSelection     -> addFragmentToBackstack(R.id.loginFragmentContainer, LoginServerSelectionFragment::class.java)
                        is LoginNavigation.OnServerSelectionDone   -> onServerSelectionDone()
                        is LoginNavigation.OnSignModeSelected      -> onSignModeSelected(it)
                        is LoginNavigation.OnLoginFlowRetrieved    -> onLoginFlowRetrieved(it)
                        is LoginNavigation.OnSsoLoginFallbackError -> onSsoLoginFallbackError(it)
                    }
                }
                .disposeOnDestroy()

        loginViewModel
                .subscribe(this) {
                    updateWithState(it)
                }
                .disposeOnDestroy()
    }

    private fun onLoginFlowRetrieved(onLoginFlowRetrieved: LoginNavigation.OnLoginFlowRetrieved) {
        when (onLoginFlowRetrieved.loginMode) {
            LoginMode.Sso      -> addFragmentToBackstack(R.id.loginFragmentContainer, LoginSsoFallbackFragment::class.java)
            LoginMode.Unsupported,
            LoginMode.Password -> addFragmentToBackstack(R.id.loginFragmentContainer, LoginSignUpSignInSelectionFragment::class.java)
        }
    }

    private fun updateWithState(loginViewState: LoginViewState) {
        if (loginViewState.asyncLoginAction is Success) {
            val intent = HomeActivity.newIntent(this)
            startActivity(intent)
            finish()
            return
        }

        // Loading
        loginLoading.isVisible = loginViewState.isLoading()
    }

    private fun onSsoLoginFallbackError(onSsoLoginFallbackError: LoginNavigation.OnSsoLoginFallbackError) {
        // Pop the backstack
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)

        // And inform the user
        AlertDialog.Builder(this)
                .setTitle(R.string.dialog_title_error)
                .setMessage(getString(R.string.login_sso_error_message, onSsoLoginFallbackError.description, onSsoLoginFallbackError.errorCode))
                .setPositiveButton(R.string.ok, null)
                .show()
    }

    private fun onServerSelectionDone() = withState(loginViewModel) {
        when (it.serverType) {
            ServerType.MatrixOrg -> Unit // In this case, we wait for the login flow
            ServerType.Modular,
            ServerType.Other     -> addFragmentToBackstack(R.id.loginFragmentContainer, LoginServerUrlFormFragment::class.java)
        }
    }

    private fun onSignModeSelected(mode: LoginNavigation.OnSignModeSelected) {
        // We cannot use the state, it is not ready...
        when (mode.signMode) {
            SignMode.Unknown -> error("Sign mode has to be set before calling this method")
            SignMode.SignUp  -> Unit // TODO addFragmentToBackstack(R.id.loginFragmentContainer, SignUpFragment::class.java)
            SignMode.SignIn  -> addFragmentToBackstack(R.id.loginFragmentContainer, LoginFragment::class.java)
        }
    }

    override fun onResume() {
        super.onResume()

        showDisclaimerDialog(this)
    }

    companion object {
        private const val EXTRA_CONFIG = "EXTRA_CONFIG"

        fun newIntent(context: Context, loginConfig: LoginConfig?): Intent {
            return Intent(context, LoginActivity::class.java).apply {
                putExtra(EXTRA_CONFIG, loginConfig)
            }
        }
    }
}
