/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.utils

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper

internal fun createBackgroundHandler(name: String): Handler = Handler(
        HandlerThread(name).apply { start() }.looper
)

internal fun createUIHandler(): Handler = Handler(
        Looper.getMainLooper()
)
