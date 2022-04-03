/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.common

import android.os.Debug

object TestConstants {

    const val TESTS_HOME_SERVER_URL = "http://10.0.2.2:8080"

    // Time out to use when waiting for server response.
    private const val AWAIT_TIME_OUT_MILLIS = 60_000

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
