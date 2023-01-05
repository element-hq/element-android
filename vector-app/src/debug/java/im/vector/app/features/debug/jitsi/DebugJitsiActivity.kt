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

package im.vector.app.features.debug.jitsi

import android.annotation.SuppressLint
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.application.databinding.ActivityDebugJitsiBinding
import org.jitsi.meet.sdk.JitsiMeet

@AndroidEntryPoint
class DebugJitsiActivity : VectorBaseActivity<ActivityDebugJitsiBinding>() {

    override fun getBinding() = ActivityDebugJitsiBinding.inflate(layoutInflater)

    override fun getCoordinatorLayout() = views.coordinatorLayout

    @SuppressLint("SetTextI18n")
    override fun initUiAndData() {
        val isCrashReportingDisabled = JitsiMeet.isCrashReportingDisabled(this)
        views.status.text = "Jitsi crash reporting is disabled: $isCrashReportingDisabled"

        views.splash.setOnClickListener {
            JitsiMeet.showSplashScreen(this)
        }

        views.dev.setOnClickListener {
            JitsiMeet.showDevOptions()
        }
    }
}
