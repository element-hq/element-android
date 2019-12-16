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

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.squareup.moshi.JsonEncodingException
import im.vector.matrix.android.api.Matrix
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.failure.MatrixError
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.network.NetworkConnectivityChecker
import im.vector.matrix.android.internal.session.sync.SyncTask
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.TaskThread
import im.vector.matrix.android.internal.task.configureWith
import timber.log.Timber
import java.net.SocketTimeoutException
import java.util.Timer
import java.util.TimerTask

/**
 * Can execute periodic sync task.
 * An IntentService is used in conjunction with the AlarmManager and a Broadcast Receiver
 * in order to be able to perform a sync even if the app is not running.
 * The <receiver> and <service> must be declared in the Manifest or the app using the SDK
 */
open class SyncService : Service() {

    private var mIsSelfDestroyed: Boolean = false
    private var cancelableTask: Cancelable? = null

    private lateinit var syncTask: SyncTask
    private lateinit var networkConnectivityChecker: NetworkConnectivityChecker
    private lateinit var taskExecutor: TaskExecutor

    var timer = Timer()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.i("onStartCommand $intent")
        intent?.let {
            val userId = it.getStringExtra(EXTRA_USER_ID)
            val sessionComponent = Matrix.getInstance(applicationContext).sessionManager.getSessionComponent(userId)
                    ?: return@let
            syncTask = sessionComponent.syncTask()
            networkConnectivityChecker = sessionComponent.networkConnectivityChecker()
            taskExecutor = sessionComponent.taskExecutor()
            if (cancelableTask == null) {
                timer.cancel()
                timer = Timer()
                doSync(true)
            } else {
                // Already syncing ignore
                Timber.i("Received a start while was already syncking... ignore")
            }
        }
        // No intent just start the service, an alarm will should call with intent
        return START_STICKY
    }

    override fun onDestroy() {
        Timber.i("## onDestroy() : $this")

        if (!mIsSelfDestroyed) {
            Timber.w("## Destroy by the system : $this")
        }

        cancelableTask?.cancel()
        super.onDestroy()
    }

    fun stopMe() {
        timer.cancel()
        timer = Timer()
        cancelableTask?.cancel()
        mIsSelfDestroyed = true
        stopSelf()
    }

    fun doSync(once: Boolean = false) {
        if (!networkConnectivityChecker.hasInternetAccess) {
            Timber.v("No internet access. Waiting...")
            // TODO Retry in ?
            timer.schedule(object : TimerTask() {
                override fun run() {
                    doSync()
                }
            }, NO_NETWORK_DELAY)
        } else {
            Timber.v("Execute sync request with timeout 0")
            val params = SyncTask.Params(TIME_OUT)
            cancelableTask = syncTask
                    .configureWith(params) {
                        callbackThread = TaskThread.SYNC
                        executionThread = TaskThread.SYNC
                        callback = object : MatrixCallback<Unit> {
                            override fun onSuccess(data: Unit) {
                                cancelableTask = null
                                if (!once) {
                                    timer.schedule(object : TimerTask() {
                                        override fun run() {
                                            doSync()
                                        }
                                    }, NEXT_BATCH_DELAY)
                                } else {
                                    // stop
                                    stopMe()
                                }
                            }

                            override fun onFailure(failure: Throwable) {
                                Timber.e(failure)
                                cancelableTask = null
                                if (failure is Failure.NetworkConnection
                                        && failure.cause is SocketTimeoutException) {
                                    // Timeout are not critical
                                    timer.schedule(object : TimerTask() {
                                        override fun run() {
                                            doSync()
                                        }
                                    }, 5_000L)
                                }

                                if (failure !is Failure.NetworkConnection
                                        || failure.cause is JsonEncodingException) {
                                    // Wait 10s before retrying
                                    timer.schedule(object : TimerTask() {
                                        override fun run() {
                                            doSync()
                                        }
                                    }, 5_000L)
                                }

                                if (failure is Failure.ServerError
                                        && (failure.error.code == MatrixError.M_UNKNOWN_TOKEN || failure.error.code == MatrixError.M_MISSING_TOKEN)) {
                                    // No token or invalid token, stop the thread
                                    stopSelf()
                                }
                            }
                        }
                    }
                    .executeBy(taskExecutor)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        const val EXTRA_USER_ID = "EXTRA_USER_ID"

        const val TIME_OUT = 0L
        const val NEXT_BATCH_DELAY = 60_000L
        const val NO_NETWORK_DELAY = 5_000L
    }
}
