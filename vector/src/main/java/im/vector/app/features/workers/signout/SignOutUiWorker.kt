/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.workers.signout

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import im.vector.app.core.extensions.cannotLogoutSafely
import im.vector.app.core.extensions.singletonEntryPoint
import im.vector.app.features.MainActivity
import im.vector.app.features.MainActivityArgs
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session

class SignOutUiWorker(private val activity: FragmentActivity) {

    fun perform() {
        val session = activity.singletonEntryPoint().activeSessionHolder().getSafeActiveSession() ?: return
        activity.lifecycleScope.perform(session)
    }

    private fun CoroutineScope.perform(session: Session) = launch {
        if (session.cannotLogoutSafely()) {
            // The backup check on logout flow has to be displayed if there are keys in the store, and the keys backup state is not Ready
            val signOutDialog = SignOutBottomSheetDialogFragment.newInstance()
            signOutDialog.onSignOut = Runnable {
                doSignOut()
            }
            signOutDialog.show(activity.supportFragmentManager, "SO")
        } else {
            // Display a simple confirmation dialog
            MaterialAlertDialogBuilder(activity)
                    .setTitle(CommonStrings.action_sign_out)
                    .setMessage(CommonStrings.action_sign_out_confirmation_simple)
                    .setPositiveButton(CommonStrings.action_sign_out) { _, _ ->
                        doSignOut()
                    }
                    .setNegativeButton(CommonStrings.action_cancel, null)
                    .show()
        }
    }

    private fun doSignOut() {
        MainActivity.restartApp(activity, MainActivityArgs(clearCredentials = true))
    }
}
