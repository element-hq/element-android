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

package im.vector.riotredesign.features.workers.signout

import android.content.Intent
import androidx.appcompat.app.AlertDialog
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.Session
import im.vector.riotredesign.R
import im.vector.riotredesign.core.platform.VectorBaseActivity
import im.vector.riotredesign.features.MainActivity

class SignOutUiWorker(val activity: VectorBaseActivity) {

    fun perform(session: Session) {
        if (SignOutViewModel.doYouNeedToBeDisplayed(session)) {
            val signOutDialog = SignOutBottomSheetDialogFragment.newInstance(session.sessionParams.credentials.userId)
            signOutDialog.onSignOut = Runnable {
                doSignOut(session)
            }
            signOutDialog.show(activity.supportFragmentManager, "SO")
        } else {
            // Display a simple confirmation dialog
            AlertDialog.Builder(activity)
                    .setTitle(R.string.action_sign_out)
                    .setMessage(R.string.action_sign_out_confirmation_simple)
                    .setPositiveButton(R.string.action_sign_out) { _, _ ->
                        doSignOut(session)
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
        }
    }

    private fun doSignOut(session: Session) {
        // TODO showWaitingView()

        session.signOut(object : MatrixCallback<Unit> {

            override fun onSuccess(data: Unit) {
                // Start MainActivity in a new task
                val intent = Intent(activity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

                activity.startActivity(intent)
            }

            override fun onFailure(failure: Throwable) {
                // TODO Notify user, or ignore?
            }

        })

    }
}
