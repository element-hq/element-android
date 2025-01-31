/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.util

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper

internal fun createBackgroundHandler(name: String): Handler = Handler(
        HandlerThread(name).apply { start() }.looper
)

internal fun createUIHandler(): Handler = Handler(
        Looper.getMainLooper()
)
