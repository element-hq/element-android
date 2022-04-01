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

package im.vector.app.features.onboarding.ftueauth

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.app.R
import im.vector.app.core.extensions.POP_BACK_STACK_EXCLUSIVE
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.extensions.addFragmentToBackstack
import im.vector.app.core.extensions.popBackstack
import im.vector.app.core.extensions.replaceFragment
import im.vector.app.core.platform.ScreenOrientationLocker
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivityLoginBinding
import im.vector.app.features.VectorFeatures
import im.vector.app.features.home.HomeActivity
import im.vector.app.features.login.LoginConfig
import im.vector.app.features.login.LoginMode
import im.vector.app.features.login.ServerType
import im.vector.app.features.login.SignMode
import im.vector.app.features.login.TextInputFormFragmentMode
import im.vector.app.features.login.isSupported
import im.vector.app.features.login.terms.toLocalizedLoginTerms
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.onboarding.OnboardingActivity
import im.vector.app.features.onboarding.OnboardingVariant
import im.vector.app.features.onboarding.OnboardingViewEvents
import im.vector.app.features.onboarding.OnboardingViewModel
import im.vector.app.features.onboarding.OnboardingViewState
import im.vector.app.features.onboarding.ftueauth.terms.FtueAuthTermsFragment
import im.vector.app.features.onboarding.ftueauth.terms.FtueAuthTermsFragmentArgument
import org.matrix.android.sdk.api.auth.registration.FlowResult
import org.matrix.android.sdk.api.auth.registration.Stage
import org.matrix.android.sdk.api.extensions.tryOrNull

private const val FRAGMENT_REGISTRATION_STAGE_TAG = "FRAGMENT_REGISTRATION_STAGE_TAG"
private const val FRAGMENT_LOGIN_TAG = "FRAGMENT_LOGIN_TAG"

class FtueAuthVariant(
        private val views: ActivityLoginBinding,
        private val onboardingViewModel: OnboardingViewModel,
        private val activity: VectorBaseActivity<ActivityLoginBinding>,
        private val supportFragmentManager: FragmentManager,
        private val vectorFeatures: VectorFeatures,
        private val orientationLocker: ScreenOrientationLocker,
) : OnboardingVariant {

    private val enterAnim = R.anim.enter_fade_in
    private val exitAnim = R.anim.exit_fade_out

    private val popEnterAnim = R.anim.no_anim
    private val popExitAnim = R.anim.exit_fade_out

    private var isForceLoginFallbackEnabled = false

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

    override fun initUiAndData(isFirstCreation: Boolean) {
        if (isFirstCreation) {
            addFirstFragment()
        }

        with(activity) {
            orientationLocker.lockPhonesToPortrait(this)
            onboardingViewModel.onEach {
                updateWithState(it)
            }
            onboardingViewModel.observeViewEvents { handleOnboardingViewEvents(it) }
        }

        // Get config extra
        val loginConfig = activity.intent.getParcelableExtra<LoginConfig?>(OnboardingActivity.EXTRA_CONFIG)
        if (isFirstCreation) {
            onboardingViewModel.handle(OnboardingAction.InitWith(loginConfig))
        }
    }

    private fun addFirstFragment() {
        val splashFragment = when (vectorFeatures.isOnboardingSplashCarouselEnabled()) {
            true -> FtueAuthSplashCarouselFragment::class.java
            else -> FtueAuthSplashFragment::class.java
        }
        activity.addFragment(views.loginFragmentContainer, splashFragment)
    }

    private fun updateWithState(viewState: OnboardingViewState) {
        isForceLoginFallbackEnabled = viewState.isForceLoginFallbackEnabled
        views.loginLoading.isVisible = viewState.isLoading
    }

    override fun setIsLoading(isLoading: Boolean) = Unit

    private fun handleOnboardingViewEvents(viewEvents: OnboardingViewEvents) {
        when (viewEvents) {
            is OnboardingViewEvents.RegistrationFlowResult                     -> {
                if (registrationShouldFallback(viewEvents)) {
                    // Display a popup to propose use web fallback
                    onRegistrationStageNotSupported()
                } else {
                    if (viewEvents.isRegistrationStarted) {
                        // Go on with registration flow
                        handleRegistrationNavigation(viewEvents.flowResult)
                    } else {
                        if (vectorFeatures.isOnboardingCombinedRegisterEnabled()) {
                            openCombinedRegister()
                        } else {
                            // First ask for login and password
                            // I add a tag to indicate that this fragment is a registration stage.
                            // This way it will be automatically popped in when starting the next registration stage
                            openAuthLoginFragmentWithTag(FRAGMENT_REGISTRATION_STAGE_TAG)
                        }
                    }
                }
            }
            is OnboardingViewEvents.OutdatedHomeserver                         -> {
                MaterialAlertDialogBuilder(activity)
                        .setTitle(R.string.login_error_outdated_homeserver_title)
                        .setMessage(R.string.login_error_outdated_homeserver_warning_content)
                        .setPositiveButton(R.string.ok, null)
                        .show()
                Unit
            }
            is OnboardingViewEvents.OpenServerSelection                        ->
                activity.addFragmentToBackstack(views.loginFragmentContainer,
                        FtueAuthServerSelectionFragment::class.java,
                        option = { ft ->
                            if (vectorFeatures.isOnboardingUseCaseEnabled()) {
                                ft.setCustomAnimations(enterAnim, exitAnim, popEnterAnim, popExitAnim)
                            } else {
                                activity.findViewById<View?>(R.id.loginSplashLogo)?.let { ft.addSharedElement(it, ViewCompat.getTransitionName(it) ?: "") }
                                // TODO Disabled because it provokes a flickering
                                // Disable transition of text
                                // findViewById<View?>(R.id.loginSplashTitle)?.let { ft.addSharedElement(it, ViewCompat.getTransitionName(it) ?: "") }
                                // No transition here now actually
                                // findViewById<View?>(R.id.loginSplashSubmit)?.let { ft.addSharedElement(it, ViewCompat.getTransitionName(it) ?: "") }
                            }
                        })
            is OnboardingViewEvents.OnServerSelectionDone                      -> onServerSelectionDone(viewEvents)
            is OnboardingViewEvents.OnSignModeSelected                         -> onSignModeSelected(viewEvents)
            is OnboardingViewEvents.OnLoginFlowRetrieved                       ->
                activity.addFragmentToBackstack(views.loginFragmentContainer,
                        FtueAuthSignUpSignInSelectionFragment::class.java,
                        option = commonOption)
            is OnboardingViewEvents.OnWebLoginError                            -> onWebLoginError(viewEvents)
            is OnboardingViewEvents.OnForgetPasswordClicked                    ->
                activity.addFragmentToBackstack(views.loginFragmentContainer,
                        FtueAuthResetPasswordFragment::class.java,
                        option = commonOption)
            is OnboardingViewEvents.OnResetPasswordSendThreePidDone            -> {
                supportFragmentManager.popBackStack(FRAGMENT_LOGIN_TAG, POP_BACK_STACK_EXCLUSIVE)
                activity.addFragmentToBackstack(views.loginFragmentContainer,
                        FtueAuthResetPasswordMailConfirmationFragment::class.java,
                        option = commonOption)
            }
            is OnboardingViewEvents.OnResetPasswordMailConfirmationSuccess     -> {
                supportFragmentManager.popBackStack(FRAGMENT_LOGIN_TAG, POP_BACK_STACK_EXCLUSIVE)
                activity.addFragmentToBackstack(views.loginFragmentContainer,
                        FtueAuthResetPasswordSuccessFragment::class.java,
                        option = commonOption)
            }
            is OnboardingViewEvents.OnResetPasswordMailConfirmationSuccessDone -> {
                // Go back to the login fragment
                supportFragmentManager.popBackStack(FRAGMENT_LOGIN_TAG, POP_BACK_STACK_EXCLUSIVE)
            }
            is OnboardingViewEvents.OnSendEmailSuccess                         -> {
                // Pop the enter email Fragment
                supportFragmentManager.popBackStack(FRAGMENT_REGISTRATION_STAGE_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                activity.addFragmentToBackstack(views.loginFragmentContainer,
                        FtueAuthWaitForEmailFragment::class.java,
                        FtueAuthWaitForEmailFragmentArgument(viewEvents.email),
                        tag = FRAGMENT_REGISTRATION_STAGE_TAG,
                        option = commonOption)
            }
            is OnboardingViewEvents.OnSendMsisdnSuccess                        -> {
                // Pop the enter Msisdn Fragment
                supportFragmentManager.popBackStack(FRAGMENT_REGISTRATION_STAGE_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                activity.addFragmentToBackstack(views.loginFragmentContainer,
                        FtueAuthGenericTextInputFormFragment::class.java,
                        FtueAuthGenericTextInputFormFragmentArgument(TextInputFormFragmentMode.ConfirmMsisdn, true, viewEvents.msisdn),
                        tag = FRAGMENT_REGISTRATION_STAGE_TAG,
                        option = commonOption)
            }
            is OnboardingViewEvents.Failure,
            is OnboardingViewEvents.Loading                                    ->
                // This is handled by the Fragments
                Unit
            OnboardingViewEvents.OpenUseCaseSelection                          -> {
                activity.addFragmentToBackstack(views.loginFragmentContainer,
                        FtueAuthUseCaseFragment::class.java,
                        option = commonOption)
            }
            OnboardingViewEvents.OpenCombinedRegister                          -> openCombinedRegister()
            is OnboardingViewEvents.OnAccountCreated                           -> onAccountCreated()
            OnboardingViewEvents.OnAccountSignedIn                             -> onAccountSignedIn()
            OnboardingViewEvents.OnChooseDisplayName                           -> onChooseDisplayName()
            OnboardingViewEvents.OnTakeMeHome                                  -> navigateToHome(createdAccount = true)
            OnboardingViewEvents.OnChooseProfilePicture                        -> onChooseProfilePicture()
            OnboardingViewEvents.OnPersonalizationComplete                     -> onPersonalizationComplete()
            OnboardingViewEvents.OnBack                                        -> activity.popBackstack()
            OnboardingViewEvents.EditServerSelection                           -> {
                activity.addFragmentToBackstack(
                        views.loginFragmentContainer,
                        FtueAuthCombinedServerSelectionFragment::class.java,
                        option = commonOption
                )
            }
            OnboardingViewEvents.OnHomeserverEdited                            -> activity.popBackstack()
        }
    }

    private fun openCombinedRegister() {
        activity.addFragmentToBackstack(
                views.loginFragmentContainer,
                FtueAuthCombinedRegisterFragment::class.java,
                tag = FRAGMENT_REGISTRATION_STAGE_TAG,
                option = commonOption
        )
    }

    private fun registrationShouldFallback(registrationFlowResult: OnboardingViewEvents.RegistrationFlowResult) =
            isForceLoginFallbackEnabled || registrationFlowResult.containsUnsupportedRegistrationFlow()

    private fun OnboardingViewEvents.RegistrationFlowResult.containsUnsupportedRegistrationFlow() =
            flowResult.missingStages.any { !it.isSupported() }

    private fun onRegistrationStageNotSupported() {
        MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.app_name)
                .setMessage(activity.getString(R.string.login_registration_not_supported))
                .setPositiveButton(R.string.yes) { _, _ ->
                    activity.addFragmentToBackstack(views.loginFragmentContainer,
                            FtueAuthWebFragment::class.java,
                            option = commonOption)
                }
                .setNegativeButton(R.string.no, null)
                .show()
    }

    private fun onWebLoginError(onWebLoginError: OnboardingViewEvents.OnWebLoginError) {
        // Pop the backstack
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)

        // And inform the user
        MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.dialog_title_error)
                .setMessage(activity.getString(R.string.login_sso_error_message, onWebLoginError.description, onWebLoginError.errorCode))
                .setPositiveButton(R.string.ok, null)
                .show()
    }

    private fun onServerSelectionDone(OnboardingViewEvents: OnboardingViewEvents.OnServerSelectionDone) {
        when (OnboardingViewEvents.serverType) {
            ServerType.MatrixOrg -> Unit // In this case, we wait for the login flow
            ServerType.EMS,
            ServerType.Other     -> activity.addFragmentToBackstack(views.loginFragmentContainer,
                    FtueAuthServerUrlFormFragment::class.java,
                    option = commonOption)
            ServerType.Unknown   -> Unit /* Should not happen */
        }
    }

    private fun onSignModeSelected(OnboardingViewEvents: OnboardingViewEvents.OnSignModeSelected) = withState(onboardingViewModel) { state ->
        // state.signMode could not be ready yet. So use value from the ViewEvent
        when (OnboardingViewEvents.signMode) {
            SignMode.Unknown            -> error("Sign mode has to be set before calling this method")
            SignMode.SignUp             -> Unit // This case is processed in handleOnboardingViewEvents
            SignMode.SignIn             -> handleSignInSelected(state)
            SignMode.SignInWithMatrixId -> handleSignInWithMatrixId(state)
        }
    }

    private fun handleSignInSelected(state: OnboardingViewState) {
        if (isForceLoginFallbackEnabled) {
            onLoginModeNotSupported(state.selectedHomeserver.supportedLoginTypes)
        } else {
            disambiguateLoginMode(state)
        }
    }

    private fun disambiguateLoginMode(state: OnboardingViewState) = when (state.selectedHomeserver.preferredLoginMode) {
        LoginMode.Unknown,
        is LoginMode.Sso      -> error("Developer error")
        is LoginMode.SsoAndPassword,
        LoginMode.Password    -> openAuthLoginFragmentWithTag(FRAGMENT_LOGIN_TAG)
        LoginMode.Unsupported -> onLoginModeNotSupported(state.selectedHomeserver.supportedLoginTypes)
    }

    private fun openAuthLoginFragmentWithTag(tag: String) {
        activity.addFragmentToBackstack(views.loginFragmentContainer,
                FtueAuthLoginFragment::class.java,
                tag = tag,
                option = commonOption)
    }

    private fun onLoginModeNotSupported(supportedTypes: List<String>) {
        MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.app_name)
                .setMessage(activity.getString(R.string.login_mode_not_supported, supportedTypes.joinToString { "'$it'" }))
                .setPositiveButton(R.string.yes) { _, _ -> openAuthWebFragment() }
                .setNegativeButton(R.string.no, null)
                .show()
    }

    private fun handleSignInWithMatrixId(state: OnboardingViewState) {
        if (isForceLoginFallbackEnabled) {
            onLoginModeNotSupported(state.selectedHomeserver.supportedLoginTypes)
        } else {
            openAuthLoginFragmentWithTag(FRAGMENT_LOGIN_TAG)
        }
    }

    private fun openAuthWebFragment() {
        activity.addFragmentToBackstack(views.loginFragmentContainer,
                FtueAuthWebFragment::class.java,
                option = commonOption)
    }

    /**
     * Handle the SSO redirection here
     */
    override fun onNewIntent(intent: Intent?) {
        intent?.data
                ?.let { tryOrNull { it.getQueryParameter("loginToken") } }
                ?.let { onboardingViewModel.handle(OnboardingAction.LoginWithToken(it)) }
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
                    FtueAuthCaptchaFragment::class.java,
                    FtueAuthCaptchaFragmentArgument(stage.publicKey),
                    tag = FRAGMENT_REGISTRATION_STAGE_TAG,
                    option = commonOption)
            is Stage.Email     -> activity.addFragmentToBackstack(views.loginFragmentContainer,
                    FtueAuthGenericTextInputFormFragment::class.java,
                    FtueAuthGenericTextInputFormFragmentArgument(TextInputFormFragmentMode.SetEmail, stage.mandatory),
                    tag = FRAGMENT_REGISTRATION_STAGE_TAG,
                    option = commonOption)
            is Stage.Msisdn    -> activity.addFragmentToBackstack(views.loginFragmentContainer,
                    FtueAuthGenericTextInputFormFragment::class.java,
                    FtueAuthGenericTextInputFormFragmentArgument(TextInputFormFragmentMode.SetMsisdn, stage.mandatory),
                    tag = FRAGMENT_REGISTRATION_STAGE_TAG,
                    option = commonOption)
            is Stage.Terms     -> activity.addFragmentToBackstack(views.loginFragmentContainer,
                    FtueAuthTermsFragment::class.java,
                    FtueAuthTermsFragmentArgument(stage.policies.toLocalizedLoginTerms(activity.getString(R.string.resources_language))),
                    tag = FRAGMENT_REGISTRATION_STAGE_TAG,
                    option = commonOption)
            else               -> Unit // Should not happen
        }
    }

    private fun onAccountSignedIn() {
        navigateToHome(createdAccount = false)
    }

    private fun onAccountCreated() {
        activity.supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        activity.replaceFragment(
                views.loginFragmentContainer,
                FtueAuthAccountCreatedFragment::class.java,
                useCustomAnimation = true
        )
    }

    private fun navigateToHome(createdAccount: Boolean) {
        val intent = HomeActivity.newIntent(activity, accountCreation = createdAccount)
        activity.startActivity(intent)
        activity.finish()
    }

    private fun onChooseDisplayName() {
        activity.addFragmentToBackstack(views.loginFragmentContainer,
                FtueAuthChooseDisplayNameFragment::class.java,
                option = commonOption
        )
    }

    private fun onChooseProfilePicture() {
        activity.addFragmentToBackstack(views.loginFragmentContainer,
                FtueAuthChooseProfilePictureFragment::class.java,
                option = commonOption
        )
    }

    private fun onPersonalizationComplete() {
        activity.supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        activity.replaceFragment(
                views.loginFragmentContainer,
                FtueAuthPersonalizationCompleteFragment::class.java,
                useCustomAnimation = true
        )
    }
}
