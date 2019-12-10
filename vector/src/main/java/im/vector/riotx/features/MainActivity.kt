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
import im.vector.riotx.R
import im.vector.riotx.core.di.ActiveSessionHolder
import im.vector.riotx.core.di.ScreenComponent
import im.vector.riotx.core.error.ErrorFormatter
import im.vector.riotx.core.platform.VectorBaseActivity
import im.vector.riotx.core.utils.deleteAllFiles
import im.vector.riotx.features.home.HomeActivity
import im.vector.riotx.features.login.LoginActivity
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

    private var args: MainActivityArgs? = null
    @Inject lateinit var sessionHolder: ActiveSessionHolder
    @Inject lateinit var errorFormatter: ErrorFormatter

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        args = intent.getParcelableExtra(EXTRA_ARGS)

        val clearCache = args?.clearCache ?: false
        val clearCredentials = args?.clearCredentials ?: false

        // Handle some wanted cleanup
        if (clearCache || clearCredentials) {
            doCleanUp(clearCache, clearCredentials)
        } else {
            start()
        }
    }

    private fun doCleanUp(clearCache: Boolean, clearCredentials: Boolean) {
        when {
            clearCredentials -> sessionHolder.getActiveSession().signOut(object : MatrixCallback<Unit> {
                override fun onSuccess(data: Unit) {
                    Timber.w("SIGN_OUT: success, start app")
                    sessionHolder.clearActiveSession()
                    doLocalCleanupAndStart()
                }

                override fun onFailure(failure: Throwable) {
                    displayError(failure, clearCache, clearCredentials)
                }
            })
            clearCache       -> sessionHolder.getActiveSession().clearCache(object : MatrixCallback<Unit> {
                override fun onSuccess(data: Unit) {
                    doLocalCleanupAndStart()
                }

                override fun onFailure(failure: Throwable) {
                    displayError(failure, clearCache, clearCredentials)
                }
            })
        }
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

        start()
    }

    private fun displayError(failure: Throwable, clearCache: Boolean, clearCredentials: Boolean) {
        AlertDialog.Builder(this)
                .setTitle(R.string.dialog_title_error)
                .setMessage(errorFormatter.toHumanReadable(failure))
                .setPositiveButton(R.string.global_retry) { _, _ -> doCleanUp(clearCache, clearCredentials) }
                .setNegativeButton(R.string.cancel) { _, _ -> start() }
                .setCancelable(false)
                .show()
    }

    private fun start() {
        val intent = if (sessionHolder.hasActiveSession()) {
            HomeActivity.newIntent(this)
        } else {
            LoginActivity.newIntent(this, null)
        }
        startActivity(intent)
        finish()
    }
}
