/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
