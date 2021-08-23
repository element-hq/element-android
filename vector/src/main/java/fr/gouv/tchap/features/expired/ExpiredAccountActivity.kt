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

package fr.gouv.tchap.features.expired

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import im.vector.app.R
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.di.ScreenComponent
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivityTchapExpiredBinding
import im.vector.app.features.MainActivity
import im.vector.app.features.MainActivityArgs
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * In this screen, the user is viewing a message informing that his account has expired.
 */
class ExpiredAccountActivity : VectorBaseActivity<ActivityTchapExpiredBinding>() {

    @Inject internal lateinit var sessionHolder: ActiveSessionHolder

    override fun getBinding() = ActivityTchapExpiredBinding.inflate(layoutInflater)

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupViews()
    }

    private fun setupViews() {
        with(views) {
            resumeButton.setOnClickListener {
                MainActivity.restartApp(this@ExpiredAccountActivity, MainActivityArgs())
            }
            renewalEmailButton.setOnClickListener {
                lifecycleScope.launch { sessionHolder.getSafeActiveSession()?.accountValidityService()?.requestRenewalEmail() }
                titleView.setText(R.string.tchap_expired_account_on_new_sent_email_msg)
                renewalEmailButton.isVisible = false
            }
        }
    }

    override fun handleExpiredAccount() {
        // No op here
        Timber.w("Ignoring expired account global error")
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, ExpiredAccountActivity::class.java)
        }
    }
}
