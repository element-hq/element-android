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

package org.matrix.android.sdk.internal.session.room.send.queue

import org.matrix.android.sdk.api.util.Cancelable
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger

/**
 * @param queueIdentifier String value to identify a unique Queue
 * @param taskIdentifier String value to identify a unique Task. Should be different from queueIdentifier
 */
internal abstract class QueuedTask(
        val queueIdentifier: String,
        val taskIdentifier: String
) : Cancelable {

    override fun toString() = "${javaClass.simpleName} queueIdentifier: $queueIdentifier, taskIdentifier:  $taskIdentifier)"

    var retryCount = AtomicInteger(0)

    private var hasBeenCancelled: Boolean = false

    suspend fun execute() {
        if (!isCancelled()) {
            Timber.v("Execute: $this start")
            doExecute()
            Timber.v("Execute: $this finish")
        }
    }

    abstract suspend fun doExecute()

    abstract fun onTaskFailed()

    open fun isCancelled() = hasBeenCancelled

    final override fun cancel() {
        hasBeenCancelled = true
    }
}
