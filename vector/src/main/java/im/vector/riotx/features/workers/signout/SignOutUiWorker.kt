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

package im.vector.riotx.features.workers.signout

import android.content.Context
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import im.vector.riotx.R
import im.vector.riotx.core.di.ActiveSessionHolder
import im.vector.riotx.core.extensions.vectorComponent
import im.vector.riotx.features.MainActivity
import im.vector.riotx.features.notifications.NotificationDrawerManager

class SignOutUiWorker(private val activity: FragmentActivity) {

    lateinit var notificationDrawerManager: NotificationDrawerManager
    lateinit var activeSessionHolder: ActiveSessionHolder

    fun perform(context: Context) {
        notificationDrawerManager = context.vectorComponent().notificationDrawerManager()
        activeSessionHolder = context.vectorComponent().activeSessionHolder()
        val session = activeSessionHolder.getActiveSession()
        if (SignOutViewModel.doYouNeedToBeDisplayed(session)) {
            val signOutDialog = SignOutBottomSheetDialogFragment.newInstance(session.sessionParams.credentials.userId)
            signOutDialog.onSignOut = Runnable {
                doSignOut()
            }
            signOutDialog.show(activity.supportFragmentManager, "SO")
        } else {
            // Display a simple confirmation dialog
            AlertDialog.Builder(activity)
                    .setTitle(R.string.action_sign_out)
                    .setMessage(R.string.action_sign_out_confirmation_simple)
                    .setPositiveButton(R.string.action_sign_out) { _, _ ->
                        doSignOut()
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
        }
    }

    private fun doSignOut() {
        // Dismiss all notifications
        notificationDrawerManager.clearAllEvents()

        MainActivity.restartApp(activity, clearCache = true, clearCredentials = true)
    }
}
