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

package im.vector.app.core.utils

import kotlinx.coroutines.delay
import org.amshove.kluent.fail
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Tries a [condition] several times until it returns true or a [timeout] is reached waiting for some [retryDelay] time between retries.
 * On timeout it fails with an [errorMessage].
 */
suspend fun waitUntilCondition(
        errorMessage: String,
        timeout: Duration = 1.seconds,
        retryDelay: Duration = 50.milliseconds,
        condition: () -> Boolean,
) {
    val start = System.currentTimeMillis()
    do {
        if (condition()) return
        delay(retryDelay.inWholeMilliseconds)
    } while (System.currentTimeMillis() - start < timeout.inWholeMilliseconds)
    fail(errorMessage)
}

/**
 * Tries a [block] several times until it runs with no errors or a [timeout] is reached waiting for some [retryDelay] time between retries.
 * On timeout it fails with a custom [errorMessage] or a caught [AssertionError].
 */
suspend fun waitUntil(
        errorMessage: String? = null,
        timeout: Duration = 1.seconds,
        retryDelay: Duration = 50.milliseconds,
        block: () -> Unit,
) {
    var error: AssertionError?
    val start = System.currentTimeMillis()
    do {
        try {
            block()
            return
        } catch (e: AssertionError) {
            error = e
        }
        delay(retryDelay.inWholeMilliseconds)
    } while (System.currentTimeMillis() - start < timeout.inWholeMilliseconds)
    if (errorMessage != null) {
        fail(errorMessage)
    } else {
        throw error!!
    }
}
