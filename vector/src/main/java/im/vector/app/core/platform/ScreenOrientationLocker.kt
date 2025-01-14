/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.platform

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.res.Resources
import androidx.appcompat.app.AppCompatActivity
import im.vector.lib.ui.styles.R
import javax.inject.Inject

class ScreenOrientationLocker @Inject constructor(
        private val resources: Resources
) {

    // Some screens do not provide enough value for us to provide phone landscape experiences
    @SuppressLint("SourceLockedOrientationActivity")
    fun lockPhonesToPortrait(activity: AppCompatActivity) {
        when (resources.getBoolean(R.bool.is_tablet)) {
            true -> {
                // do nothing
            }
            false -> {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }
}
