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

package im.vector.app.screenshot

import android.os.Build
import app.cash.paparazzi.DeviceConfig.Companion.PIXEL_3
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.androidHome
import app.cash.paparazzi.detectEnvironment

fun createPaparazziRule() = Paparazzi(
        // Apply trick from https://github.com/cashapp/paparazzi/issues/489#issuecomment-1195674603
        environment = detectEnvironment().copy(
                platformDir = "${androidHome()}/platforms/android-32",
                compileSdkVersion = Build.VERSION_CODES.S_V2 /* 32 */
        ),
        deviceConfig = PIXEL_3,
        theme = "Theme.Vector.Light",
        maxPercentDifference = 0.0,
)
