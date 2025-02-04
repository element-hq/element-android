/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
