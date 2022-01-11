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

package im.vector.app.core.platform

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.res.Resources
import androidx.appcompat.app.AppCompatActivity
import im.vector.app.R
import javax.inject.Inject

class ScreenOrientationLocker @Inject constructor(
        private val resources: Resources
) {

    // Some screens do not provide enough value for us to provide phone landscape experiences
    @SuppressLint("SourceLockedOrientationActivity")
    fun lockPhonesToPortrait(activity: AppCompatActivity) {
        when (resources.getBoolean(R.bool.is_tablet)) {
            true  -> {
                // do nothing
            }
            false -> {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }
}
