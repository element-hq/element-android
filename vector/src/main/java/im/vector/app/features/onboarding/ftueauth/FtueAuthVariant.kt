/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.onboarding.ftueauth

import android.content.Intent
import android.os.Parcelable
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.app.R
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.extensions.addFragmentToBackstack
import im.vector.app.core.extensions.popBackstack
import im.vector.app.core.extensions.replaceFragment
import im.vector.app.core.platform.ScreenOrientationLocker
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.core.resources.BuildMeta
import im.vector.app.databinding.ActivityLoginBinding
import im.vector.app.features.VectorFeatures
import im.vector.app.features.home.HomeActivity
import im.vector.app.features.login.LoginConfig
import im.vector.app.features.login.LoginMode
import im.vector.app.features.login.ServerType
import im.vector.app.features.login.SignMode
import im.vector.app.features.login.TextInputFormFragmentMode
import im.vector.app.features.onboarding.OnboardingAction
import im.vector.app.features.onboarding.OnboardingActivity
import im.vector.app.features.onboarding.OnboardingFlow
import im.vector.app.features.onboarding.OnboardingVariant
import im.vector.app.features.onboarding.OnboardingViewEvents
import im.vector.app.features.onboarding.OnboardingViewModel
import im.vector.app.features.onboarding.OnboardingViewState
import im.vector.app.features.onboarding.ftueauth.terms.FtueAuthLegacyStyleTermsFragment
import im.vector.app.features.onboarding.ftueauth.terms.FtueAuthTermsFragment
import im.vector.app.features.onboarding.ftueauth.terms.FtueAuthTermsLegacyStyleFragmentArgument
import im.vector.lib.core.utils.compat.getParcelableExtraCompat
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.auth.registration.Stage
import org.matrix.android.sdk.api.auth.toLocalizedLoginTerms
import org.matrix.android.sdk.api.extensions.tryOrNull

private const val FRAGMENT_REGISTRATION_STAGE_TAG = "FRAGMENT_REGISTRATION_STAGE_TAG"
private const val FRAGMENT_LOGIN_TAG = "FRAGMENT_LOGIN_TAG"
private const val FRAGMENT_EDIT_HOMESERVER_TAG = "FRAGMENT_EDIT_HOMESERVER"

class FtueAuthVariant(
        private val views: ActivityLoginBinding,
        private val onboardingViewModel: OnboardingViewModel,
        private val activity: VectorBaseActivity<ActivityLoginBinding>,
        private val supportFragmentManager: FragmentManager,
        private val vectorFeatures: VectorFeatures,
        private val orientationLocker: ScreenOrientationLocker,
        private val buildMeta: BuildMeta,
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
                ?.firstOrNull { it.id == im.vector.lib.ui.styles.R.id.loginLogo }
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
        val loginConfig = activity.intent.getParcelableExtraCompat<LoginConfig?>(OnboardingActivity.EXTRA_CONFIG)
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
            is OnboardingViewEvents.OutdatedHomeserver -> {
                MaterialAlertDialogBuilder(activity)
                        .setTitle(CommonStrings.login_error_outdated_homeserver_title)
                        .setMessage(CommonStrings.login_error_outdated_homeserver_warning_content)
                        .setPositiveButton(CommonStrings.ok, null)
                        .show()
                Unit
            }
            is OnboardingViewEvents.OpenServerSelection ->
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
            is OnboardingViewEvents.OnServerSelectionDone -> onServerSelectionDone(viewEvents)
            is OnboardingViewEvents.OnSignModeSelected -> onSignModeSelected(viewEvents)
            is OnboardingViewEvents.OnLoginFlowRetrieved ->
                activity.addFragmentToBackstack(
                        views.loginFragmentContainer,
                        FtueAuthSignUpSignInSelectionFragment::class.java,
                        option = commonOption
                )
            is OnboardingViewEvents.OnWebLoginError -> onWebLoginError(viewEvents)
            is OnboardingViewEvents.OnForgetPasswordClicked ->
                when {
                    vectorFeatures.isOnboardingCombinedLoginEnabled() -> addLoginStageFragmentToBackstack(FtueAuthResetPasswordEmailEntryFragment::class.java)
                    else -> addLoginStageFragmentToBackstack(FtueAuthResetPasswordFragment::class.java)
                }
            is OnboardingViewEvents.OnResetPasswordEmailConfirmationSent -> {
                supportFragmentManager.popBackStack(FRAGMENT_LOGIN_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                when {
                    vectorFeatures.isOnboardingCombinedLoginEnabled() -> addLoginStageFragmentToBackstack(
                            FtueAuthResetPasswordBreakerFragment::class.java,
                            FtueAuthResetPasswordBreakerArgument(viewEvents.email),
                    )
                    else -> activity.addFragmentToBackstack(
                            views.loginFragmentContainer,
                            FtueAuthResetPasswordMailConfirmationFragment::class.java,
                    )
                }
            }
            OnboardingViewEvents.OnResetPasswordBreakerConfirmed -> {
                supportFragmentManager.popBackStack(FRAGMENT_LOGIN_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                activity.addFragmentToBackstack(
                        views.loginFragmentContainer,
                        FtueAuthResetPasswordEntryFragment::class.java,
                        option = commonOption
                )
            }
            is OnboardingViewEvents.OpenResetPasswordComplete -> {
                supportFragmentManager.popBackStack(FRAGMENT_LOGIN_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                addLoginStageFragmentToBackstack(FtueAuthResetPasswordSuccessFragment::class.java)
            }
            OnboardingViewEvents.OnResetPasswordComplete -> {
                Toast.makeText(activity, CommonStrings.ftue_auth_password_reset_confirmation, Toast.LENGTH_SHORT).show()
                activity.popBackstack()
            }
            is OnboardingViewEvents.OnSendEmailSuccess -> {
                openWaitForEmailVerification(viewEvents.email, viewEvents.isRestoredSession)
            }
            is OnboardingViewEvents.OnSendMsisdnSuccess -> {
                openMsisdnConfirmation(viewEvents.msisdn)
            }
            is OnboardingViewEvents.Failure,
            is OnboardingViewEvents.UnrecognisedCertificateFailure,
            is OnboardingViewEvents.Loading ->
                // This is handled by the Fragments
                Unit
            OnboardingViewEvents.OpenUseCaseSelection -> {
                activity.addFragmentToBackstack(
                        views.loginFragmentContainer,
                        FtueAuthUseCaseFragment::class.java,
                        option = commonOption
                )
            }
            OnboardingViewEvents.OpenCombinedRegister -> onStartCombinedRegister()
            is OnboardingViewEvents.OnAccountCreated -> onAccountCreated()
            OnboardingViewEvents.OnAccountSignedIn -> onAccountSignedIn()
            OnboardingViewEvents.OnChooseDisplayName -> onChooseDisplayName()
            OnboardingViewEvents.OnTakeMeHome -> navigateToHome()
            OnboardingViewEvents.OnChooseProfilePicture -> onChooseProfilePicture()
            OnboardingViewEvents.OnPersonalizationComplete -> onPersonalizationComplete()
            OnboardingViewEvents.OnBack -> activity.popBackstack()
            OnboardingViewEvents.EditServerSelection -> {
                activity.addFragmentToBackstack(
                        views.loginFragmentContainer,
                        FtueAuthCombinedServerSelectionFragment::class.java,
                        option = commonOption,
                        tag = FRAGMENT_EDIT_HOMESERVER_TAG
                )
            }
            OnboardingViewEvents.OnHomeserverEdited -> {
                supportFragmentManager.popBackStack(
                        FRAGMENT_EDIT_HOMESERVER_TAG,
                        FragmentManager.POP_BACK_STACK_INCLUSIVE
                )
                ensureEditServerBackstack()
            }
            OnboardingViewEvents.OpenCombinedLogin -> onStartCombinedLogin()
            OnboardingViewEvents.DisplayRegistrationFallback -> displayFallbackWebDialog()
            is OnboardingViewEvents.DisplayRegistrationStage -> doStage(viewEvents.stage)
            OnboardingViewEvents.DisplayStartRegistration -> when {
                vectorFeatures.isOnboardingCombinedRegisterEnabled() -> onStartCombinedRegister()
                else -> openAuthLoginFragmentWithTag(FRAGMENT_REGISTRATION_STAGE_TAG)
            }
        }
    }

    private fun ensureEditServerBackstack() {
        when (activity.supportFragmentManager.findFragmentById(views.loginFragmentContainer.id)) {
            is FtueAuthCombinedLoginFragment,
            is FtueAuthCombinedRegisterFragment -> {
                // do nothing
            }
            else -> {
                withState(onboardingViewModel) { state ->
                    when (state.onboardingFlow) {
                        OnboardingFlow.SignIn -> onStartCombinedLogin()
                        OnboardingFlow.SignUp -> onStartCombinedRegister()
                        OnboardingFlow.SignInSignUp,
                        null -> error("${state.onboardingFlow} does not support editing server url")
                    }
                }
            }
        }
    }

    private fun onStartCombinedLogin() {
        addRegistrationStageFragmentToBackstack(FtueAuthCombinedLoginFragment::class.java, allowStateLoss = true)
    }

    private fun onStartCombinedRegister() {
        addRegistrationStageFragmentToBackstack(FtueAuthCombinedRegisterFragment::class.java, allowStateLoss = true)
    }

    private fun displayFallbackWebDialog() {
        MaterialAlertDialogBuilder(activity)
                .setTitle(buildMeta.applicationName)
                .setMessage(activity.getString(CommonStrings.login_registration_not_supported))
                .setPositiveButton(CommonStrings.yes) { _, _ ->
                    activity.addFragmentToBackstack(
                            views.loginFragmentContainer,
                            FtueAuthWebFragment::class.java,
                            option = commonOption
                    )
                }
                .setNegativeButton(CommonStrings.no, null)
                .show()
    }

    private fun onWebLoginError(onWebLoginError: OnboardingViewEvents.OnWebLoginError) {
        // Pop the backstack
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)

        // And inform the user
        MaterialAlertDialogBuilder(activity)
                .setTitle(CommonStrings.dialog_title_error)
                .setMessage(activity.getString(CommonStrings.login_sso_error_message, onWebLoginError.description, onWebLoginError.errorCode))
                .setPositiveButton(CommonStrings.ok, null)
                .show()
    }

    private fun onServerSelectionDone(onboardingViewEvents: OnboardingViewEvents.OnServerSelectionDone) {
        when (onboardingViewEvents.serverType) {
            ServerType.MatrixOrg -> Unit // In this case, we wait for the login flow
            ServerType.EMS,
            ServerType.Other -> activity.addFragmentToBackstack(
                    views.loginFragmentContainer,
                    FtueAuthServerUrlFormFragment::class.java,
                    option = commonOption
            )
            ServerType.Unknown -> Unit /* Should not happen */
        }
    }

    private fun onSignModeSelected(onboardingViewEvents: OnboardingViewEvents.OnSignModeSelected) = withState(onboardingViewModel) { state ->
        // state.signMode could not be ready yet. So use value from the ViewEvent
        when (onboardingViewEvents.signMode) {
            SignMode.Unknown -> error("Sign mode has to be set before calling this method")
            SignMode.SignUp -> Unit // This case is processed in handleOnboardingViewEvents
            SignMode.SignIn -> handleSignInSelected(state)
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
        is LoginMode.Sso -> error("Developer error")
        is LoginMode.SsoAndPassword,
        LoginMode.Password -> openAuthLoginFragmentWithTag(FRAGMENT_LOGIN_TAG)
        LoginMode.Unsupported -> onLoginModeNotSupported(state.selectedHomeserver.supportedLoginTypes)
    }

    private fun openAuthLoginFragmentWithTag(tag: String) {
        activity.addFragmentToBackstack(
                views.loginFragmentContainer,
                FtueAuthLoginFragment::class.java,
                tag = tag,
                option = commonOption
        )
    }

    private fun onLoginModeNotSupported(supportedTypes: List<String>) {
        MaterialAlertDialogBuilder(activity)
                .setTitle(buildMeta.applicationName)
                .setMessage(activity.getString(CommonStrings.login_mode_not_supported, supportedTypes.joinToString { "'$it'" }))
                .setPositiveButton(CommonStrings.yes) { _, _ -> openAuthWebFragment() }
                .setNegativeButton(CommonStrings.no, null)
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
        activity.addFragmentToBackstack(
                views.loginFragmentContainer,
                FtueAuthWebFragment::class.java,
                option = commonOption
        )
    }

    /**
     * Handle the SSO redirection here.
     */
    override fun onNewIntent(intent: Intent?) {
        intent?.data
                ?.let { tryOrNull { it.getQueryParameter("loginToken") } }
                ?.let { onboardingViewModel.handle(OnboardingAction.LoginWithToken(it)) }
    }

    private fun doStage(stage: Stage) {
        // Ensure there is no fragment for registration stage in the backstack
        supportFragmentManager.popBackStack(FRAGMENT_REGISTRATION_STAGE_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE)

        when (stage) {
            is Stage.ReCaptcha -> onCaptcha(stage)
            is Stage.Email -> onEmail(stage)
            is Stage.Msisdn -> onMsisdn(stage)
            is Stage.Terms -> onTerms(stage)
            else -> Unit // Should not happen
        }
    }

    private fun onMsisdn(stage: Stage) {
        when {
            vectorFeatures.isOnboardingCombinedRegisterEnabled() -> addRegistrationStageFragmentToBackstack(
                    FtueAuthPhoneEntryFragment::class.java
            )
            else -> addRegistrationStageFragmentToBackstack(
                    FtueAuthGenericTextInputFormFragment::class.java,
                    FtueAuthGenericTextInputFormFragmentArgument(TextInputFormFragmentMode.SetMsisdn, stage.mandatory),
            )
        }
    }

    private fun onEmail(stage: Stage) {
        when {
            vectorFeatures.isOnboardingCombinedRegisterEnabled() -> addRegistrationStageFragmentToBackstack(
                    FtueAuthEmailEntryFragment::class.java,
                    FtueAuthEmailEntryFragmentArgument(mandatory = stage.mandatory)
            )
            else -> addRegistrationStageFragmentToBackstack(
                    FtueAuthGenericTextInputFormFragment::class.java,
                    FtueAuthGenericTextInputFormFragmentArgument(TextInputFormFragmentMode.SetEmail, stage.mandatory),
            )
        }
    }

    private fun openWaitForEmailVerification(email: String, isRestoredSession: Boolean) {
        when {
            vectorFeatures.isOnboardingCombinedRegisterEnabled() -> addRegistrationStageFragmentToBackstack(
                    FtueAuthWaitForEmailFragment::class.java,
                    FtueAuthWaitForEmailFragmentArgument(email, isRestoredSession),
            )
            else -> {
                supportFragmentManager.popBackStack(FRAGMENT_REGISTRATION_STAGE_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                addRegistrationStageFragmentToBackstack(
                        FtueAuthLegacyWaitForEmailFragment::class.java,
                        FtueAuthWaitForEmailFragmentArgument(email, isRestoredSession),
                )
            }
        }
    }

    private fun openMsisdnConfirmation(msisdn: String) {
        supportFragmentManager.popBackStack(FRAGMENT_REGISTRATION_STAGE_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        when {
            vectorFeatures.isOnboardingCombinedRegisterEnabled() -> addRegistrationStageFragmentToBackstack(
                    FtueAuthPhoneConfirmationFragment::class.java,
                    FtueAuthPhoneConfirmationFragmentArgument(msisdn),
            )
            else -> addRegistrationStageFragmentToBackstack(
                    FtueAuthGenericTextInputFormFragment::class.java,
                    FtueAuthGenericTextInputFormFragmentArgument(TextInputFormFragmentMode.ConfirmMsisdn, true, msisdn),
            )
        }
    }

    private fun onTerms(stage: Stage.Terms) {
        when {
            vectorFeatures.isOnboardingCombinedRegisterEnabled() -> addRegistrationStageFragmentToBackstack(
                    FtueAuthTermsFragment::class.java,
                    FtueAuthTermsLegacyStyleFragmentArgument(stage.policies.toLocalizedLoginTerms(activity.getString(CommonStrings.resources_language))),
            )
            else -> addRegistrationStageFragmentToBackstack(
                    FtueAuthLegacyStyleTermsFragment::class.java,
                    FtueAuthTermsLegacyStyleFragmentArgument(stage.policies.toLocalizedLoginTerms(activity.getString(CommonStrings.resources_language))),
            )
        }
    }

    private fun onCaptcha(stage: Stage.ReCaptcha) {
        when {
            vectorFeatures.isOnboardingCombinedRegisterEnabled() -> addRegistrationStageFragmentToBackstack(
                    FtueAuthCaptchaFragment::class.java,
                    FtueAuthCaptchaFragmentArgument(stage.publicKey),
            )
            else -> addRegistrationStageFragmentToBackstack(
                    FtueAuthLegacyStyleCaptchaFragment::class.java,
                    FtueAuthLegacyStyleCaptchaFragmentArgument(stage.publicKey),
            )
        }
    }

    private fun onAccountSignedIn() {
        navigateToHome()
    }

    private fun onAccountCreated() {
        activity.supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        activity.replaceFragment(
                views.loginFragmentContainer,
                FtueAuthAccountCreatedFragment::class.java,
                useCustomAnimation = true
        )
    }

    private fun navigateToHome() {
        withState(onboardingViewModel) {
            val intent = HomeActivity.newIntent(
                    activity,
                    firstStartMainActivity = false,
                    authenticationDescription = it.selectedAuthenticationState.description
            )
            activity.startActivity(intent)
            activity.finish()
        }
    }

    private fun onChooseDisplayName() {
        activity.addFragmentToBackstack(
                views.loginFragmentContainer,
                FtueAuthChooseDisplayNameFragment::class.java,
                option = commonOption
        )
    }

    private fun onChooseProfilePicture() {
        activity.addFragmentToBackstack(
                views.loginFragmentContainer,
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

    private fun addRegistrationStageFragmentToBackstack(fragmentClass: Class<out Fragment>, params: Parcelable? = null, allowStateLoss: Boolean = false) {
        activity.addFragmentToBackstack(
                views.loginFragmentContainer,
                fragmentClass,
                params,
                tag = FRAGMENT_REGISTRATION_STAGE_TAG,
                option = commonOption,
                allowStateLoss = allowStateLoss,
        )
    }

    private fun addLoginStageFragmentToBackstack(fragmentClass: Class<out Fragment>, params: Parcelable? = null) {
        activity.addFragmentToBackstack(
                views.loginFragmentContainer,
                fragmentClass,
                params,
                tag = FRAGMENT_LOGIN_TAG,
                option = commonOption
        )
    }
}
