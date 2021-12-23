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

package im.vector.app.features.signout.hard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivitySignedOutBinding
import im.vector.app.features.MainActivity
import im.vector.app.features.MainActivityArgs
import org.matrix.android.sdk.api.failure.GlobalError
import timber.log.Timber

/**
 * In this screen, the user is viewing a message informing that he has been logged out
 */
@AndroidEntryPoint
class SignedOutActivity : VectorBaseActivity<ActivitySignedOutBinding>() {

    override fun getBinding() = ActivitySignedOutBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupViews()
    }

    private fun setupViews() {
        views.signedOutSubmit.debouncedClicks { submit() }
    }

    private fun submit() {
        // All is already cleared when we are here
        MainActivity.restartApp(this, MainActivityArgs())
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, SignedOutActivity::class.java)
        }
    }

    override fun handleInvalidToken(globalError: GlobalError.InvalidToken) {
        // No op here
        Timber.w("Ignoring invalid token global error")
    }
}
