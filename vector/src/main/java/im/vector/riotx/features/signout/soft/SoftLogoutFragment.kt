/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.riotx.features.signout.soft

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.withState
import im.vector.riotx.R
import im.vector.riotx.core.dialogs.withColoredButton
import im.vector.riotx.core.extensions.cleanup
import im.vector.riotx.core.extensions.configureWith
import im.vector.riotx.core.extensions.hideKeyboard
import im.vector.riotx.features.MainActivity
import im.vector.riotx.features.MainActivityArgs
import im.vector.riotx.features.login.AbstractLoginFragment
import im.vector.riotx.features.login.LoginNavigation
import im.vector.riotx.features.login.LoginViewState
import kotlinx.android.synthetic.main.fragment_generic_recycler.*
import javax.inject.Inject

/**
 * In this screen:
 * - the user is asked to enter a password to sign in again to a homeserver.
 * - or to cleanup all the data
 */
class SoftLogoutFragment @Inject constructor(
        private val softLogoutController: SoftLogoutController
) : AbstractLoginFragment(), SoftLogoutController.Listener {

    private val softLogoutViewModel: SoftLogoutViewModel by activityViewModel()

    override fun getLayoutResId() = R.layout.fragment_generic_recycler

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        recyclerView.configureWith(softLogoutController)
        softLogoutController.listener = this
    }

    override fun onDestroyView() {
        recyclerView.cleanup()
        softLogoutController.listener = null
        super.onDestroyView()
    }

    override fun retry() {
        softLogoutViewModel.handle(SoftLogoutAction.RetryLoginFlow)
    }

    override fun passwordEdited(password: String) {
        softLogoutViewModel.handle(SoftLogoutAction.PasswordChanged(password))
    }

    override fun submit(password: String) {
        cleanupUi()
        softLogoutViewModel.handle(SoftLogoutAction.SignInAgain(password))
    }

    override fun ssoSubmit() {
        // TODO loginSharedActionViewModel.post(LoginNavigation.Sso)
    }

    override fun clearData() {
        withState(softLogoutViewModel) { state ->
            cleanupUi()

            val messageResId = if (state.hasUnsavedKeys) {
                R.string.soft_logout_clear_data_dialog_e2e_warning_content
            } else {
                R.string.soft_logout_clear_data_dialog_content
            }

            AlertDialog.Builder(requireActivity())
                    .setTitle(R.string.soft_logout_clear_data_dialog_title)
                    .setMessage(messageResId)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.soft_logout_clear_data_submit) { _, _ ->
                        MainActivity.restartApp(requireActivity(), MainActivityArgs(
                                clearCache = true,
                                clearCredentials = true,
                                isUserLoggedOut = true
                        ))
                    }
                    .show()
                    .withColoredButton(DialogInterface.BUTTON_POSITIVE)
        }
    }

    private fun cleanupUi() {
        recyclerView.hideKeyboard()
    }

    override fun forgetPasswordClicked() {
        loginSharedActionViewModel.post(LoginNavigation.OnForgetPasswordClicked)
    }

    override fun revealPasswordClicked() {
        softLogoutViewModel.handle(SoftLogoutAction.TogglePassword)
    }

    override fun updateWithState(state: LoginViewState) {
        super.updateWithState(state)

        withState(softLogoutViewModel) { state2 ->
            softLogoutController.update(state2)
        }
    }

    override fun onError(throwable: Throwable) {
    }

    override fun resetViewModel() {
        // No op
    }
}
