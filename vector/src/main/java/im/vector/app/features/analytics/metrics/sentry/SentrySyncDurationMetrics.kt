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

package im.vector.app.features.analytics.metrics.sentry

import io.sentry.ISpan
import io.sentry.ITransaction
import io.sentry.Sentry
import io.sentry.SpanStatus
import org.matrix.android.sdk.api.metrics.SyncDurationMetricPlugin
import java.util.EmptyStackException
import java.util.Stack
import javax.inject.Inject

/**
 * Sentry based implementation of SyncDurationMetricPlugin.
 */
class SentrySyncDurationMetrics @Inject constructor() : SyncDurationMetricPlugin {
    private var transaction: ITransaction? = null

    // Stacks to keep spans in LIFO order.
    private var spans: Stack<ISpan> = Stack()

    override fun shouldReport(isInitialSync: Boolean, isAfterPause: Boolean): Boolean {
        // Report only for initial sync and for sync after pause
        return isInitialSync || isAfterPause
    }

    /**
     * Starts the span for a sub-task.
     *
     * @param operation Name of the new span.
     * @param description Description of the new span.
     *
     * @throws IllegalStateException if this is called without starting a transaction ie. `measureSpan` must be called within `measureMetric`.
     */
    override fun startSpan(operation: String, description: String) {
        if (Sentry.isEnabled()) {
            val span = Sentry.getSpan() ?: throw IllegalStateException("measureSpan block must be called within measureMetric")
            val innerSpan = span.startChild(operation, description)
            spans.push(innerSpan)
            logTransaction("Sentry span started: operation=[$operation], description=[$description]")
        }
    }

    override fun finishSpan() {
        try {
            spans.pop()
        } catch (e: EmptyStackException) {
            null
        }?.finish()
        logTransaction("Sentry span finished")
    }

    override fun startTransaction() {
        if (Sentry.isEnabled()) {
            transaction = Sentry.startTransaction("sync_response_handler", "task", true)
            logTransaction("Sentry transaction started")
        }
    }

    override fun finishTransaction() {
        transaction?.finish()
        transaction = null
        logTransaction("Sentry transaction finished")
    }

    override fun onError(throwable: Throwable) {
        try {
            spans.peek()
        } catch (e: EmptyStackException) {
            null
        }?.apply {
            this.throwable = throwable
            this.status = SpanStatus.INTERNAL_ERROR
        } ?: transaction?.apply {
            this.throwable = throwable
            this.status = SpanStatus.INTERNAL_ERROR
        }
        logTransaction("Sentry transaction encountered error ${throwable.message}")
    }
}
