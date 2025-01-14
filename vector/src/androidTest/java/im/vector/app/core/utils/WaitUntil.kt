/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
