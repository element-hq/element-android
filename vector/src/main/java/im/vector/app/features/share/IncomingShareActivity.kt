/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
