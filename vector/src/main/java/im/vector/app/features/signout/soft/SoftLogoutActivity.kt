/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
import im.vector.app.core.extensions.replaceFragment
import im.vector.app.features.MainActivity
import im.vector.app.features.MainActivityArgs
import im.vector.app.features.login.LoginActivity
import im.vector.lib.strings.CommonStrings
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
                                CommonStrings.soft_logout_sso_not_same_user_error,
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
                .setTitle(CommonStrings.dialog_title_error)
                .setMessage(message)
                .setPositiveButton(CommonStrings.ok, null)
                .show()
    }

    override fun addFirstFragment() {
        replaceFragment(views.loginFragmentContainer, SoftLogoutFragment::class.java)
    }

    private fun updateWithState(softLogoutViewState: SoftLogoutViewState) {
        if (softLogoutViewState.asyncLoginAction is Success) {
            MainActivity.restartApp(this, MainActivityArgs())
        }

        views.loginLoading.isVisible = softLogoutViewState.isLoading
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
