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

package im.vector.app.features.permalink

import android.content.Intent
import android.os.Bundle
import im.vector.app.R
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.extensions.replaceFragment
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.core.utils.toast
import im.vector.app.features.home.LoadingFragment
import im.vector.app.features.login.LoginActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class PermalinkHandlerActivity : VectorBaseActivity() {

    @Inject lateinit var permalinkHandler: PermalinkHandler
    @Inject lateinit var sessionHolder: ActiveSessionHolder

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple)
        if (isFirstCreation()) {
            replaceFragment(R.id.simpleFragmentContainer, LoadingFragment::class.java)
        }
        // If we are not logged in, open login screen.
        // In the future, we might want to relaunch the process after login.
        if (!sessionHolder.hasActiveSession()) {
            startLoginActivity()
            return
        }
        val uri = intent.dataString
        permalinkHandler.launch(this, uri, buildTask = true)
                .delay(500, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { isHandled ->
                    if (!isHandled) {
                        toast(R.string.permalink_malformed)
                    }
                    finish()
                }
                .disposeOnDestroy()
    }

    private fun startLoginActivity() {
        val intent = LoginActivity.newIntent(this, null)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }
}
