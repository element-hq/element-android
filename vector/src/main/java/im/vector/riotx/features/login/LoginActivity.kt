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
import com.airbnb.mvrx.viewModel
import com.airbnb.mvrx.withState
import im.vector.matrix.android.api.auth.registration.FlowResult
import im.vector.matrix.android.api.auth.registration.Stage
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
                        is LoginNavigation.OpenServerSelection        -> addFragmentToBackstack(R.id.loginFragmentContainer, LoginServerSelectionFragment::class.java)
                        is LoginNavigation.OnServerSelectionDone      -> onServerSelectionDone()
                        is LoginNavigation.OnSignModeSelected         -> onSignModeSelected()
                        is LoginNavigation.OnLoginFlowRetrieved       -> onLoginFlowRetrieved()
                        is LoginNavigation.OnWebLoginError            -> onWebLoginError(it)
                        is LoginNavigation.OnForgetPasswordClicked    -> addFragmentToBackstack(R.id.loginFragmentContainer, LoginResetPasswordFragment::class.java)
                        is LoginNavigation.OnResetPasswordSuccess     -> {
                            supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                            addFragmentToBackstack(R.id.loginFragmentContainer, LoginResetPasswordSuccessFragment::class.java)
                        }
                        is LoginNavigation.OnResetPasswordSuccessDone -> supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    }
                }
                .disposeOnDestroy()

        loginViewModel
                .subscribe(this) {
                    updateWithState(it)
                }
                .disposeOnDestroy()

        loginViewModel.viewEvents
                .observe()
                .subscribe {
                    handleLoginViewEvents(it)
                }
                .disposeOnDestroy()
    }

    private fun handleLoginViewEvents(loginViewEvents: LoginViewEvents) {
        when (loginViewEvents) {
            is LoginViewEvents.RegistrationFlowResult -> {
                // Check that all flows are supported by the application
                if (loginViewEvents.flowResult.missingStages.any { it is Stage.Other }) {
                    // Display a popup to propose use web fallback
                    // TODO
                } else {
                    // Go on with registration flow
                    // loginSharedActionViewModel.post(LoginNavigation.OnSignModeSelected)
                    if (loginViewModel.isPasswordSent) {
                        handleRegistrationNavigation(loginViewEvents.flowResult)
                    } else {
                        // First ask for login and password
                        addFragmentToBackstack(R.id.loginFragmentContainer, LoginFragment::class.java)
                    }
                }
            }
        }
    }

    private fun onLoginFlowRetrieved() {
        addFragmentToBackstack(R.id.loginFragmentContainer, LoginSignUpSignInSelectionFragment::class.java)
    }

    private fun updateWithState(loginViewState: LoginViewState) {
        if (loginViewState.isUserLogged()) {
            val intent = HomeActivity.newIntent(this)
            startActivity(intent)
            finish()
            return
        }

        // Loading
        loginLoading.isVisible = loginViewState.isLoading()
    }

    private fun onWebLoginError(onWebLoginError: LoginNavigation.OnWebLoginError) {
        // Pop the backstack
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)

        // And inform the user
        AlertDialog.Builder(this)
                .setTitle(R.string.dialog_title_error)
                .setMessage(getString(R.string.login_sso_error_message, onWebLoginError.description, onWebLoginError.errorCode))
                .setPositiveButton(R.string.ok, null)
                .show()
    }

    private fun onServerSelectionDone() {
        when (loginViewModel.serverType) {
            ServerType.MatrixOrg -> Unit // In this case, we wait for the login flow
            ServerType.Modular,
            ServerType.Other     -> addFragmentToBackstack(R.id.loginFragmentContainer, LoginServerUrlFormFragment::class.java)
        }
    }

    private fun onSignModeSelected() {
        when (loginViewModel.signMode) {
            SignMode.Unknown -> error("Sign mode has to be set before calling this method")
            SignMode.SignUp  -> addFragmentToBackstack(R.id.loginFragmentContainer, LoginFragment::class.java)
            SignMode.SignIn  -> {
                // It depends on the LoginMode
                withState(loginViewModel) {
                    when (val loginMode = it.asyncHomeServerLoginFlowRequest.invoke()) {
                        null                     -> error("Developer error")
                        LoginMode.Password       -> addFragmentToBackstack(R.id.loginFragmentContainer, LoginFragment::class.java)
                        LoginMode.Sso            -> addFragmentToBackstack(R.id.loginFragmentContainer, LoginWebFragment::class.java)
                        is LoginMode.Unsupported -> onLoginModeNotSupported(loginMode)
                    }
                }
            }
        }
    }

    private fun onLoginModeNotSupported(unsupportedLoginMode: LoginMode.Unsupported) {
        AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(getString(R.string.login_mode_not_supported, unsupportedLoginMode.types.joinToString { "'$it'" }))
                .setPositiveButton(R.string.yes) { _, _ ->
                    addFragmentToBackstack(R.id.loginFragmentContainer, LoginWebFragment::class.java)
                }
                .setNegativeButton(R.string.no, null)
                .show()
    }

    private fun handleRegistrationNavigation(flowResult: FlowResult) {
        // Complete all mandatory stage first
        val mandatoryStages = flowResult.missingStages.filter { it.mandatory }

        if (mandatoryStages.isEmpty()) {
            // Consider optional stages
            val optionalStages = flowResult.missingStages.filter { !it.mandatory }
            if (optionalStages.isEmpty()) {
                // Should not happen...
            } else {
                doStage(optionalStages.first())
            }
        } else {
            doStage(mandatoryStages.first())
        }
    }

    private fun doStage(stage: Stage) {
        when (stage) {
            is Stage.ReCaptcha -> addFragmentToBackstack(R.id.loginFragmentContainer, LoginCaptchaFragment::class.java)
            is Stage.Email     -> addFragmentToBackstack(R.id.loginFragmentContainer,
                    LoginGenericTextInputFormFragment::class.java,
                    LoginGenericTextInputFormFragmentArgument(TextInputFormFragmentMode.SetEmail, stage.mandatory))
            is Stage.Msisdn
                               -> addFragmentToBackstack(R.id.loginFragmentContainer, LoginGenericTextInputFormFragment::class.java,
                    LoginGenericTextInputFormFragmentArgument(TextInputFormFragmentMode.SetMsisdn, stage.mandatory))
            is Stage.Terms
                               -> TODO()
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
