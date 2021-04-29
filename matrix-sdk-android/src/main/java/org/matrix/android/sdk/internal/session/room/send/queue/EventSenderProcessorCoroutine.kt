/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room.send.queue

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.auth.data.SessionParams
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.failure.MatrixError
import org.matrix.android.sdk.api.failure.getRetryDelay
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.CryptoService
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.util.Cancelable
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.task.CoroutineSequencer
import org.matrix.android.sdk.internal.task.SemaphoreCoroutineSequencer
import org.matrix.android.sdk.internal.task.TaskExecutor
import org.matrix.android.sdk.internal.util.toCancelable
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

private const val RETRY_WAIT_TIME_MS = 10_000L
private const val MAX_RETRY_COUNT = 3

/**
 * This class is responsible for sending events in order in each room. It uses the QueuedTask.queueIdentifier to execute tasks sequentially.
 * Each send is retried 3 times, if there is no network (e.g if cannot ping home server) it will wait and
 * periodically test reachability before resume (does not count as a retry)
 *
 * If the app is killed before all event were sent, on next wakeup the scheduled events will be re posted
 *
 */
@SessionScope
internal class EventSenderProcessorCoroutine @Inject constructor(
        private val cryptoService: CryptoService,
        private val sessionParams: SessionParams,
        private val queuedTaskFactory: QueuedTaskFactory,
        private val taskExecutor: TaskExecutor,
        private val memento: QueueMemento
) : EventSenderProcessor {

    private val waitForNetworkSequencer = SemaphoreCoroutineSequencer()

    /**
     * sequencers use QueuedTask.queueIdentifier as key
     */
    private val sequencers = ConcurrentHashMap<String, CoroutineSequencer>()

    /**
     * cancelableBag use QueuedTask.taskIdentifier as key
     */
    private val cancelableBag = ConcurrentHashMap<String, Cancelable>()

    override fun onSessionStarted(session: Session) {
        // We should check for sending events not handled because app was killed
        // But we should be careful of only took those that was submitted to us, because if it's
        // for example it's a media event it is handled by some worker and he will handle it
        // This is a bit fragile :/
        // also some events cannot be retried manually by users, e.g reactions
        // they were previously relying on workers to do the work :/ and was expected to always finally succeed
        // Also some echos are not to be resent like redaction echos (fake event created for aggregation)
        taskExecutor.executorScope.launch {
            Timber.d("## Send relaunched pending events on restart")
            try {
                memento.restoreTasks(this@EventSenderProcessorCoroutine)
            } catch (failure: Throwable) {
                Timber.e(failure, "Fail restoring send tasks")
            }
        }
    }

    override fun postEvent(event: Event): Cancelable {
        return postEvent(event, event.roomId?.let { cryptoService.isRoomEncrypted(it) } ?: false)
    }

    override fun postEvent(event: Event, encrypt: Boolean): Cancelable {
        val task = queuedTaskFactory.createSendTask(event, encrypt)
        return postTask(task)
    }

    override fun postRedaction(redactionLocalEcho: Event, reason: String?): Cancelable {
        return postRedaction(redactionLocalEcho.eventId!!, redactionLocalEcho.redacts!!, redactionLocalEcho.roomId!!, reason)
    }

    override fun postRedaction(redactionLocalEchoId: String, eventToRedactId: String, roomId: String, reason: String?): Cancelable {
        val task = queuedTaskFactory.createRedactTask(redactionLocalEchoId, eventToRedactId, roomId, reason)
        return postTask(task)
    }

    override fun postTask(task: QueuedTask): Cancelable {
        markAsManaged(task)
        val sequencer = sequencers.getOrPut(task.queueIdentifier) {
            SemaphoreCoroutineSequencer()
        }
        Timber.v("## post $task")
        return taskExecutor.executorScope
                .launchWith(sequencer) {
                    executeTask(task)
                }.toCancelable()
                .also {
                    cancelableBag[task.taskIdentifier] = it
                }
    }

    override fun cancel(eventId: String, roomId: String) {
        // eventId is most likely the taskIdentifier
        cancelableBag[eventId]?.cancel()
    }

    private fun CoroutineScope.launchWith(sequencer: CoroutineSequencer, block: suspend CoroutineScope.() -> Unit) = launch {
        sequencer.post {
            block()
        }
    }

    private suspend fun executeTask(task: QueuedTask) {
        try {
            if (task.isCancelled()) {
                Timber.v("## $task has been cancelled, try next task")
                return
            }
            task.waitForNetwork()
            task.execute()
        } catch (exception: Throwable) {
            when {
                exception is IOException || exception is Failure.NetworkConnection                         -> {
                    canReachServer.set(false)
                    task.markAsFailedOrRetry(exception, 0)
                }
                (exception is Failure.ServerError && exception.error.code == MatrixError.M_LIMIT_EXCEEDED) -> {
                    task.markAsFailedOrRetry(exception, exception.getRetryDelay(3_000))
                }
                exception is CancellationException                                                         -> {
                    Timber.v("## $task has been cancelled, try next task")
                }
                else                                                                                       -> {
                    Timber.v("## un-retryable error for $task, try next task")
                    // this task is in error, check next one?
                    task.onTaskFailed()
                }
            }
        }
        markAsFinished(task)
    }

    private suspend fun QueuedTask.markAsFailedOrRetry(failure: Throwable, retryDelay: Long) {
        if (retryCount.incrementAndGet() >= MAX_RETRY_COUNT) {
            onTaskFailed()
        } else {
            Timber.v("## retryable error for $this reason: ${failure.localizedMessage}")
            // Wait if necessary
            delay(retryDelay)
            // And then retry
            executeTask(this)
        }
    }

    private fun markAsManaged(task: QueuedTask) {
        memento.track(task)
    }

    private fun markAsFinished(task: QueuedTask) {
        cancelableBag.remove(task.taskIdentifier)
        memento.unTrack(task)
    }

    private val canReachServer = AtomicBoolean(true)

    private suspend fun QueuedTask.waitForNetwork() = waitForNetworkSequencer.post {
        while (!canReachServer.get()) {
            Timber.v("## $this cannot reach server wait ts:${System.currentTimeMillis()}")
            delay(RETRY_WAIT_TIME_MS)
            withContext(Dispatchers.IO) {
                val hostAvailable = HomeServerAvailabilityChecker(sessionParams).check()
                canReachServer.set(hostAvailable)
            }
        }
    }
}
