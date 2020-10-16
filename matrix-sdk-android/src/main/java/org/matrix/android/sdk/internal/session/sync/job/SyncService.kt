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

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.content.getSystemService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.Matrix
import org.matrix.android.sdk.api.failure.Failure
import org.matrix.android.sdk.api.failure.isTokenError
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.sync.SyncState
import org.matrix.android.sdk.internal.network.NetworkConnectivityChecker
import org.matrix.android.sdk.internal.session.sync.SyncPresence
import org.matrix.android.sdk.internal.session.sync.SyncTask
import org.matrix.android.sdk.internal.task.TaskExecutor
import org.matrix.android.sdk.internal.util.BackgroundDetectionObserver
import org.matrix.android.sdk.internal.util.MatrixCoroutineDispatchers
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Can execute periodic sync task.
 * An IntentService is used in conjunction with the AlarmManager and a Broadcast Receiver
 * in order to be able to perform a sync even if the app is not running.
 * The <receiver> and <service> must be declared in the Manifest or the app using the SDK
 */
abstract class SyncService : Service() {

    private var sessionId: String? = null
    private var mIsSelfDestroyed: Boolean = false

    private var syncTimeoutSeconds: Int = 6
    private var syncDelaySeconds: Int = 60
    private var periodic: Boolean = false
    private var preventReschedule: Boolean = false

    private var isInitialSync: Boolean = false
    private lateinit var session: Session
    private lateinit var syncTask: SyncTask
    private lateinit var networkConnectivityChecker: NetworkConnectivityChecker
    private lateinit var taskExecutor: TaskExecutor
    private lateinit var coroutineDispatchers: MatrixCoroutineDispatchers
    private lateinit var backgroundDetectionObserver: BackgroundDetectionObserver

    private val isRunning = AtomicBoolean(false)

    private val serviceScope = CoroutineScope(SupervisorJob())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.i("## Sync: onStartCommand [$this] $intent with action: ${intent?.action}")

        // We should start we have to ensure we fulfill contract to show notification
        // for foreground service (as per design for this service)
        // TODO can we check if it's really in foreground
        onStart(isInitialSync)
        when (intent?.action) {
            ACTION_STOP -> {
                Timber.i("## Sync: stop command received")
                // If it was periodic we ensure that it will not reschedule itself
                preventReschedule = true
                // we don't want to cancel initial syncs, let it finish
                if (!isInitialSync) {
                    stopMe()
                }
            }
            else        -> {
                val isInit = initialize(intent)
                if (isInit) {
                    periodic = intent?.getBooleanExtra(EXTRA_PERIODIC, false) ?: false
                    val onNetworkBack = intent?.getBooleanExtra(EXTRA_NETWORK_BACK_RESTART, false) ?: false
                    Timber.d("## Sync: command received, periodic: $periodic  networkBack: $onNetworkBack")
                    if (onNetworkBack && !backgroundDetectionObserver.isInBackground) {
                        // the restart after network occurs while the app is in foreground
                        // so just stop. It will be restarted when entering background
                        preventReschedule = true
                        stopMe()
                    } else {
                        // default is syncing
                        doSyncIfNotAlreadyRunning()
                    }
                } else {
                    Timber.d("## Sync: Failed to initialize service")
                    stopMe()
                }
            }
        }

        // It's ok to be not sticky because we will explicitly start it again on the next alarm?
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Timber.i("## Sync: onDestroy() [$this] periodic:$periodic preventReschedule:$preventReschedule")
        if (!mIsSelfDestroyed) {
            Timber.d("## Sync: Destroy by the system : $this")
        }
        isRunning.set(false)
        // Cancelling the context will trigger the catch close the doSync try
        serviceScope.coroutineContext.cancelChildren()
        if (!preventReschedule && periodic && sessionId != null && backgroundDetectionObserver.isInBackground) {
            Timber.d("## Sync: Reschedule service in $syncDelaySeconds sec")
            onRescheduleAsked(sessionId ?: "", false, syncTimeoutSeconds, syncDelaySeconds)
        }
        super.onDestroy()
    }

    private fun stopMe() {
        mIsSelfDestroyed = true
        stopSelf()
    }

    private fun doSyncIfNotAlreadyRunning() {
        if (isRunning.get()) {
            Timber.i("## Sync: Received a start while was already syncing... ignore")
        } else {
            isRunning.set(true)
            // Acquire a lock to give enough time for the sync :/
            getSystemService<PowerManager>()?.run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "riotx:fdroidSynclock").apply {
                    acquire((syncTimeoutSeconds * 1000L + 10_000L))
                }
            }
            serviceScope.launch(coroutineDispatchers.io) {
                doSync()
            }
        }
    }

    private suspend fun doSync() {
        Timber.v("## Sync: Execute sync request with timeout $syncTimeoutSeconds seconds")
        val params = SyncTask.Params(syncTimeoutSeconds * 1000L, SyncPresence.Offline)
        try {
            // never do that in foreground, let the syncThread work
            syncTask.execute(params)
            // Start sync if we were doing an initial sync and the syncThread is not launched yet
            if (isInitialSync && session.getSyncState() == SyncState.Idle) {
                val isForeground = !backgroundDetectionObserver.isInBackground
                session.startSync(isForeground)
            }
            stopMe()
        } catch (throwable: Throwable) {
            Timber.e(throwable, "## Sync: sync service did fail ${isRunning.get()}")
            if (throwable.isTokenError()) {
                // no need to retry
                preventReschedule = true
            }
            if (throwable is Failure.NetworkConnection) {
                // Network is off, no need to reschedule endless alarms :/
                preventReschedule = true
                // Instead start a work to restart background sync when network is back
                onNetworkError(sessionId ?: "", isInitialSync, syncTimeoutSeconds, syncDelaySeconds)
            }
            // JobCancellation could be caught here when onDestroy cancels the coroutine context
            if (isRunning.get()) stopMe()
        }
    }

    private fun initialize(intent: Intent?): Boolean {
        if (intent == null) {
            Timber.d("## Sync: initialize intent is null")
            return false
        }
        val matrix = Matrix.getInstance(applicationContext)
        val safeSessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: return false
        syncTimeoutSeconds = intent.getIntExtra(EXTRA_TIMEOUT_SECONDS, 6)
        syncDelaySeconds = intent.getIntExtra(EXTRA_DELAY_SECONDS, 60)
        try {
            val sessionComponent = matrix.sessionManager.getSessionComponent(safeSessionId)
                    ?: throw IllegalStateException("## Sync: You should have a session to make it work")
            session = sessionComponent.session()
            sessionId = safeSessionId
            syncTask = sessionComponent.syncTask()
            isInitialSync = !session.hasAlreadySynced()
            networkConnectivityChecker = sessionComponent.networkConnectivityChecker()
            taskExecutor = sessionComponent.taskExecutor()
            coroutineDispatchers = sessionComponent.coroutineDispatchers()
            backgroundDetectionObserver = matrix.backgroundDetectionObserver
            return true
        } catch (exception: Exception) {
            Timber.e(exception, "## Sync: An exception occurred during initialisation")
            return false
        }
    }

    abstract fun onStart(isInitialSync: Boolean)

    abstract fun onRescheduleAsked(sessionId: String, isInitialSync: Boolean, timeout: Int, delay: Int)

    abstract fun onNetworkError(sessionId: String, isInitialSync: Boolean, timeout: Int, delay: Int)

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        const val EXTRA_SESSION_ID = "EXTRA_SESSION_ID"
        const val EXTRA_TIMEOUT_SECONDS = "EXTRA_TIMEOUT_SECONDS"
        const val EXTRA_DELAY_SECONDS = "EXTRA_DELAY_SECONDS"
        const val EXTRA_PERIODIC = "EXTRA_PERIODIC"
        const val EXTRA_NETWORK_BACK_RESTART = "EXTRA_NETWORK_BACK_RESTART"

        const val ACTION_STOP = "ACTION_STOP"
    }
}
