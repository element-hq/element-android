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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.failure.isTokenError
import org.matrix.android.sdk.api.logger.LoggerTag
import org.matrix.android.sdk.api.session.call.MxCall
import org.matrix.android.sdk.api.session.sync.SyncState
import org.matrix.android.sdk.api.session.sync.model.SyncResponse
import org.matrix.android.sdk.internal.network.NetworkConnectivityChecker
import org.matrix.android.sdk.internal.session.call.ActiveCallHandler
import org.matrix.android.sdk.internal.session.sync.SyncTask
import org.matrix.android.sdk.internal.settings.DefaultLightweightSettingsStorage
import org.matrix.android.sdk.internal.util.BackgroundDetectionObserver
import org.matrix.android.sdk.internal.util.Debouncer
import org.matrix.android.sdk.internal.util.createUIHandler
import timber.log.Timber
import java.net.SocketTimeoutException
import java.util.Timer
import java.util.TimerTask
import javax.inject.Inject
import kotlin.concurrent.schedule

private const val RETRY_WAIT_TIME_MS = 10_000L
private const val DEFAULT_LONG_POOL_TIMEOUT = 30_000L

private val loggerTag = LoggerTag("SyncThread", LoggerTag.SYNC)

internal class SyncThread @Inject constructor(private val syncTask: SyncTask,
                                              private val networkConnectivityChecker: NetworkConnectivityChecker,
                                              private val backgroundDetectionObserver: BackgroundDetectionObserver,
                                              private val activeCallHandler: ActiveCallHandler,
                                              private val lightweightSettingsStorage: DefaultLightweightSettingsStorage
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
    private var previousSyncResponseHasToDevice = false

    private val activeCallListObserver = Observer<MutableList<MxCall>> { activeCalls ->
        if (activeCalls.isEmpty() && backgroundDetectionObserver.isInBackground) {
            pause()
        }
    }

    private val _syncFlow = MutableSharedFlow<SyncResponse>()

    init {
        updateStateTo(SyncState.Idle)
    }

    fun setInitialForeground(initialForeground: Boolean) {
        val newState = if (initialForeground) SyncState.Idle else SyncState.Paused
        updateStateTo(newState)
    }

    fun restart() = synchronized(lock) {
        if (!isStarted) {
            Timber.tag(loggerTag.value).d("Resume sync...")
            isStarted = true
            // Check again server availability and the token validity
            canReachServer = true
            isTokenValid = true
            lock.notify()
        }
    }

    fun pause() = synchronized(lock) {
        if (isStarted) {
            Timber.tag(loggerTag.value).d("Pause sync... Not cancelling incremental sync")
            isStarted = false
            retryNoNetworkTask?.cancel()
            // Do not cancel the current incremental sync.
            // Incremental sync can be long and it requires the user to wait for the treatment to end,
            // else all is restarted from the beginning each time the user moves the app to foreground.
        }
    }

    fun kill() = synchronized(lock) {
        Timber.tag(loggerTag.value).d("Kill sync...")
        updateStateTo(SyncState.Killing)
        retryNoNetworkTask?.cancel()
        syncScope.coroutineContext.cancelChildren()
        lock.notify()
    }

    fun currentState() = state

    fun liveState(): LiveData<SyncState> {
        return liveState
    }

    fun syncFlow(): SharedFlow<SyncResponse> = _syncFlow

    override fun onConnectivityChanged() {
        retryNoNetworkTask?.cancel()
        synchronized(lock) {
            canReachServer = true
            lock.notify()
        }
    }

    override fun run() {
        Timber.tag(loggerTag.value).d("Start syncing...")

        isStarted = true
        networkConnectivityChecker.register(this)
        backgroundDetectionObserver.register(this)
        registerActiveCallsObserver()
        while (state != SyncState.Killing) {
            Timber.tag(loggerTag.value).d("Entering loop, state: $state")
            if (!isStarted) {
                Timber.tag(loggerTag.value).d("Sync is Paused. Waiting...")
                updateStateTo(SyncState.Paused)
                synchronized(lock) { lock.wait() }
                Timber.tag(loggerTag.value).d("...unlocked")
            } else if (!canReachServer) {
                Timber.tag(loggerTag.value).d("No network. Waiting...")
                updateStateTo(SyncState.NoNetwork)
                // We force retrying in RETRY_WAIT_TIME_MS maximum. Otherwise it will be unlocked by onConnectivityChanged() or restart()
                retryNoNetworkTask = Timer(SyncState.NoNetwork.toString(), false).schedule(RETRY_WAIT_TIME_MS) {
                    synchronized(lock) {
                        canReachServer = true
                        lock.notify()
                    }
                }
                synchronized(lock) { lock.wait() }
                Timber.tag(loggerTag.value).d("...retry")
            } else if (!isTokenValid) {
                if (state == SyncState.Killing) {
                    continue
                }
                Timber.tag(loggerTag.value).d("Token is invalid. Waiting...")
                updateStateTo(SyncState.InvalidToken)
                synchronized(lock) { lock.wait() }
                Timber.tag(loggerTag.value).d("...unlocked")
            } else {
                if (state !is SyncState.Running) {
                    updateStateTo(SyncState.Running(afterPause = true))
                }
                val afterPause = state.let { it is SyncState.Running && it.afterPause }
                val timeout = when {
                    previousSyncResponseHasToDevice -> 0L /* Force timeout to 0 */
                    afterPause                      -> 0L /* No timeout after a pause */
                    else                            -> DEFAULT_LONG_POOL_TIMEOUT
                }
                Timber.tag(loggerTag.value).d("Execute sync request with timeout $timeout")
                val presence = lightweightSettingsStorage.getSyncPresenceStatus()
                val params = SyncTask.Params(timeout, presence, afterPause = afterPause)
                val sync = syncScope.launch {
                    previousSyncResponseHasToDevice = doSync(params)
                }
                runBlocking {
                    sync.join()
                }
                Timber.tag(loggerTag.value).d("...Continue")
            }
        }
        Timber.tag(loggerTag.value).d("Sync killed")
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

    /**
     * Will return true if the sync response contains some toDevice events.
     */
    private suspend fun doSync(params: SyncTask.Params): Boolean {
        return try {
            val syncResponse = syncTask.execute(params)
            _syncFlow.emit(syncResponse)
            syncResponse.toDevice?.events?.isNotEmpty().orFalse()
        } catch (failure: Throwable) {
            if (failure is Failure.NetworkConnection) {
                canReachServer = false
            }
            if (failure is Failure.NetworkConnection && failure.cause is SocketTimeoutException) {
                // Timeout are not critical
                Timber.tag(loggerTag.value).d("Timeout")
            } else if (failure is CancellationException) {
                Timber.tag(loggerTag.value).d("Cancelled")
            } else if (failure.isTokenError()) {
                // No token or invalid token, stop the thread
                Timber.tag(loggerTag.value).w(failure, "Token error")
                isStarted = false
                isTokenValid = false
            } else {
                Timber.tag(loggerTag.value).e(failure)
                if (failure !is Failure.NetworkConnection || failure.cause is JsonEncodingException) {
                    // Wait 10s before retrying
                    Timber.tag(loggerTag.value).d("Wait 10s")
                    delay(RETRY_WAIT_TIME_MS)
                }
            }
            false
        } finally {
            state.let {
                if (it is SyncState.Running && it.afterPause) {
                    updateStateTo(SyncState.Running(afterPause = false))
                }
            }
        }
    }

    private fun updateStateTo(newState: SyncState) {
        Timber.tag(loggerTag.value).d("Update state from $state to $newState")
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
