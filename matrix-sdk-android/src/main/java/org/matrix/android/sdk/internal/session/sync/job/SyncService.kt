/*
 * Copyright 2019 New Vector Ltd
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
import org.matrix.android.sdk.api.Matrix
import org.matrix.android.sdk.api.failure.isTokenError
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.sync.SyncState
import org.matrix.android.sdk.internal.network.NetworkConnectivityChecker
import org.matrix.android.sdk.internal.session.sync.SyncTask
import org.matrix.android.sdk.internal.task.TaskExecutor
import org.matrix.android.sdk.internal.util.BackgroundDetectionObserver
import org.matrix.android.sdk.internal.util.MatrixCoroutineDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
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
        Timber.i("onStartCommand $intent")
        val isInit = initialize(intent)
        if (isInit) {
            onStart(isInitialSync)
            doSyncIfNotAlreadyRunning()
        } else {
            // We should start and stop as we have to ensure to call Service.startForeground()
            onStart(isInitialSync)
            stopMe()
        }
        // No intent just start the service, an alarm will should call with intent
        return START_STICKY
    }

    override fun onDestroy() {
        Timber.i("## onDestroy() : $this")
        if (!mIsSelfDestroyed) {
            Timber.w("## Destroy by the system : $this")
        }
        serviceScope.coroutineContext.cancelChildren()
        isRunning.set(false)
        super.onDestroy()
    }

    private fun stopMe() {
        mIsSelfDestroyed = true
        stopSelf()
    }

    private fun doSyncIfNotAlreadyRunning() {
        if (isRunning.get()) {
            Timber.i("Received a start while was already syncing... ignore")
        } else {
            isRunning.set(true)
            serviceScope.launch(coroutineDispatchers.io) {
                doSync()
            }
        }
    }

    private suspend fun doSync() {
        Timber.v("Execute sync request with timeout 0")
        val params = SyncTask.Params(TIME_OUT)
        try {
            syncTask.execute(params)
            // Start sync if we were doing an initial sync and the syncThread is not launched yet
            if (isInitialSync && session.getSyncState() == SyncState.Idle) {
                val isForeground = !backgroundDetectionObserver.isInBackground
                session.startSync(isForeground)
            }
            stopMe()
        } catch (throwable: Throwable) {
            Timber.e(throwable)
            if (throwable.isTokenError()) {
                stopMe()
            } else {
                Timber.v("Should be rescheduled to avoid wasting resources")
                sessionId?.also {
                    onRescheduleAsked(it, isInitialSync, delay = 10_000L)
                }
                stopMe()
            }
        }
    }

    private fun initialize(intent: Intent?): Boolean {
        if (intent == null) {
            return false
        }
        val matrix = Matrix.getInstance(applicationContext)
        val safeSessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: return false
        try {
            val sessionComponent = matrix.sessionManager.getSessionComponent(safeSessionId)
                    ?: throw IllegalStateException("You should have a session to make it work")
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
            Timber.e(exception, "An exception occurred during initialisation")
            return false
        }
    }

    abstract fun onStart(isInitialSync: Boolean)

    abstract fun onRescheduleAsked(sessionId: String, isInitialSync: Boolean, delay: Long)

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        const val EXTRA_SESSION_ID = "EXTRA_SESSION_ID"
        private const val TIME_OUT = 0L
    }
}
