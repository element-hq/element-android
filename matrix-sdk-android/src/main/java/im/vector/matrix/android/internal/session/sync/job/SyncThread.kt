/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.matrix.android.internal.session.sync.job

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.squareup.moshi.JsonEncodingException
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.failure.MatrixError
import im.vector.matrix.android.api.session.sync.SyncState
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.network.NetworkConnectivityChecker
import im.vector.matrix.android.internal.session.sync.SyncTask
import im.vector.matrix.android.internal.session.sync.SyncTokenStore
import im.vector.matrix.android.internal.session.sync.model.SyncResponse
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.TaskThread
import im.vector.matrix.android.internal.task.configureWith
import im.vector.matrix.android.internal.util.BackgroundDetectionObserver
import timber.log.Timber
import java.net.SocketTimeoutException
import java.util.concurrent.CountDownLatch

private const val RETRY_WAIT_TIME_MS = 10_000L
private const val DEFAULT_LONG_POOL_TIMEOUT = 30_000L
private const val DEFAULT_LONG_POOL_DELAY = 0L

internal class SyncThread(private val syncTask: SyncTask,
                          private val networkConnectivityChecker: NetworkConnectivityChecker,
                          private val syncTokenStore: SyncTokenStore,
                          private val backgroundDetectionObserver: BackgroundDetectionObserver,
                          private val taskExecutor: TaskExecutor
) : Thread(), NetworkConnectivityChecker.Listener, BackgroundDetectionObserver.Listener {

    private var state: SyncState = SyncState.IDLE
    private var liveState = MutableLiveData<SyncState>()
    private val lock = Object()
    private var nextBatch = syncTokenStore.getLastToken()
    private var cancelableTask: Cancelable? = null

    init {
        updateStateTo(SyncState.IDLE)
    }

    fun restart() = synchronized(lock) {
        if (state is SyncState.PAUSED) {
            Timber.v("Resume sync...")
            // Retrieve the last token, it may have been deleted in case of a clear cache
            nextBatch = syncTokenStore.getLastToken()
            updateStateTo(SyncState.RUNNING(catchingUp = true))
            lock.notify()
        }
    }

    fun pause() = synchronized(lock) {
        if (state is SyncState.RUNNING) {
            Timber.v("Pause sync...")
            updateStateTo(SyncState.PAUSED)
        }
    }

    fun kill() = synchronized(lock) {
        Timber.v("Kill sync...")
        updateStateTo(SyncState.KILLING)
        cancelableTask?.cancel()
        lock.notify()
    }

    fun liveState(): LiveData<SyncState> {
        return liveState
    }

    override fun run() {
        Timber.v("Start syncing...")
        networkConnectivityChecker.register(this)
        backgroundDetectionObserver.register(this)
        updateStateTo(SyncState.RUNNING(catchingUp = true))

        while (state != SyncState.KILLING) {
            if (!networkConnectivityChecker.isConnected() || state == SyncState.PAUSED) {
                Timber.v("Sync is Paused. Waiting...")
                synchronized(lock) {
                    lock.wait()
                }
            } else {
                Timber.v("Execute sync request with token $nextBatch and timeout $DEFAULT_LONG_POOL_TIMEOUT")
                val latch = CountDownLatch(1)
                val params = SyncTask.Params(nextBatch, DEFAULT_LONG_POOL_TIMEOUT)
                cancelableTask = syncTask.configureWith(params)
                        .callbackOn(TaskThread.CALLER)
                        .executeOn(TaskThread.CALLER)
                        .dispatchTo(object : MatrixCallback<SyncResponse> {
                            override fun onSuccess(data: SyncResponse) {
                                nextBatch = data.nextBatch
                                syncTokenStore.saveToken(nextBatch)
                                latch.countDown()
                            }

                            override fun onFailure(failure: Throwable) {
                                if (failure is Failure.NetworkConnection
                                        && failure.cause is SocketTimeoutException) {
                                    // Timeout are not critical
                                    Timber.v("Timeout")
                                } else {
                                    Timber.e(failure)
                                }

                                if (failure !is Failure.NetworkConnection
                                        || failure.cause is JsonEncodingException) {
                                    // Wait 10s before retrying
                                    sleep(RETRY_WAIT_TIME_MS)
                                }

                                if (failure is Failure.ServerError
                                        && (failure.error.code == MatrixError.UNKNOWN_TOKEN || failure.error.code == MatrixError.MISSING_TOKEN)) {
                                    // No token or invalid token, stop the thread
                                    updateStateTo(SyncState.KILLING)
                                }

                                latch.countDown()
                            }

                        })
                        .executeBy(taskExecutor)

                 latch.await()
                if (state is SyncState.RUNNING) {
                    updateStateTo(SyncState.RUNNING(catchingUp = false))
                }

                Timber.v("Waiting for $DEFAULT_LONG_POOL_DELAY delay before new pool...")
                if (DEFAULT_LONG_POOL_DELAY > 0) sleep(DEFAULT_LONG_POOL_DELAY)
                Timber.v("...Continue")
            }
        }
        Timber.v("Sync killed")
        updateStateTo(SyncState.KILLED)
        backgroundDetectionObserver.unregister(this)
        networkConnectivityChecker.unregister(this)
    }

    private fun updateStateTo(newState: SyncState) {
        state = newState
        liveState.postValue(newState)
    }

    override fun onConnect() {
        synchronized(lock) {
            lock.notify()
        }
    }

    override fun onMoveToForeground() {
        restart()
    }

    override fun onMoveToBackground() {
        pause()
    }

}


