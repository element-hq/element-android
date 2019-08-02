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
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.TaskThread
import im.vector.matrix.android.internal.task.configureWith
import im.vector.matrix.android.internal.util.BackgroundDetectionObserver
import timber.log.Timber
import java.net.SocketTimeoutException
import java.util.concurrent.CountDownLatch
import javax.inject.Inject

private const val RETRY_WAIT_TIME_MS = 10_000L
private const val DEFAULT_LONG_POOL_TIMEOUT = 30_000L

internal class SyncThread @Inject constructor(private val syncTask: SyncTask,
                                              private val networkConnectivityChecker: NetworkConnectivityChecker,
                                              private val backgroundDetectionObserver: BackgroundDetectionObserver,
                                              private val taskExecutor: TaskExecutor
) : Thread(), NetworkConnectivityChecker.Listener, BackgroundDetectionObserver.Listener {

    private var state: SyncState = SyncState.IDLE
    private var liveState = MutableLiveData<SyncState>()
    private val lock = Object()
    private var cancelableTask: Cancelable? = null

    init {
        updateStateTo(SyncState.IDLE)
    }

    fun setInitialForeground(initialForeground: Boolean) {
        val newState = if (initialForeground) SyncState.IDLE else SyncState.PAUSED
        updateStateTo(newState)
    }

    fun restart() = synchronized(lock) {
        if (state is SyncState.PAUSED) {
            Timber.v("Resume sync...")
            updateStateTo(SyncState.RUNNING(afterPause = true))
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

        while (state != SyncState.KILLING) {
            if (!networkConnectivityChecker.isConnected() || state == SyncState.PAUSED) {
                Timber.v("Sync is Paused. Waiting...")
                synchronized(lock) {
                    lock.wait()
                }
            } else {
                if (state !is SyncState.RUNNING) {
                    updateStateTo(SyncState.RUNNING(afterPause = true))
                }
                Timber.v("[$this] Execute sync request with timeout $DEFAULT_LONG_POOL_TIMEOUT")
                val latch = CountDownLatch(1)
                val params = SyncTask.Params(DEFAULT_LONG_POOL_TIMEOUT)
                cancelableTask = syncTask.configureWith(params)
                        .callbackOn(TaskThread.SYNC)
                        .executeOn(TaskThread.SYNC)
                        .dispatchTo(object : MatrixCallback<Unit> {
                            override fun onSuccess(data: Unit) {
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
                    updateStateTo(SyncState.RUNNING(afterPause = false))
                }

                Timber.v("...Continue")
            }
        }
        Timber.v("Sync killed")
        updateStateTo(SyncState.KILLED)
        backgroundDetectionObserver.unregister(this)
        networkConnectivityChecker.unregister(this)
    }

    private fun updateStateTo(newState: SyncState) {
        Timber.v("Update state to $newState")
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


