/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.matrix.android.sdk.internal.task

import kotlinx.coroutines.delay
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.failure.shouldBeRetried
import timber.log.Timber

internal interface Task<PARAMS, RESULT> {

    suspend fun execute(params: PARAMS): RESULT

    suspend fun executeRetry(params: PARAMS, remainingRetry: Int) : RESULT {
        return try {
            execute(params)
        } catch (failure: Throwable) {
            if (failure.shouldBeRetried() && remainingRetry > 0) {
                Timber.d(failure, "## TASK: Retriable error")
                if (failure is Failure.ServerError) {
                    val waitTime = failure.error.retryAfterMillis ?: 0L
                    Timber.d(failure, "## TASK: Quota wait time $waitTime")
                    delay(waitTime + 100)
                }
                return executeRetry(params, remainingRetry - 1)
            }
            throw failure
        }
    }
}
