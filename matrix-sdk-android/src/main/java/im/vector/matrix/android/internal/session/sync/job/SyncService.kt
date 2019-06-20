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
import android.os.Binder
import android.os.IBinder
import com.squareup.moshi.JsonEncodingException
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.failure.MatrixError
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.di.MatrixKoinComponent
import im.vector.matrix.android.internal.network.NetworkConnectivityChecker
import im.vector.matrix.android.internal.session.sync.SyncTask
import im.vector.matrix.android.internal.session.sync.SyncTokenStore
import im.vector.matrix.android.internal.session.sync.model.SyncResponse
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.TaskThread
import im.vector.matrix.android.internal.task.configureWith
import org.koin.standalone.inject
import timber.log.Timber
import java.net.SocketTimeoutException
import java.util.*
import kotlin.collections.ArrayList

private const val DEFAULT_LONG_POOL_TIMEOUT = 10_000L
private const val BACKGROUND_LONG_POOL_TIMEOUT = 0L

/**
 * Can execute periodic sync task.
 * An IntentService is used in conjunction with the AlarmManager and a Broadcast Receiver
 * in order to be able to perform a sync even if the app is not running.
 * The <receiver> and <service> must be declared in the Manifest or the app using the SDK
 */
open class SyncService : Service(), MatrixKoinComponent {

    private var mIsSelfDestroyed: Boolean = false
    private var cancelableTask: Cancelable? = null
    private val syncTokenStore: SyncTokenStore by inject()

    private val syncTask: SyncTask by inject()
    private val networkConnectivityChecker: NetworkConnectivityChecker by inject()
    private val taskExecutor: TaskExecutor by inject()

    private var localBinder = LocalBinder()

    var timer = Timer()

    var nextBatchDelay = 0L
    var timeout = 10_000L

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.i("onStartCommand ${intent}")
        nextBatchDelay  = 60_000L
        timeout = 0
        intent?.let {
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
                            localBinder.notifySyncFinish()
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
                            localBinder.notifyFailure(failure)
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

    override fun onBind(intent: Intent?): IBinder {
        return localBinder
    }

    inner class LocalBinder : Binder() {

        private var listeners = ArrayList<SyncListener>()

        fun addListener(listener: SyncListener) {
            if (!listeners.contains(listener)) {
                listeners.add(listener)
            }
        }

        fun removeListener(listener: SyncListener) {
            listeners.remove(listener)
        }

        internal fun notifySyncFinish() {

            listeners.forEach {
                try {
                    it.onSyncFinsh()
                } catch (t: Throwable) {
                    Timber.e("Failed to notify listener $it")
                }
            }
        }

        internal fun notifyNetworkNotAvailable() {
            listeners.forEach {
                try {
                    it.networkNotAvailable()
                } catch (t: Throwable) {
                    Timber.e("Failed to notify listener $it")
                }
            }
        }

        internal fun notifyFailure(throwable: Throwable) {

            listeners.forEach {
                try {
                    it.onFailed(throwable)
                } catch (t: Throwable) {
                    Timber.e("Failed to notify listener $it")
                }
            }

        }

        fun getService(): SyncService = this@SyncService

    }

    interface SyncListener {
        fun onSyncFinsh()
        fun networkNotAvailable()
        fun onFailed(throwable: Throwable)
    }

    companion object {

        fun startLongPool(delay: Long) {

        }
    }
}