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

package im.vector.riotx.features.permalink

import android.content.Intent
import android.os.Bundle
import im.vector.riotx.core.di.ActiveSessionHolder
import im.vector.riotx.core.di.ScreenComponent
import im.vector.riotx.core.platform.VectorBaseActivity
import im.vector.riotx.features.login.LoginActivity
import timber.log.Timber
import javax.inject.Inject

class PermalinkHandlerActivity : VectorBaseActivity() {

    @Inject lateinit var permalinkHandler: PermalinkHandler
    @Inject lateinit var sessionHolder: ActiveSessionHolder

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // If we are not logged in, stop the sharing process and open login screen.
        // In the future, we might want to relaunch the sharing process after login.
        if (!sessionHolder.hasActiveSession()) {
            startLoginActivity()
            return
        }
        val uri = intent.dataString
        val isHandled = permalinkHandler.launch(this, uri, buildTask = true)
        if (!isHandled) {
            Timber.v("Couldn't handle permalink")
        }
        finish()
    }

    private fun startLoginActivity() {
        val intent = LoginActivity.newIntent(this, null)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }


}
