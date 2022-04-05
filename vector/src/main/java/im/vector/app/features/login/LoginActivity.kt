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

package im.vector.app.features.login

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.airbnb.mvrx.viewModel
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.POP_BACK_STACK_EXCLUSIVE
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.extensions.addFragmentToBackstack
import im.vector.app.core.extensions.validateBackPressed
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivityLoginBinding
import im.vector.app.features.analytics.plan.MobileScreen
import im.vector.app.features.home.HomeActivity
import im.vector.app.features.login.terms.LoginTermsFragment
import im.vector.app.features.login.terms.LoginTermsFragmentArgument
import im.vector.app.features.login.terms.toLocalizedLoginTerms
import im.vector.app.features.pin.UnlockedActivity
import org.matrix.android.sdk.api.auth.registration.FlowResult
import org.matrix.android.sdk.api.auth.registration.Stage
import org.matrix.android.sdk.api.extensions.tryOrNull

/**
 * The LoginActivity manages the fragment navigation and also display the loading View
 */
@AndroidEntryPoint
open class LoginActivity : VectorBaseActivity<ActivityLoginBinding>(), UnlockedActivity {

    private val loginViewModel: LoginViewModel by viewModel()

    private val enterAnim = R.anim.enter_fade_in
    private val exitAnim = R.anim.exit_fade_out

    private val popEnterAnim = R.anim.no_anim
    private val popExitAnim = R.anim.exit_fade_out

    private val topFragment: Fragment?
        get() = supportFragmentManager.findFragmentById(views.loginFragmentContainer.id)

    private val commonOption: (FragmentTransaction) -> Unit = { ft ->
        // Find the loginLogo on the current Fragment, this should not return null
        (topFragment?.view as? ViewGroup)
                // Find findViewById does not work, I do not know why
                // findViewById<View?>(R.id.loginLogo)
                ?.children
                ?.firstOrNull { it.id == R.id.loginLogo }
                ?.let { ft.addSharedElement(it, ViewCompat.getTransitionName(it) ?: "") }
        ft.setCustomAnimations(enterAnim, exitAnim, popEnterAnim, popExitAnim)
    }

    final override fun getBinding() = ActivityLoginBinding.inflate(layoutInflater)

    override fun getCoordinatorLayout() = views.coordinatorLayout

    override fun initUiAndData() {
        analyticsScreenName = MobileScreen.ScreenName.Login

        if (isFirstCreation()) {
            addFirstFragment()
        }

        loginViewModel.onEach {
            updateWithState(it)
        }

        loginViewModel.observeViewEvents { handleLoginViewEvents(it) }

        // Get config extra
        val loginConfig = intent.getParcelableExtra<LoginConfig?>(EXTRA_CONFIG)
        if (isFirstCreation()) {
            loginViewModel.handle(LoginAction.InitWith(loginConfig))
        }
    }

    protected open fun addFirstFragment() {
        addFragment(views.loginFragmentContainer, LoginSplashFragment::class.java)
    }

    private fun handleLoginViewEvents(loginViewEvents: LoginViewEvents) {
        when (loginViewEvents) {
            is LoginViewEvents.RegistrationFlowResult                     -> {
                // Check that all flows are supported by the application
                if (loginViewEvents.flowResult.missingStages.any { !it.isSupported() }) {
                    // Display a popup to propose use web fallback
                    onRegistrationStageNotSupported()
                } else {
                    if (loginViewEvents.isRegistrationStarted) {
                        // Go on with registration flow
                        handleRegistrationNavigation(loginViewEvents.flowResult)
                    } else {
                        // First ask for login and password
                        // I add a tag to indicate that this fragment is a registration stage.
                        // This way it will be automatically popped in when starting the next registration stage
                        addFragmentToBackstack(views.loginFragmentContainer,
                                LoginFragment::class.java,
                                tag = FRAGMENT_REGISTRATION_STAGE_TAG,
                                option = commonOption
                        )
                    }
                }
            }
            is LoginViewEvents.OutdatedHomeserver                         -> {
                MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.login_error_outdated_homeserver_title)
                        .setMessage(R.string.login_error_outdated_homeserver_warning_content)
                        .setPositiveButton(R.string.ok, null)
                        .show()
                Unit
            }
            is LoginViewEvents.OpenServerSelection                        ->
                addFragmentToBackstack(views.loginFragmentContainer,
                        LoginServerSelectionFragment::class.java,
                        option = { ft ->
                            findViewById<View?>(R.id.loginSplashLogo)?.let { ft.addSharedElement(it, ViewCompat.getTransitionName(it) ?: "") }
                            // Disable transition of text
                            // findViewById<View?>(R.id.loginSplashTitle)?.let { ft.addSharedElement(it, ViewCompat.getTransitionName(it) ?: "") }
                            // No transition here now actually
                            // findViewById<View?>(R.id.loginSplashSubmit)?.let { ft.addSharedElement(it, ViewCompat.getTransitionName(it) ?: "") }
                            // TODO Disabled because it provokes a flickering
                            // ft.setCustomAnimations(enterAnim, exitAnim, popEnterAnim, popExitAnim)
                        })
            is LoginViewEvents.OnServerSelectionDone                      -> onServerSelectionDone(loginViewEvents)
            is LoginViewEvents.OnSignModeSelected                         -> onSignModeSelected(loginViewEvents)
            is LoginViewEvents.OnLoginFlowRetrieved                       ->
                addFragmentToBackstack(views.loginFragmentContainer,
                        LoginSignUpSignInSelectionFragment::class.java,
                        option = commonOption)
            is LoginViewEvents.OnWebLoginError                            -> onWebLoginError(loginViewEvents)
            is LoginViewEvents.OnForgetPasswordClicked                    ->
                addFragmentToBackstack(views.loginFragmentContainer,
                        LoginResetPasswordFragment::class.java,
                        option = commonOption)
            is LoginViewEvents.OnResetPasswordSendThreePidDone            -> {
                supportFragmentManager.popBackStack(FRAGMENT_LOGIN_TAG, POP_BACK_STACK_EXCLUSIVE)
                addFragmentToBackstack(views.loginFragmentContainer,
                        LoginResetPasswordMailConfirmationFragment::class.java,
                        option = commonOption)
            }
            is LoginViewEvents.OnResetPasswordMailConfirmationSuccess     -> {
                supportFragmentManager.popBackStack(FRAGMENT_LOGIN_TAG, POP_BACK_STACK_EXCLUSIVE)
                addFragmentToBackstack(views.loginFragmentContainer,
                        LoginResetPasswordSuccessFragment::class.java,
                        option = commonOption)
            }
            is LoginViewEvents.OnResetPasswordMailConfirmationSuccessDone -> {
                // Go back to the login fragment
                supportFragmentManager.popBackStack(FRAGMENT_LOGIN_TAG, POP_BACK_STACK_EXCLUSIVE)
            }
            is LoginViewEvents.OnSendEmailSuccess                         -> {
                // Pop the enter email Fragment
                supportFragmentManager.popBackStack(FRAGMENT_REGISTRATION_STAGE_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                addFragmentToBackstack(views.loginFragmentContainer,
                        LoginWaitForEmailFragment::class.java,
                        LoginWaitForEmailFragmentArgument(loginViewEvents.email),
                        tag = FRAGMENT_REGISTRATION_STAGE_TAG,
                        option = commonOption)
            }
            is LoginViewEvents.OnSendMsisdnSuccess                        -> {
                // Pop the enter Msisdn Fragment
                supportFragmentManager.popBackStack(FRAGMENT_REGISTRATION_STAGE_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                addFragmentToBackstack(views.loginFragmentContainer,
                        LoginGenericTextInputFormFragment::class.java,
                        LoginGenericTextInputFormFragmentArgument(TextInputFormFragmentMode.ConfirmMsisdn, true, loginViewEvents.msisdn),
                        tag = FRAGMENT_REGISTRATION_STAGE_TAG,
                        option = commonOption)
            }
            is LoginViewEvents.Failure,
            is LoginViewEvents.Loading                                    ->
                // This is handled by the Fragments
                Unit
        }
    }

    private fun updateWithState(loginViewState: LoginViewState) {
        if (loginViewState.isUserLogged()) {
            if (loginViewState.signMode == SignMode.SignUp) {
                // change the screen name
                analyticsScreenName = MobileScreen.ScreenName.Register
            }
            val intent = HomeActivity.newIntent(
                    this,
                    accountCreation = loginViewState.signMode == SignMode.SignUp
            )
            startActivity(intent)
            finish()
            return
        }

        // Loading
        views.loginLoading.isVisible = loginViewState.isLoading()
    }

    private fun onWebLoginError(onWebLoginError: LoginViewEvents.OnWebLoginError) {
        // Pop the backstack
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)

        // And inform the user
        MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_title_error)
                .setMessage(getString(R.string.login_sso_error_message, onWebLoginError.description, onWebLoginError.errorCode))
                .setPositiveButton(R.string.ok, null)
                .show()
    }

    private fun onServerSelectionDone(loginViewEvents: LoginViewEvents.OnServerSelectionDone) {
        when (loginViewEvents.serverType) {
            ServerType.MatrixOrg -> Unit // In this case, we wait for the login flow
            ServerType.EMS,
            ServerType.Other     -> addFragmentToBackstack(views.loginFragmentContainer,
                    LoginServerUrlFormFragment::class.java,
                    option = commonOption)
            ServerType.Unknown   -> Unit /* Should not happen */
        }
    }

    private fun onSignModeSelected(loginViewEvents: LoginViewEvents.OnSignModeSelected) = withState(loginViewModel) { state ->
        // state.signMode could not be ready yet. So use value from the ViewEvent
        when (loginViewEvents.signMode) {
            SignMode.Unknown            -> error("Sign mode has to be set before calling this method")
            SignMode.SignUp             -> {
                // This is managed by the LoginViewEvents
            }
            SignMode.SignIn             -> {
                // It depends on the LoginMode
                when (state.loginMode) {
                    LoginMode.Unknown,
                    is LoginMode.Sso      -> error("Developer error")
                    is LoginMode.SsoAndPassword,
                    LoginMode.Password    -> addFragmentToBackstack(views.loginFragmentContainer,
                            LoginFragment::class.java,
                            tag = FRAGMENT_LOGIN_TAG,
                            option = commonOption)
                    LoginMode.Unsupported -> onLoginModeNotSupported(state.loginModeSupportedTypes)
                }
            }
            SignMode.SignInWithMatrixId -> addFragmentToBackstack(views.loginFragmentContainer,
                    LoginFragment::class.java,
                    tag = FRAGMENT_LOGIN_TAG,
                    option = commonOption)
        }
    }

    /**
     * Handle the SSO redirection here
     */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        intent?.data
                ?.let { tryOrNull { it.getQueryParameter("loginToken") } }
                ?.let { loginViewModel.handle(LoginAction.LoginWithToken(it)) }
    }

    override fun onBackPressed() {
        validateBackPressed { super.onBackPressed() }
    }

    private fun onRegistrationStageNotSupported() {
        MaterialAlertDialogBuilder(this)
                .setTitle(R.string.app_name)
                .setMessage(getString(R.string.login_registration_not_supported))
                .setPositiveButton(R.string.yes) { _, _ ->
                    addFragmentToBackstack(views.loginFragmentContainer,
                            LoginWebFragment::class.java,
                            option = commonOption)
                }
                .setNegativeButton(R.string.no, null)
                .show()
    }

    private fun onLoginModeNotSupported(supportedTypes: List<String>) {
        MaterialAlertDialogBuilder(this)
                .setTitle(R.string.app_name)
                .setMessage(getString(R.string.login_mode_not_supported, supportedTypes.joinToString { "'$it'" }))
                .setPositiveButton(R.string.yes) { _, _ ->
                    addFragmentToBackstack(views.loginFragmentContainer,
                            LoginWebFragment::class.java,
                            option = commonOption)
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
            is Stage.ReCaptcha -> addFragmentToBackstack(views.loginFragmentContainer,
                    LoginCaptchaFragment::class.java,
                    LoginCaptchaFragmentArgument(stage.publicKey),
                    tag = FRAGMENT_REGISTRATION_STAGE_TAG,
                    option = commonOption)
            is Stage.Email     -> addFragmentToBackstack(views.loginFragmentContainer,
                    LoginGenericTextInputFormFragment::class.java,
                    LoginGenericTextInputFormFragmentArgument(TextInputFormFragmentMode.SetEmail, stage.mandatory),
                    tag = FRAGMENT_REGISTRATION_STAGE_TAG,
                    option = commonOption)
            is Stage.Msisdn    -> addFragmentToBackstack(views.loginFragmentContainer,
                    LoginGenericTextInputFormFragment::class.java,
                    LoginGenericTextInputFormFragmentArgument(TextInputFormFragmentMode.SetMsisdn, stage.mandatory),
                    tag = FRAGMENT_REGISTRATION_STAGE_TAG,
                    option = commonOption)
            is Stage.Terms     -> addFragmentToBackstack(views.loginFragmentContainer,
                    LoginTermsFragment::class.java,
                    LoginTermsFragmentArgument(stage.policies.toLocalizedLoginTerms(getString(R.string.resources_language))),
                    tag = FRAGMENT_REGISTRATION_STAGE_TAG,
                    option = commonOption)
            else               -> Unit // Should not happen
        }
    }

    companion object {
        private const val FRAGMENT_REGISTRATION_STAGE_TAG = "FRAGMENT_REGISTRATION_STAGE_TAG"
        private const val FRAGMENT_LOGIN_TAG = "FRAGMENT_LOGIN_TAG"

        private const val EXTRA_CONFIG = "EXTRA_CONFIG"

        fun newIntent(context: Context, loginConfig: LoginConfig?): Intent {
            return Intent(context, LoginActivity::class.java).apply {
                putExtra(EXTRA_CONFIG, loginConfig)
            }
        }

        fun redirectIntent(context: Context, data: Uri?): Intent {
            return Intent(context, LoginActivity::class.java).apply {
                setData(data)
            }
        }
    }
}
