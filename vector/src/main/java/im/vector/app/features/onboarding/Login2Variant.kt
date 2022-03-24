/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.onboarding

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.app.R
import im.vector.app.core.extensions.POP_BACK_STACK_EXCLUSIVE
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.extensions.addFragmentToBackstack
import im.vector.app.core.extensions.resetBackstack
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivityLoginBinding
import im.vector.app.features.home.HomeActivity
import im.vector.app.features.login.LoginCaptchaFragmentArgument
import im.vector.app.features.login.LoginConfig
import im.vector.app.features.login.LoginGenericTextInputFormFragmentArgument
import im.vector.app.features.login.LoginWaitForEmailFragmentArgument
import im.vector.app.features.login.TextInputFormFragmentMode
import im.vector.app.features.login.isSupported
import im.vector.app.features.login.terms.LoginTermsFragmentArgument
import im.vector.app.features.login.terms.toLocalizedLoginTerms
import im.vector.app.features.login2.LoginAction2
import im.vector.app.features.login2.LoginCaptchaFragment2
import im.vector.app.features.login2.LoginFragmentSigninPassword2
import im.vector.app.features.login2.LoginFragmentSigninUsername2
import im.vector.app.features.login2.LoginFragmentSignupPassword2
import im.vector.app.features.login2.LoginFragmentSignupUsername2
import im.vector.app.features.login2.LoginFragmentToAny2
import im.vector.app.features.login2.LoginGenericTextInputFormFragment2
import im.vector.app.features.login2.LoginResetPasswordFragment2
import im.vector.app.features.login2.LoginResetPasswordMailConfirmationFragment2
import im.vector.app.features.login2.LoginResetPasswordSuccessFragment2
import im.vector.app.features.login2.LoginServerSelectionFragment2
import im.vector.app.features.login2.LoginServerUrlFormFragment2
import im.vector.app.features.login2.LoginSplashSignUpSignInSelectionFragment2
import im.vector.app.features.login2.LoginSsoOnlyFragment2
import im.vector.app.features.login2.LoginViewEvents2
import im.vector.app.features.login2.LoginViewModel2
import im.vector.app.features.login2.LoginViewState2
import im.vector.app.features.login2.LoginWaitForEmailFragment2
import im.vector.app.features.login2.LoginWebFragment2
import im.vector.app.features.login2.created.AccountCreatedFragment
import im.vector.app.features.login2.terms.LoginTermsFragment2
import org.matrix.android.sdk.api.auth.registration.FlowResult
import org.matrix.android.sdk.api.auth.registration.Stage
import org.matrix.android.sdk.api.extensions.tryOrNull

private const val FRAGMENT_REGISTRATION_STAGE_TAG = "FRAGMENT_REGISTRATION_STAGE_TAG"
private const val FRAGMENT_LOGIN_TAG = "FRAGMENT_LOGIN_TAG"

class Login2Variant(
        private val views: ActivityLoginBinding,
        private val loginViewModel: LoginViewModel2,
        private val activity: VectorBaseActivity<ActivityLoginBinding>,
        private val supportFragmentManager: FragmentManager
) : OnboardingVariant {

    private val enterAnim = R.anim.enter_fade_in
    private val exitAnim = R.anim.exit_fade_out

    private val popEnterAnim = R.anim.no_anim
    private val popExitAnim = R.anim.exit_fade_out

    private val topFragment: Fragment?
        get() = supportFragmentManager.findFragmentById(views.loginFragmentContainer.id)

    private val commonOption: (FragmentTransaction) -> Unit = { ft ->
        // Find the loginLogo on the current Fragment, this should not return null
        (topFragment?.view as? ViewGroup)
                // Find activity.findViewById does not work, I do not know why
                // activity.findViewById<View?>(views.loginLogo)
                ?.children
                ?.firstOrNull { it.id == R.id.loginLogo }
                ?.let { ft.addSharedElement(it, ViewCompat.getTransitionName(it) ?: "") }
        ft.setCustomAnimations(enterAnim, exitAnim, popEnterAnim, popExitAnim)
    }

    override fun initUiAndData(isFirstCreation: Boolean) {
        if (isFirstCreation) {
            addFirstFragment()
        }

        with(activity) {
            loginViewModel.onEach {
                updateWithState(it)
            }
            loginViewModel.observeViewEvents { handleLoginViewEvents(it) }
        }

        // Get config extra
        val loginConfig = activity.intent.getParcelableExtra<LoginConfig?>(OnboardingActivity.EXTRA_CONFIG)
        if (isFirstCreation) {
            // TODO Check this
            loginViewModel.handle(LoginAction2.InitWith(loginConfig))
        }
    }

    private fun addFirstFragment() {
        activity.addFragment(views.loginFragmentContainer, LoginSplashSignUpSignInSelectionFragment2::class.java)
    }

    private fun handleLoginViewEvents(event: LoginViewEvents2) {
        when (event) {
            is LoginViewEvents2.RegistrationFlowResult                     -> {
                // Check that all flows are supported by the application
                if (event.flowResult.missingStages.any { !it.isSupported() }) {
                    // Display a popup to propose use web fallback
                    onRegistrationStageNotSupported()
                } else {
                    if (event.isRegistrationStarted) {
                        // Go on with registration flow
                        handleRegistrationNavigation(event.flowResult)
                    } else {
                        /*
                        // First ask for login and password
                        // I add a tag to indicate that this fragment is a registration stage.
                        // This way it will be automatically popped in when starting the next registration stage
                        activity.addFragmentToBackstack(views.loginFragmentContainer,
                                LoginFragment2::class.java,
                                tag = FRAGMENT_REGISTRATION_STAGE_TAG,
                                option = commonOption
                        )

                         */
                    }
                }
            }
            is LoginViewEvents2.OutdatedHomeserver                         -> {
                MaterialAlertDialogBuilder(activity)
                        .setTitle(R.string.login_error_outdated_homeserver_title)
                        .setMessage(R.string.login_error_outdated_homeserver_warning_content)
                        .setPositiveButton(R.string.ok, null)
                        .show()
                Unit
            }
            is LoginViewEvents2.OpenServerSelection                        ->
                activity.addFragmentToBackstack(views.loginFragmentContainer,
                        LoginServerSelectionFragment2::class.java,
                        option = { ft ->
                            activity.findViewById<View?>(R.id.loginSplashLogo)?.let { ft.addSharedElement(it, ViewCompat.getTransitionName(it) ?: "") }
                            // Disable transition of text
                            // activity.findViewById<View?>(views.loginSplashTitle)?.let { ft.addSharedElement(it, ViewCompat.getTransitionName(it) ?: "") }
                            // No transition here now actually
                            // activity.findViewById<View?>(views.loginSplashSubmit)?.let { ft.addSharedElement(it, ViewCompat.getTransitionName(it) ?: "") }
                            // TODO Disabled because it provokes a flickering
                            // ft.setCustomAnimations(enterAnim, exitAnim, popEnterAnim, popExitAnim)
                        })
            is LoginViewEvents2.OpenHomeServerUrlFormScreen                -> {
                activity.addFragmentToBackstack(views.loginFragmentContainer,
                        LoginServerUrlFormFragment2::class.java,
                        option = commonOption)
            }
            is LoginViewEvents2.OpenSignInEnterIdentifierScreen            -> {
                activity.addFragmentToBackstack(views.loginFragmentContainer,
                        LoginFragmentSigninUsername2::class.java,
                        option = { ft ->
                            activity.findViewById<View?>(R.id.loginSplashLogo)?.let { ft.addSharedElement(it, ViewCompat.getTransitionName(it) ?: "") }
                            // Disable transition of text
                            // activity.findViewById<View?>(views.loginSplashTitle)?.let { ft.addSharedElement(it, ViewCompat.getTransitionName(it) ?: "") }
                            // No transition here now actually
                            // activity.findViewById<View?>(views.loginSplashSubmit)?.let { ft.addSharedElement(it, ViewCompat.getTransitionName(it) ?: "") }
                            // TODO Disabled because it provokes a flickering
                            // ft.setCustomAnimations(enterAnim, exitAnim, popEnterAnim, popExitAnim)
                        })
            }
            is LoginViewEvents2.OpenSsoOnlyScreen                          -> {
                activity.addFragmentToBackstack(views.loginFragmentContainer,
                        LoginSsoOnlyFragment2::class.java,
                        option = commonOption)
            }
            is LoginViewEvents2.OnWebLoginError                            -> onWebLoginError(event)
            is LoginViewEvents2.OpenResetPasswordScreen                    ->
                activity.addFragmentToBackstack(views.loginFragmentContainer,
                        LoginResetPasswordFragment2::class.java,
                        option = commonOption)
            is LoginViewEvents2.OnResetPasswordSendThreePidDone            -> {
                supportFragmentManager.popBackStack(FRAGMENT_LOGIN_TAG, POP_BACK_STACK_EXCLUSIVE)
                activity.addFragmentToBackstack(views.loginFragmentContainer,
                        LoginResetPasswordMailConfirmationFragment2::class.java,
                        option = commonOption)
            }
            is LoginViewEvents2.OnResetPasswordMailConfirmationSuccess     -> {
                supportFragmentManager.popBackStack(FRAGMENT_LOGIN_TAG, POP_BACK_STACK_EXCLUSIVE)
                activity.addFragmentToBackstack(views.loginFragmentContainer,
                        LoginResetPasswordSuccessFragment2::class.java,
                        option = commonOption)
            }
            is LoginViewEvents2.OnResetPasswordMailConfirmationSuccessDone -> {
                // Go back to the login fragment
                supportFragmentManager.popBackStack(FRAGMENT_LOGIN_TAG, POP_BACK_STACK_EXCLUSIVE)
            }
            is LoginViewEvents2.OnSendEmailSuccess                         ->
                activity.addFragmentToBackstack(views.loginFragmentContainer,
                        LoginWaitForEmailFragment2::class.java,
                        LoginWaitForEmailFragmentArgument(event.email),
                        tag = FRAGMENT_REGISTRATION_STAGE_TAG,
                        option = commonOption)
            is LoginViewEvents2.OpenSigninPasswordScreen                   -> {
                activity.addFragmentToBackstack(views.loginFragmentContainer,
                        LoginFragmentSigninPassword2::class.java,
                        tag = FRAGMENT_LOGIN_TAG,
                        option = commonOption)
            }
            is LoginViewEvents2.OpenSignupPasswordScreen                   -> {
                activity.addFragmentToBackstack(views.loginFragmentContainer,
                        LoginFragmentSignupPassword2::class.java,
                        tag = FRAGMENT_REGISTRATION_STAGE_TAG,
                        option = commonOption)
            }
            is LoginViewEvents2.OpenSignUpChooseUsernameScreen             -> {
                activity.addFragmentToBackstack(views.loginFragmentContainer,
                        LoginFragmentSignupUsername2::class.java,
                        tag = FRAGMENT_REGISTRATION_STAGE_TAG,
                        option = commonOption)
            }
            is LoginViewEvents2.OpenSignInWithAnythingScreen               -> {
                activity.addFragmentToBackstack(views.loginFragmentContainer,
                        LoginFragmentToAny2::class.java,
                        tag = FRAGMENT_LOGIN_TAG,
                        option = commonOption)
            }
            is LoginViewEvents2.OnSendMsisdnSuccess                        ->
                activity.addFragmentToBackstack(views.loginFragmentContainer,
                        LoginGenericTextInputFormFragment2::class.java,
                        LoginGenericTextInputFormFragmentArgument(TextInputFormFragmentMode.ConfirmMsisdn, true, event.msisdn),
                        tag = FRAGMENT_REGISTRATION_STAGE_TAG,
                        option = commonOption)
            is LoginViewEvents2.Failure                                    ->
                // This is handled by the Fragments
                Unit
            is LoginViewEvents2.OnLoginModeNotSupported                    ->
                onLoginModeNotSupported(event.supportedTypes)
            is LoginViewEvents2.OnSessionCreated                           -> handleOnSessionCreated(event)
            is LoginViewEvents2.Finish                                     -> terminate(true)
            is LoginViewEvents2.CancelRegistration                         -> handleCancelRegistration()
        }
    }

    private fun handleCancelRegistration() {
        // Cleanup the back stack
        activity.resetBackstack()
    }

    private fun handleOnSessionCreated(event: LoginViewEvents2.OnSessionCreated) {
        if (event.newAccount) {
            // Propose to set avatar and display name
            // Back on this Fragment will finish the Activity
            activity.addFragmentToBackstack(views.loginFragmentContainer,
                    AccountCreatedFragment::class.java,
                    option = commonOption)
        } else {
            terminate(false)
        }
    }

    private fun terminate(newAccount: Boolean) {
        val intent = HomeActivity.newIntent(
                activity,
                accountCreation = newAccount
        )
        activity.startActivity(intent)
        activity.finish()
    }

    private fun updateWithState(LoginViewState2: LoginViewState2) {
        // Loading
        setIsLoading(LoginViewState2.isLoading)
    }

    // Hack for AccountCreatedFragment
    override fun setIsLoading(isLoading: Boolean) {
        views.loginLoading.isVisible = isLoading
    }

    private fun onWebLoginError(onWebLoginError: LoginViewEvents2.OnWebLoginError) {
        // Pop the backstack
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)

        // And inform the user
        MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.dialog_title_error)
                .setMessage(activity.getString(R.string.login_sso_error_message, onWebLoginError.description, onWebLoginError.errorCode))
                .setPositiveButton(R.string.ok, null)
                .show()
    }

    /**
     * Handle the SSO redirection here
     */
    override fun onNewIntent(intent: Intent?) {
        intent?.data
                ?.let { tryOrNull { it.getQueryParameter("loginToken") } }
                ?.let { loginViewModel.handle(LoginAction2.LoginWithToken(it)) }
    }

    private fun onRegistrationStageNotSupported() {
        MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.app_name)
                .setMessage(activity.getString(R.string.login_registration_not_supported))
                .setPositiveButton(R.string.yes) { _, _ ->
                    activity.addFragmentToBackstack(views.loginFragmentContainer,
                            LoginWebFragment2::class.java,
                            option = commonOption)
                }
                .setNegativeButton(R.string.no, null)
                .show()
    }

    private fun onLoginModeNotSupported(supportedTypes: List<String>) {
        MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.app_name)
                .setMessage(activity.getString(R.string.login_mode_not_supported, supportedTypes.joinToString { "'$it'" }))
                .setPositiveButton(R.string.yes) { _, _ ->
                    activity.addFragmentToBackstack(views.loginFragmentContainer,
                            LoginWebFragment2::class.java,
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
            is Stage.ReCaptcha -> activity.addFragmentToBackstack(views.loginFragmentContainer,
                    LoginCaptchaFragment2::class.java,
                    LoginCaptchaFragmentArgument(stage.publicKey),
                    tag = FRAGMENT_REGISTRATION_STAGE_TAG,
                    option = commonOption)
            is Stage.Email     -> activity.addFragmentToBackstack(views.loginFragmentContainer,
                    LoginGenericTextInputFormFragment2::class.java,
                    LoginGenericTextInputFormFragmentArgument(TextInputFormFragmentMode.SetEmail, stage.mandatory),
                    tag = FRAGMENT_REGISTRATION_STAGE_TAG,
                    option = commonOption)
            is Stage.Msisdn    -> activity.addFragmentToBackstack(views.loginFragmentContainer,
                    LoginGenericTextInputFormFragment2::class.java,
                    LoginGenericTextInputFormFragmentArgument(TextInputFormFragmentMode.SetMsisdn, stage.mandatory),
                    tag = FRAGMENT_REGISTRATION_STAGE_TAG,
                    option = commonOption)
            is Stage.Terms     -> activity.addFragmentToBackstack(views.loginFragmentContainer,
                    LoginTermsFragment2::class.java,
                    LoginTermsFragmentArgument(stage.policies.toLocalizedLoginTerms(activity.getString(R.string.resources_language))),
                    tag = FRAGMENT_REGISTRATION_STAGE_TAG,
                    option = commonOption)
            else               -> Unit // Should not happen
        }
    }
}
