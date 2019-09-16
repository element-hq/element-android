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
import com.bumptech.glide.Glide
import im.vector.matrix.android.api.Matrix
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.auth.Authenticator
import im.vector.riotx.core.di.ActiveSessionHolder
import im.vector.riotx.core.di.ScreenComponent
import im.vector.riotx.core.platform.VectorBaseActivity
import im.vector.riotx.core.utils.deleteAllFiles
import im.vector.riotx.features.home.HomeActivity
import im.vector.riotx.features.login.LoginActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject


class MainActivity : VectorBaseActivity() {

    companion object {
        private const val EXTRA_CLEAR_CACHE = "EXTRA_CLEAR_CACHE"
        private const val EXTRA_CLEAR_CREDENTIALS = "EXTRA_CLEAR_CREDENTIALS"

        // Special action to clear cache and/or clear credentials
        fun restartApp(activity: Activity, clearCache: Boolean = false, clearCredentials: Boolean = false) {
            val intent = Intent(activity, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

            intent.putExtra(EXTRA_CLEAR_CACHE, clearCache)
            intent.putExtra(EXTRA_CLEAR_CREDENTIALS, clearCredentials)
            activity.startActivity(intent)
        }
    }

    @Inject lateinit var matrix: Matrix
    @Inject lateinit var authenticator: Authenticator
    @Inject lateinit var sessionHolder: ActiveSessionHolder

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val clearCache = intent.getBooleanExtra(EXTRA_CLEAR_CACHE, false)
        val clearCredentials = intent.getBooleanExtra(EXTRA_CLEAR_CREDENTIALS, false)

        // Handle some wanted cleanup
        if (clearCache || clearCredentials) {
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
        }

        when {
            clearCredentials -> sessionHolder.getActiveSession().signOut(object : MatrixCallback<Unit> {
                override fun onSuccess(data: Unit) {
                    Timber.w("SIGN_OUT: success, start app")
                    sessionHolder.clearActiveSession()
                    start()
                }
            })
            clearCache       -> sessionHolder.getActiveSession().clearCache(object : MatrixCallback<Unit> {
                override fun onSuccess(data: Unit) {
                    start()
                }
            })
            else             -> start()

        }
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