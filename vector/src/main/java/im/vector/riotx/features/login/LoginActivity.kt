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
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
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
import im.vector.riotx.core.platform.ToolbarConfigurable
import im.vector.riotx.core.platform.VectorBaseActivity
import im.vector.riotx.features.home.HomeActivity
import im.vector.riotx.features.login.terms.LoginTermsFragment
import im.vector.riotx.features.login.terms.LoginTermsFragmentArgument
import im.vector.riotx.features.login.terms.toLocalizedLoginTerms
import kotlinx.android.synthetic.main.activity_login.*
import javax.inject.Inject

/**
 * The LoginActivity manages the fragment navigation and also display the loading View
 */
class LoginActivity : VectorBaseActivity(), ToolbarConfigurable {

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
                    // Assigning to dummy make sure we do not forget a case
                    @Suppress("UNUSED_VARIABLE")
                    val dummy = when (it) {
                        is LoginNavigation.OpenServerSelection                        -> addFragmentToBackstack(R.id.loginFragmentContainer, LoginServerSelectionFragment::class.java,
                                option = { ft ->
                                    val view = findViewById<View?>(R.id.loginSplashLogo)
                                    if (view != null) {
                                        ft.addSharedElement(view, ViewCompat.getTransitionName(view) ?: "")
                                    }
                                })
                        is LoginNavigation.OnServerSelectionDone                      -> onServerSelectionDone()
                        is LoginNavigation.OnSignModeSelected                         -> onSignModeSelected()
                        is LoginNavigation.OnLoginFlowRetrieved                       -> onLoginFlowRetrieved()
                        is LoginNavigation.OnWebLoginError                            -> onWebLoginError(it)
                        is LoginNavigation.OnForgetPasswordClicked                    -> addFragmentToBackstack(R.id.loginFragmentContainer, LoginResetPasswordFragment::class.java)
                        is LoginNavigation.OnResetPasswordSendThreePidDone            -> {
                            supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                            addFragmentToBackstack(R.id.loginFragmentContainer, LoginResetPasswordMailConfirmationFragment::class.java)
                        }
                        is LoginNavigation.OnResetPasswordMailConfirmationSuccess     -> {
                            supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                            addFragmentToBackstack(R.id.loginFragmentContainer, LoginResetPasswordSuccessFragment::class.java)
                        }
                        is LoginNavigation.OnResetPasswordMailConfirmationSuccessDone -> supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                        is LoginNavigation.OnSendEmailSuccess                         -> addFragmentToBackstack(R.id.loginFragmentContainer,
                                LoginWaitForEmailFragment::class.java,
                                LoginWaitForEmailFragmentArgument(it.email),
                                tag = FRAGMENT_REGISTRATION_STAGE_TAG)
                        is LoginNavigation.OnSendMsisdnSuccess                        -> addFragmentToBackstack(R.id.loginFragmentContainer,
                                LoginGenericTextInputFormFragment::class.java,
                                LoginGenericTextInputFormFragmentArgument(TextInputFormFragmentMode.ConfirmMsisdn, true, it.msisdn),
                                tag = FRAGMENT_REGISTRATION_STAGE_TAG)
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
                if (loginViewEvents.flowResult.missingStages.any { !it.isSupported() }) {
                    // Display a popup to propose use web fallback
                    onRegistrationStageNotSupported()
                } else {
                    // Go on with registration flow
                    if (loginViewModel.isPasswordSent) {
                        handleRegistrationNavigation(loginViewEvents.flowResult)
                    } else {
                        // First ask for login and password
                        // I add a tag to indicate that this fragment is a registration stage.
                        // This way it will be automatically popped in when starting the next registration stage
                        addFragmentToBackstack(R.id.loginFragmentContainer,
                                LoginFragment::class.java,
                                tag = FRAGMENT_REGISTRATION_STAGE_TAG)
                    }
                }
            }
            is LoginViewEvents.RegistrationError      ->
                // This is handled by the Fragments
                Unit
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
            SignMode.SignUp  -> {
                // This is managed by the LoginViewEvents
            }
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

    private fun onRegistrationStageNotSupported() {
        AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(getString(R.string.login_registration_not_supported))
                .setPositiveButton(R.string.yes) { _, _ ->
                    addFragmentToBackstack(R.id.loginFragmentContainer, LoginWebFragment::class.java)
                }
                .setNegativeButton(R.string.no, null)
                .show()
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
        // Complete all mandatory stages first
        val mandatoryStage = flowResult.missingStages.firstOrNull { it.mandatory }

        if (mandatoryStage != null) {
            doStage(mandatoryStage)
        } else {
            // Consider optional stages
            val optionalStage = flowResult.missingStages.firstOrNull { !it.mandatory && it !is Stage.Dummy }
            if (optionalStage == null) {
                // Should not happen...
            } else {
                doStage(optionalStage)
            }
        }
    }

    private fun doStage(stage: Stage) {
        // Ensure there is no fragment for registration stage in the backstack
        supportFragmentManager.popBackStack(FRAGMENT_REGISTRATION_STAGE_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE)

        when (stage) {
            is Stage.ReCaptcha -> addFragmentToBackstack(R.id.loginFragmentContainer,
                    LoginCaptchaFragment::class.java,
                    LoginCaptchaFragmentArgument(stage.publicKey),
                    tag = FRAGMENT_REGISTRATION_STAGE_TAG)
            is Stage.Email     -> addFragmentToBackstack(R.id.loginFragmentContainer,
                    LoginGenericTextInputFormFragment::class.java,
                    LoginGenericTextInputFormFragmentArgument(TextInputFormFragmentMode.SetEmail, stage.mandatory),
                    tag = FRAGMENT_REGISTRATION_STAGE_TAG)
            is Stage.Msisdn    -> addFragmentToBackstack(R.id.loginFragmentContainer,
                    LoginGenericTextInputFormFragment::class.java,
                    LoginGenericTextInputFormFragmentArgument(TextInputFormFragmentMode.SetMsisdn, stage.mandatory),
                    tag = FRAGMENT_REGISTRATION_STAGE_TAG)
            is Stage.Terms     -> addFragmentToBackstack(R.id.loginFragmentContainer,
                    LoginTermsFragment::class.java,
                    LoginTermsFragmentArgument(stage.policies.toLocalizedLoginTerms(getString(R.string.resources_language))),
                    tag = FRAGMENT_REGISTRATION_STAGE_TAG)
            else               -> Unit // Should not happen
        }
    }

    override fun configure(toolbar: Toolbar) {
        configureToolbar(toolbar)
    }

    companion object {
        private const val FRAGMENT_REGISTRATION_STAGE_TAG = "FRAGMENT_REGISTRATION_STAGE_TAG"

        private const val EXTRA_CONFIG = "EXTRA_CONFIG"

        fun newIntent(context: Context, loginConfig: LoginConfig?): Intent {
            return Intent(context, LoginActivity::class.java).apply {
                putExtra(EXTRA_CONFIG, loginConfig)
            }
        }
    }
}
