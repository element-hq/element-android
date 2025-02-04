/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.signout.soft

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.cleanup
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.extensions.hideKeyboard
import im.vector.app.databinding.FragmentGenericRecyclerBinding
import im.vector.app.features.login.AbstractLoginFragment
import im.vector.app.features.login.LoginAction
import im.vector.app.features.login.LoginMode
import im.vector.app.features.login.LoginViewEvents
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.extensions.orFalse
import javax.inject.Inject

/**
 * In this screen:
 * - the user is asked to enter a password to sign in again to a homeserver.
 * - or to cleanup all the data
 */
@AndroidEntryPoint
class SoftLogoutFragment :
        AbstractLoginFragment<FragmentGenericRecyclerBinding>(),
        SoftLogoutController.Listener {

    @Inject lateinit var softLogoutController: SoftLogoutController

    private val softLogoutViewModel: SoftLogoutViewModel by activityViewModel()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentGenericRecyclerBinding {
        return FragmentGenericRecyclerBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()

        softLogoutViewModel.onEach { softLogoutViewState ->
            softLogoutController.update(softLogoutViewState)
            when (val mode = softLogoutViewState.asyncHomeServerLoginFlowRequest.invoke()) {
                is LoginMode.SsoAndPassword -> {
                    loginViewModel.handle(
                            LoginAction.SetupSsoForSessionRecovery(
                                    softLogoutViewState.homeServerUrl,
                                    softLogoutViewState.deviceId,
                                    mode.ssoState.providersOrNull(),
                                    mode.hasOidcCompatibilityFlow
                            )
                    )
                }
                is LoginMode.Sso -> {
                    loginViewModel.handle(
                            LoginAction.SetupSsoForSessionRecovery(
                                    softLogoutViewState.homeServerUrl,
                                    softLogoutViewState.deviceId,
                                    mode.ssoState.providersOrNull(),
                                    mode.hasOidcCompatibilityFlow
                            )
                    )
                }
                LoginMode.Unsupported -> {
                    // Prepare the loginViewModel for a SSO/login fallback recovery
                    loginViewModel.handle(
                            LoginAction.SetupSsoForSessionRecovery(
                                    softLogoutViewState.homeServerUrl,
                                    softLogoutViewState.deviceId,
                                    null,
                                    false
                            )
                    )
                }
                else -> Unit
            }
        }
    }

    private fun setupRecyclerView() {
        views.genericRecyclerView.configureWith(softLogoutController)
        softLogoutController.listener = this
    }

    override fun onDestroyView() {
        views.genericRecyclerView.cleanup()
        softLogoutController.listener = null
        super.onDestroyView()
    }

    override fun retry() {
        softLogoutViewModel.handle(SoftLogoutAction.RetryLoginFlow)
    }

    override fun passwordEdited(password: String) {
        softLogoutViewModel.handle(SoftLogoutAction.PasswordChanged(password))
    }

    override fun submit() = withState(softLogoutViewModel) { state ->
        cleanupUi()
        softLogoutViewModel.handle(SoftLogoutAction.SignInAgain(state.enteredPassword))
    }

    override fun signinFallbackSubmit() = withState(loginViewModel) { state ->
        // The loginViewModel has been prepared for a SSO/login fallback recovery (above)
        loginViewModel.handle(LoginAction.PostViewEvent(LoginViewEvents.OnSignModeSelected(state.signMode)))
    }

    override fun clearData() {
        withState(softLogoutViewModel) { state ->
            cleanupUi()

            val messageResId = if (state.hasUnsavedKeys().orFalse()) {
                CommonStrings.soft_logout_clear_data_dialog_e2e_warning_content
            } else {
                CommonStrings.soft_logout_clear_data_dialog_content
            }

            MaterialAlertDialogBuilder(requireActivity(), im.vector.lib.ui.styles.R.style.ThemeOverlay_Vector_MaterialAlertDialog_Destructive)
                    .setTitle(CommonStrings.soft_logout_clear_data_dialog_title)
                    .setMessage(messageResId)
                    .setNegativeButton(CommonStrings.action_cancel, null)
                    .setPositiveButton(CommonStrings.soft_logout_clear_data_submit) { _, _ ->
                        softLogoutViewModel.handle(SoftLogoutAction.ClearData)
                    }
                    .show()
        }
    }

    private fun cleanupUi() {
        views.genericRecyclerView.hideKeyboard()
    }

    override fun forgetPasswordClicked() {
        loginViewModel.handle(LoginAction.PostViewEvent(LoginViewEvents.OnForgetPasswordClicked))
    }

    override fun resetViewModel() {
        // No op
    }
}
