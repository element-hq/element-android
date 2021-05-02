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

package org.matrix.android.sdk.internal.session.sync.job

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.squareup.moshi.JsonEncodingException
import kotlinx.coroutines.CancellationException
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.failure.isTokenError
import org.matrix.android.sdk.api.session.sync.SyncState
import org.matrix.android.sdk.internal.network.NetworkConnectivityChecker
import org.matrix.android.sdk.internal.session.sync.SyncTask
import org.matrix.android.sdk.internal.util.BackgroundDetectionObserver
import org.matrix.android.sdk.internal.util.Debouncer
import org.matrix.android.sdk.internal.util.createUIHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.matrix.android.sdk.api.session.call.MxCall
import org.matrix.android.sdk.internal.session.call.ActiveCallHandler
import org.matrix.android.sdk.internal.session.sync.SyncPresence
import timber.log.Timber
import java.net.SocketTimeoutException
import java.util.Timer
import java.util.TimerTask
import javax.inject.Inject
import kotlin.concurrent.schedule

private const val RETRY_WAIT_TIME_MS = 10_000L
private const val DEFAULT_LONG_POOL_TIMEOUT = 30_000L

internal class SyncThread @Inject constructor(private val syncTask: SyncTask,
                                              private val networkConnectivityChecker: NetworkConnectivityChecker,
                                              private val backgroundDetectionObserver: BackgroundDetectionObserver,
                                              private val activeCallHandler: ActiveCallHandler
) : Thread("SyncThread"), NetworkConnectivityChecker.Listener, BackgroundDetectionObserver.Listener {

    private var state: SyncState = SyncState.Idle
    private var liveState = MutableLiveData(state)
    private val lock = Object()
    private val syncScope = CoroutineScope(SupervisorJob())
    private val debouncer = Debouncer(createUIHandler())

    private var canReachServer = true
    private var isStarted = false
    private var isTokenValid = true
    private var retryNoNetworkTask: TimerTask? = null

    private val activeCallListObserver = Observer<MutableList<MxCall>> { activeCalls ->
        if (activeCalls.isEmpty() && backgroundDetectionObserver.isInBackground) {
            pause()
        }
    }

    init {
        updateStateTo(SyncState.Idle)
    }

    fun setInitialForeground(initialForeground: Boolean) {
        val newState = if (initialForeground) SyncState.Idle else SyncState.Paused
        updateStateTo(newState)
    }

    fun restart() = synchronized(lock) {
        if (!isStarted) {
            Timber.v("Resume sync...")
            isStarted = true
            // Check again server availability and the token validity
            canReachServer = true
            isTokenValid = true
            lock.notify()
        }
    }

    fun pause() = synchronized(lock) {
        if (isStarted) {
            Timber.v("Pause sync...")
            isStarted = false
            retryNoNetworkTask?.cancel()
            syncScope.coroutineContext.cancelChildren()
        }
    }

    fun kill() = synchronized(lock) {
        Timber.v("Kill sync...")
        updateStateTo(SyncState.Killing)
        retryNoNetworkTask?.cancel()
        syncScope.coroutineContext.cancelChildren()
        lock.notify()
    }

    fun currentState() = state

    fun liveState(): LiveData<SyncState> {
        return liveState
    }

    override fun onConnectivityChanged() {
        retryNoNetworkTask?.cancel()
        synchronized(lock) {
            canReachServer = true
            lock.notify()
        }
    }

    override fun run() {
        Timber.v("Start syncing...")

        isStarted = true
        networkConnectivityChecker.register(this)
        backgroundDetectionObserver.register(this)
        registerActiveCallsObserver()
        while (state != SyncState.Killing) {
            Timber.v("Entering loop, state: $state")
            if (!isStarted) {
                Timber.v("Sync is Paused. Waiting...")
                updateStateTo(SyncState.Paused)
                synchronized(lock) { lock.wait() }
                Timber.v("...unlocked")
            } else if (!canReachServer) {
                Timber.v("No network. Waiting...")
                updateStateTo(SyncState.NoNetwork)
                // We force retrying in RETRY_WAIT_TIME_MS maximum. Otherwise it will be unlocked by onConnectivityChanged() or restart()
                retryNoNetworkTask = Timer(SyncState.NoNetwork.toString(), false).schedule(RETRY_WAIT_TIME_MS) {
                    synchronized(lock) {
                        canReachServer = true
                        lock.notify()
                    }
                }
                synchronized(lock) { lock.wait() }
                Timber.v("...retry")
            } else if (!isTokenValid) {
                Timber.v("Token is invalid. Waiting...")
                updateStateTo(SyncState.InvalidToken)
                synchronized(lock) { lock.wait() }
                Timber.v("...unlocked")
            } else {
                if (state !is SyncState.Running) {
                    updateStateTo(SyncState.Running(afterPause = true))
                }
                // No timeout after a pause
                val timeout = state.let { if (it is SyncState.Running && it.afterPause) 0 else DEFAULT_LONG_POOL_TIMEOUT }
                Timber.v("Execute sync request with timeout $timeout")
                val params = SyncTask.Params(timeout, SyncPresence.Online)
                val sync = syncScope.launch {
                    doSync(params)
                }
                runBlocking {
                    sync.join()
                }
                Timber.v("...Continue")
            }
        }
        Timber.v("Sync killed")
        updateStateTo(SyncState.Killed)
        backgroundDetectionObserver.unregister(this)
        networkConnectivityChecker.unregister(this)
        unregisterActiveCallsObserver()
    }

    private fun registerActiveCallsObserver() {
        syncScope.launch(Dispatchers.Main) {
            activeCallHandler.getActiveCallsLiveData().observeForever(activeCallListObserver)
        }
    }

    private fun unregisterActiveCallsObserver() {
        syncScope.launch(Dispatchers.Main) {
            activeCallHandler.getActiveCallsLiveData().removeObserver(activeCallListObserver)
        }
    }

    private suspend fun doSync(params: SyncTask.Params) {
        try {
            syncTask.execute(params)
        } catch (failure: Throwable) {
            if (failure is Failure.NetworkConnection) {
                canReachServer = false
            }
            if (failure is Failure.NetworkConnection && failure.cause is SocketTimeoutException) {
                // Timeout are not critical
                Timber.v("Timeout")
            } else if (failure is CancellationException) {
                Timber.v("Cancelled")
            } else if (failure.isTokenError()) {
                // No token or invalid token, stop the thread
                Timber.w(failure, "Token error")
                isStarted = false
                isTokenValid = false
            } else {
                Timber.e(failure)
                if (failure !is Failure.NetworkConnection || failure.cause is JsonEncodingException) {
                    // Wait 10s before retrying
                    Timber.v("Wait 10s")
                    delay(RETRY_WAIT_TIME_MS)
                }
            }
        } finally {
            state.let {
                if (it is SyncState.Running && it.afterPause) {
                    updateStateTo(SyncState.Running(afterPause = false))
                }
            }
        }
    }

    private fun updateStateTo(newState: SyncState) {
        Timber.v("Update state from $state to $newState")
        if (newState == state) {
            return
        }
        state = newState
        debouncer.debounce("post_state", {
            liveState.value = newState
        }, 150)
    }

    override fun onMoveToForeground() {
        restart()
    }

    override fun onMoveToBackground() {
        if (activeCallHandler.getActiveCallsLiveData().value.isNullOrEmpty()) {
            pause()
        }
    }
}
