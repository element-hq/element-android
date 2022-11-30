/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.metrics

/**
 * A plugin that can be used to capture metrics in Client.
 */
interface MetricPlugin {
    /**
     * Start the measurement of the metrics as soon as task is started.
     */
    fun startTransaction()

    /**
     * Mark the measuring transaction finished once the task is completed.
     */
    fun finishTransaction()

    /**
     * Invoked when there is any error in the ongoing task. The metrics tool can use this information to attach to the ongoing transaction.
     *
     * @param throwable Exception thrown in the running task.
     */
    fun onError(throwable: Throwable)

    /**
     * Can be used to log this transaction.
     */
    fun logTransaction(message: String? = "") {
        // no-op
    }
}
