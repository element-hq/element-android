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

package im.vector.app.features.signout.soft

import android.content.Context
import android.content.Intent
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.viewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.extensions.replaceFragment
import im.vector.app.features.MainActivity
import im.vector.app.features.MainActivityArgs
import im.vector.app.features.login.LoginActivity
import org.matrix.android.sdk.api.failure.GlobalError
import org.matrix.android.sdk.api.session.Session
import timber.log.Timber
import javax.inject.Inject

/**
 * In this screen, the user is viewing a message informing that he has been logged out.
 * Extends LoginActivity to get the login with SSO and forget password functionality for (nearly) free.
 */
@AndroidEntryPoint
class SoftLogoutActivity : LoginActivity() {

    private val softLogoutViewModel: SoftLogoutViewModel by viewModel()

    @Inject lateinit var session: Session
    @Inject lateinit var errorFormatter: ErrorFormatter

    override fun initUiAndData() {
        super.initUiAndData()

        softLogoutViewModel.onEach {
            updateWithState(it)
        }

        softLogoutViewModel.observeViewEvents { handleSoftLogoutViewEvents(it) }
    }

    private fun handleSoftLogoutViewEvents(softLogoutViewEvents: SoftLogoutViewEvents) {
        when (softLogoutViewEvents) {
            is SoftLogoutViewEvents.Failure ->
                showError(errorFormatter.toHumanReadable(softLogoutViewEvents.throwable))
            is SoftLogoutViewEvents.ErrorNotSameUser -> {
                // Pop the backstack
                supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)

                // And inform the user
                showError(
                        getString(
                                R.string.soft_logout_sso_not_same_user_error,
                                softLogoutViewEvents.currentUserId,
                                softLogoutViewEvents.newUserId
                        )
                )
            }
            is SoftLogoutViewEvents.ClearData -> {
                MainActivity.restartApp(this, MainActivityArgs(clearCredentials = true))
            }
        }
    }

    private fun showError(message: String) {
        MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dialog_title_error)
                .setMessage(message)
                .setPositiveButton(R.string.ok, null)
                .show()
    }

    override fun addFirstFragment() {
        replaceFragment(views.loginFragmentContainer, SoftLogoutFragment::class.java)
    }

    private fun updateWithState(softLogoutViewState: SoftLogoutViewState) {
        if (softLogoutViewState.asyncLoginAction is Success) {
            MainActivity.restartApp(this, MainActivityArgs())
        }

        views.loginLoading.isVisible = softLogoutViewState.isLoading()
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, SoftLogoutActivity::class.java)
        }
    }

    override fun handleInvalidToken(globalError: GlobalError.InvalidToken) {
        // No op here
        Timber.w("Ignoring invalid token global error")
    }
}
