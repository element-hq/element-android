/*
 * Copyright 2022 New Vector Ltd
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

package im.vector.app.features.settings.devices.v2.details

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.airbnb.mvrx.Mavericks
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.extensions.addFragment
import im.vector.app.core.platform.SimpleFragmentActivity
import im.vector.lib.core.utils.compat.getParcelableExtraCompat

/**
 * Display the details info about a Session.
 */
@AndroidEntryPoint
class SessionDetailsActivity : SimpleFragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isFirstCreation()) {
            addFragment(
                    container = views.container,
                    fragmentClass = SessionDetailsFragment::class.java,
                    params = intent.getParcelableExtraCompat(Mavericks.KEY_ARG)
            )
        }
    }

    companion object {
        fun newIntent(context: Context, deviceId: String): Intent {
            return Intent(context, SessionDetailsActivity::class.java).apply {
                putExtra(Mavericks.KEY_ARG, SessionDetailsArgs(deviceId))
            }
        }
    }
}
