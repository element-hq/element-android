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
import im.vector.matrix.android.internal.session.sync.SyncTokenStore
import im.vector.matrix.android.internal.session.sync.model.SyncResponse
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.TaskThread
import im.vector.matrix.android.internal.task.configureWith
import timber.log.Timber
import java.net.SocketTimeoutException
import java.util.*

private const val DEFAULT_LONG_POOL_TIMEOUT = 10_000L
private const val BACKGROUND_LONG_POOL_TIMEOUT = 0L

/**
 * Can execute periodic sync task.
 * An IntentService is used in conjunction with the AlarmManager and a Broadcast Receiver
 * in order to be able to perform a sync even if the app is not running.
 * The <receiver> and <service> must be declared in the Manifest or the app using the SDK
 */
open class SyncService : Service() {

    private var mIsSelfDestroyed: Boolean = false
    private var cancelableTask: Cancelable? = null

    private lateinit var syncTokenStore: SyncTokenStore
    private lateinit var syncTask: SyncTask
    private lateinit var networkConnectivityChecker: NetworkConnectivityChecker
    private lateinit var taskExecutor: TaskExecutor


    var timer = Timer()

    var nextBatchDelay = 0L
    var timeout = 10_000L

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.i("onStartCommand ${intent}")
        nextBatchDelay = 60_000L
        timeout = 0
        intent?.let {
            val userId = it.getStringExtra(EXTRA_USER_ID)
            val sessionComponent = Matrix.getInstance(applicationContext).sessionManager.getSessionComponent(userId)
                    ?: return@let
            syncTokenStore = sessionComponent.syncTokenStore()
            syncTask = sessionComponent.syncTask()
            networkConnectivityChecker = sessionComponent.networkConnectivityChecker()
            taskExecutor = sessionComponent.taskExecutor()
            if (cancelableTask == null) {
                timer.cancel()
                timer = Timer()
                doSync(true)
            } else {
                //Already syncing ignore
                Timber.i("Received a start while was already syncking... ignore")
            }
        }
        //No intent just start the service, an alarm will should call with intent
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
        var nextBatch = syncTokenStore.getLastToken()
        if (!networkConnectivityChecker.isConnected()) {
            Timber.v("Sync is Paused. Waiting...")
            //TODO Retry in ?
            timer.schedule(object : TimerTask() {
                override fun run() {
                    doSync()
                }
            }, 5_000L)
        } else {
            Timber.v("Execute sync request with token $nextBatch and timeout $timeout")
            val params = SyncTask.Params(nextBatch, timeout)
            cancelableTask = syncTask.configureWith(params)
                    .callbackOn(TaskThread.CALLER)
                    .executeOn(TaskThread.CALLER)
                    .dispatchTo(object : MatrixCallback<SyncResponse> {
                        override fun onSuccess(data: SyncResponse) {
                            cancelableTask = null
                            nextBatch = data.nextBatch
                            syncTokenStore.saveToken(nextBatch)
                            if (!once) {
                                timer.schedule(object : TimerTask() {
                                    override fun run() {
                                        doSync()
                                    }
                                }, nextBatchDelay)
                            } else {
                                //stop
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
                                    && (failure.error.code == MatrixError.UNKNOWN_TOKEN || failure.error.code == MatrixError.MISSING_TOKEN)) {
                                // No token or invalid token, stop the thread
                                stopSelf()
                            }

                        }

                    })
                    .executeBy(taskExecutor)

        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        const val EXTRA_USER_ID = "EXTRA_USER_ID"
    }

}