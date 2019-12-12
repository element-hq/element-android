/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.riotx.features

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.failure.GlobalError
import im.vector.riotx.R
import im.vector.riotx.core.di.ActiveSessionHolder
import im.vector.riotx.core.di.ScreenComponent
import im.vector.riotx.core.error.ErrorFormatter
import im.vector.riotx.core.platform.VectorBaseActivity
import im.vector.riotx.core.utils.deleteAllFiles
import im.vector.riotx.features.home.HomeActivity
import im.vector.riotx.features.login.LoginActivity
import im.vector.riotx.features.signout.SignedOutActivity
import im.vector.riotx.features.signout.SoftLogoutActivity
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@Parcelize
data class MainActivityArgs(
        val clearCache: Boolean = false,
        val clearCredentials: Boolean = false,
        val isUserLoggedOut: Boolean = false,
        val isSoftLogout: Boolean = false
) : Parcelable

class MainActivity : VectorBaseActivity() {

    companion object {
        private const val EXTRA_ARGS = "EXTRA_ARGS"

        // Special action to clear cache and/or clear credentials
        fun restartApp(activity: Activity, args: MainActivityArgs) {
            val intent = Intent(activity, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

            intent.putExtra(EXTRA_ARGS, args)
            activity.startActivity(intent)
        }
    }

    private lateinit var args: MainActivityArgs

    @Inject lateinit var sessionHolder: ActiveSessionHolder
    @Inject lateinit var errorFormatter: ErrorFormatter

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        args = parseArgs()

        // Handle some wanted cleanup
        if (args.clearCache || args.clearCredentials) {
            doCleanUp()
        } else {
            startNextActivityAndFinish()
        }
    }

    private fun parseArgs(): MainActivityArgs {
        val argsFromIntent: MainActivityArgs? = intent.getParcelableExtra(EXTRA_ARGS)
        Timber.w("Starting MainActivity with $argsFromIntent")

        return MainActivityArgs(
                clearCache = argsFromIntent?.clearCache ?: false,
                clearCredentials = argsFromIntent?.clearCredentials ?: false,
                isUserLoggedOut = argsFromIntent?.isUserLoggedOut ?: false,
                isSoftLogout = argsFromIntent?.isSoftLogout ?: false
        )
    }

    private fun doCleanUp() {
        when {
            args.clearCredentials -> sessionHolder.getActiveSession().signOut(
                    !args.isUserLoggedOut,
                    object : MatrixCallback<Unit> {
                        override fun onSuccess(data: Unit) {
                            Timber.w("SIGN_OUT: success, start app")
                            sessionHolder.clearActiveSession()
                            doLocalCleanupAndStart()
                        }

                        override fun onFailure(failure: Throwable) {
                            displayError(failure)
                        }
                    })
            args.clearCache       -> sessionHolder.getActiveSession().clearCache(
                    object : MatrixCallback<Unit> {
                        override fun onSuccess(data: Unit) {
                            doLocalCleanupAndStart()
                        }

                        override fun onFailure(failure: Throwable) {
                            displayError(failure)
                        }
                    })
        }
    }

    override fun handleInvalidToken(globalError: GlobalError.InvalidToken) {
        // No op here
        Timber.w("Ignoring invalid token global error")
    }

    private fun doLocalCleanupAndStart() {
        GlobalScope.launch(Dispatchers.Main) {
            // On UI Thread
            Glide.get(this@MainActivity).clearMemory()
            withContext(Dispatchers.IO) {
                // On BG thread
                Glide.get(this@MainActivity).clearDiskCache()

                // Also clear cache (Logs, etc...)
                deleteAllFiles(this@MainActivity.cacheDir)
            }
        }

        startNextActivityAndFinish()
    }

    private fun displayError(failure: Throwable) {
        AlertDialog.Builder(this)
                .setTitle(R.string.dialog_title_error)
                .setMessage(errorFormatter.toHumanReadable(failure))
                .setPositiveButton(R.string.global_retry) { _, _ -> doCleanUp() }
                .setNegativeButton(R.string.cancel) { _, _ -> startNextActivityAndFinish() }
                .setCancelable(false)
                .show()
    }

    private fun startNextActivityAndFinish() {
        val intent = when {
            args.clearCredentials            ->
                // User has explicitly asked to log out
                LoginActivity.newIntent(this, null)
            args.isSoftLogout                ->
                // The homeserver has invalidated the token, with a soft logout
                SoftLogoutActivity.newIntent(this)
            args.isUserLoggedOut             ->
                // the homeserver has invalidated the token (password changed, device deleted, other security reason
                SignedOutActivity.newIntent(this)
            sessionHolder.hasActiveSession() ->
                // We have a session.
                // Check it can be opened
                if(sessionHolder.getActiveSession().isOpenable) {
                    HomeActivity.newIntent(this)
                } else {
                    // The token is still invalid
                    SoftLogoutActivity.newIntent(this)
                }
            else                             ->
                // First start, or no active session
                LoginActivity.newIntent(this, null)
        }
        startActivity(intent)
        finish()
    }
}
