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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.V
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.share

import android.content.Intent
import android.os.Bundle
import com.airbnb.mvrx.viewModel
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivitySimpleBinding
import im.vector.app.features.MainActivity
import im.vector.app.features.start.StartAppViewModel

@AndroidEntryPoint
class IncomingShareActivity : VectorBaseActivity<ActivitySimpleBinding>() {

    private val startAppViewModel: StartAppViewModel by viewModel()

    private val launcher = registerStartForActivityResult {
        if (it.resultCode == RESULT_OK) {
            handleAppStarted()
        } else {
            // User has pressed back on the MainActivity, so finish also this one.
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (startAppViewModel.shouldStartApp()) {
            launcher.launch(MainActivity.getIntentToInitSession(this))
        } else {
            handleAppStarted()
        }
    }

    override fun getBinding() = ActivitySimpleBinding.inflate(layoutInflater)

    override fun getCoordinatorLayout() = views.coordinatorLayout

    private fun handleAppStarted() {
        // If we are not logged in, stop the sharing process and open login screen.
        // In the future, we might want to relaunch the sharing process after login.
        if (!activeSessionHolder.hasActiveSession()) {
            startLoginActivity()
        } else {
            if (isFirstCreation()) {
                addFragment(views.simpleFragmentContainer, IncomingShareFragment::class.java)
            }
        }
    }

    private fun startLoginActivity() {
        navigator.openLogin(
                context = this,
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        )
        finish()
    }
}
