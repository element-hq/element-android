/*
 * Copyright 2021 New Vector Ltd
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

package fr.gouv.tchap.features.login

import android.content.Context
import android.content.Intent
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.airbnb.mvrx.viewModel
import fr.gouv.tchap.features.login.registration.TchapRegisterFragment
import im.vector.app.R
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.extensions.addFragmentToBackstack
import im.vector.app.core.extensions.exhaustive
import im.vector.app.core.platform.ToolbarConfigurable
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivityLoginBinding
import im.vector.app.features.home.HomeActivity
import im.vector.app.features.login.LoginCaptchaFragment
import im.vector.app.features.login.LoginCaptchaFragmentArgument
import im.vector.app.features.login.LoginConfig
import im.vector.app.features.login.LoginFragment
import im.vector.app.features.login.LoginGenericTextInputFormFragment
import im.vector.app.features.login.LoginGenericTextInputFormFragmentArgument
import im.vector.app.features.login.LoginResetPasswordFragment
import im.vector.app.features.login.LoginViewState
import im.vector.app.features.login.LoginWaitForEmailFragment
import im.vector.app.features.login.LoginWaitForEmailFragmentArgument
import im.vector.app.features.login.SignMode
import im.vector.app.features.login.TextInputFormFragmentMode
import im.vector.app.features.login.isSupported
import im.vector.app.features.login.terms.LoginTermsFragment
import im.vector.app.features.login.terms.LoginTermsFragmentArgument
import im.vector.app.features.login.terms.toLocalizedLoginTerms
import im.vector.app.features.pin.UnlockedActivity
import org.matrix.android.sdk.api.auth.registration.FlowResult
import org.matrix.android.sdk.api.auth.registration.Stage
import javax.inject.Inject

/**
 * The LoginActivity manages the fragment navigation and also display the loading View
 */
open class TchapLoginActivity : VectorBaseActivity<ActivityLoginBinding>(), ToolbarConfigurable, UnlockedActivity {

    private val tchapLoginViewModel: TchapLoginViewModel by viewModel()

    @Inject lateinit var tchapLoginViewModelFactory: TchapLoginViewModel.Factory

    @CallSuper
    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    private val enterAnim = R.anim.enter_fade_in
    private val exitAnim = R.anim.exit_fade_out

    private val popEnterAnim = R.anim.no_anim
    private val popExitAnim = R.anim.exit_fade_out

    private val topFragment: Fragment?
        get() = supportFragmentManager.findFragmentById(R.id.loginFragmentContainer)

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
        if (isFirstCreation()) {
            addFirstFragment()
        }

        tchapLoginViewModel
                .subscribe(this) {
                    updateWithState(it)
                }

        tchapLoginViewModel.observeViewEvents { handleLoginViewEvents(it) }

        // Get config extra
        val loginConfig = intent.getParcelableExtra<LoginConfig?>(EXTRA_CONFIG)
        if (isFirstCreation()) {
            // TODO Check this
            tchapLoginViewModel.handle(TchapLoginAction.InitWith(loginConfig))
        }
    }

    protected open fun addFirstFragment() {
        addFragment(R.id.loginFragmentContainer, TchapWelcomeFragment::class.java)
    }

    private fun handleLoginViewEvents(loginViewEvents: TchapLoginViewEvents) {
        when (loginViewEvents) {
            is TchapLoginViewEvents.RegistrationFlowResult                     -> {
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
                        addFragmentToBackstack(R.id.loginFragmentContainer,
                                LoginFragment::class.java,
                                tag = FRAGMENT_REGISTRATION_STAGE_TAG,
                                option = commonOption
                        )
                    }
                }
            }
            is TchapLoginViewEvents.OutdatedHomeserver                         -> {
                AlertDialog.Builder(this)
                        .setTitle(R.string.login_error_outdated_homeserver_title)
                        .setMessage(R.string.login_error_outdated_homeserver_warning_content)
                        .setPositiveButton(R.string.ok, null)
                        .show()
                Unit
            }
            is TchapLoginViewEvents.OnSignModeSelected                         -> onSignModeSelected(loginViewEvents)
            is TchapLoginViewEvents.OnLoginFlowRetrieved                       ->
                // Handled by the Tchap login fragment
                Unit
            is TchapLoginViewEvents.OnForgetPasswordClicked                    ->
                addFragmentToBackstack(R.id.loginFragmentContainer,
                        LoginResetPasswordFragment::class.java,
                        option = commonOption)
            is TchapLoginViewEvents.OnSendEmailSuccess                         ->
                addFragmentToBackstack(R.id.loginFragmentContainer,
                        LoginWaitForEmailFragment::class.java,
                        LoginWaitForEmailFragmentArgument(loginViewEvents.email),
                        tag = FRAGMENT_REGISTRATION_STAGE_TAG,
                        option = commonOption)
            is TchapLoginViewEvents.Failure,
            is TchapLoginViewEvents.Loading                                    ->
                // This is handled by the Fragments
                Unit
        }.exhaustive
    }

    private fun updateWithState(loginViewState: LoginViewState) {
        if (loginViewState.isUserLogged()) {
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

    private fun onSignModeSelected(loginViewEvents: TchapLoginViewEvents.OnSignModeSelected) {
        // state.signMode could not be ready yet. So use value from the ViewEvent
        when (loginViewEvents.signMode) {
            SignMode.Unknown            -> error("Sign mode has to be set before calling this method")
            SignMode.SignUp             -> addFragmentToBackstack(R.id.loginFragmentContainer,
                    TchapRegisterFragment::class.java,
                    tag = FRAGMENT_REGISTER_TAG,
                    option = commonOption)
            SignMode.SignIn             -> addFragmentToBackstack(R.id.loginFragmentContainer,
                    TchapLoginFragment::class.java,
                    tag = FRAGMENT_LOGIN_TAG,
                    option = commonOption)
            SignMode.SignInWithMatrixId -> Unit // Unsupported on Tchap
        }.exhaustive
    }

    private fun onRegistrationStageNotSupported() {
        AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(getString(R.string.login_registration_not_supported))
                .setPositiveButton(R.string.ok, null)
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
            is Stage.ReCaptcha -> Unit // Should not happen
            is Stage.Email     -> Unit //TODO: send email validation here
            is Stage.Msisdn    -> Unit // Should not happen
            is Stage.Terms     -> Unit // Should not happen
            else               -> Unit // Should not happen
        }
    }

    override fun configure(toolbar: Toolbar) {
        configureToolbar(toolbar)
    }

    companion object {
        private const val FRAGMENT_REGISTRATION_STAGE_TAG = "FRAGMENT_REGISTRATION_STAGE_TAG"
        private const val FRAGMENT_LOGIN_TAG = "FRAGMENT_LOGIN_TAG"
        private const val FRAGMENT_REGISTER_TAG = "FRAGMENT_REGISTER_TAG"

        private const val EXTRA_CONFIG = "EXTRA_CONFIG"

        // Note that the domain can be displayed to the user for confirmation that he trusts it. So use a human readable string
        const val VECTOR_REDIRECT_URL = "element://connect"

        fun newIntent(context: Context, loginConfig: LoginConfig?): Intent {
            return Intent(context, TchapLoginActivity::class.java).apply {
                putExtra(EXTRA_CONFIG, loginConfig)
            }
        }
    }
}
