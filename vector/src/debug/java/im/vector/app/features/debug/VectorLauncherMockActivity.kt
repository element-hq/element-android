/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.debug

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.airbnb.mvrx.launcher.MavericksLauncherMockActivity
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.databinding.ActivityLauncherMockBinding

@AndroidEntryPoint
class VectorLauncherMockActivity : VectorBaseActivity<ActivityLauncherMockBinding>() {

    override fun getBinding() = ActivityLauncherMockBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            MavericksLauncherMockActivity.showNextMockFromActivity(
                    activity = this,
                    showView = { view ->
                        // Use commit now to catch errors on initialization.
                        setFragment(view as Fragment, commitNow = true)
                    }
            )
        }
    }

    protected fun setFragment(fragment: Fragment, commitNow: Boolean = false) {
        supportFragmentManager.beginTransaction()
                .replace(R.id.container, fragment)
                .setPrimaryNavigationFragment(fragment)
                .apply {
                    if (commitNow) commitNow() else commit()
                }
    }
}
