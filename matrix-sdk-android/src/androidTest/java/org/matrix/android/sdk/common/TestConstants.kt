/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.common

import android.os.Debug

object TestConstants {

    const val TESTS_HOME_SERVER_URL = "http://10.0.2.2:8080"

    // Time out to use when waiting for server response.
    private const val AWAIT_TIME_OUT_MILLIS = 120_000

    // Time out to use when waiting for server response, when the debugger is connected. 10 minutes
    private const val AWAIT_TIME_OUT_WITH_DEBUGGER_MILLIS = 10 * 60_000

    const val USER_ALICE = "Alice"
    const val USER_BOB = "Bob"
    const val USER_SAM = "Sam"

    const val PASSWORD = "password"

    val timeOutMillis: Long
        get() = if (Debug.isDebuggerConnected()) {
            // Wait more
            AWAIT_TIME_OUT_WITH_DEBUGGER_MILLIS.toLong()
        } else {
            AWAIT_TIME_OUT_MILLIS.toLong()
        }
}
