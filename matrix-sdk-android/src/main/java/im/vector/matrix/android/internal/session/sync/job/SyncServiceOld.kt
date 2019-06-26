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
open class SyncServiceOld : Service(), MatrixKoinComponent {

    private var mIsSelfDestroyed: Boolean = false
    private var cancelableTask: Cancelable? = null
    private val syncTokenStore: SyncTokenStore by inject()

    private val syncTask: SyncTask by inject()
    private val networkConnectivityChecker: NetworkConnectivityChecker by inject()
    private val taskExecutor: TaskExecutor by inject()

    private var localBinder = LocalBinder()

    val timer = Timer()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.i("onStartCommand ${intent}")
        intent?.let {
            if (cancelableTask == null) {
                doSync(BACKGROUND_LONG_POOL_TIMEOUT)
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
        cancelableTask?.cancel()
        mIsSelfDestroyed = true
        stopSelf()
    }

    fun doSync(currentLongPoolTimeoutMs: Long = DEFAULT_LONG_POOL_TIMEOUT) {
        var nextBatch = syncTokenStore.getLastToken()
        if (!networkConnectivityChecker.isConnected()) {
            Timber.v("Sync is Paused. Waiting...")
            //TODO Retry in ?
        } else {
            Timber.v("Execute sync request with token $nextBatch and timeout $currentLongPoolTimeoutMs")
            val params = SyncTask.Params(nextBatch, currentLongPoolTimeoutMs)
            cancelableTask = syncTask.configureWith(params)
                    .callbackOn(TaskThread.CALLER)
                    .executeOn(TaskThread.CALLER)
                    .dispatchTo(object : MatrixCallback<SyncResponse> {
                        override fun onSuccess(data: SyncResponse) {
                            cancelableTask = null
                            nextBatch = data.nextBatch
                            syncTokenStore.saveToken(nextBatch)
                            localBinder.notifySyncFinish()
                        }

                        override fun onFailure(failure: Throwable) {
                            Timber.e(failure)
                            cancelableTask = null
                            localBinder.notifyFailure(failure)
//                            if (failure is Failure.NetworkConnection
//                                    && failure.cause is SocketTimeoutException) {
//                                // Timeout are not critical
//                                localBinder.notifyFailure()
//                            }
//
//                            if (failure !is Failure.NetworkConnection
//                                    || failure.cause is JsonEncodingException) {
//                                // Wait 10s before retrying
////                                Thread.sleep(RETRY_WAIT_TIME_MS)
//                                //TODO Retry in 10S?
//                            }
//
//                            if (failure is Failure.ServerError
//                                    && (failure.error.code == MatrixError.UNKNOWN_TOKEN || failure.error.code == MatrixError.MISSING_TOKEN)) {
//                                // No token or invalid token, stop the thread
//                                stopSelf()
//                            }

                        }

                    })
                    .executeBy(taskExecutor)

            //TODO return and schedule a new one?
//            Timber.v("Waiting for $currentLongPoolDelayMs delay before new pool...")
//            if (currentLongPoolDelayMs > 0) Thread.sleep(currentLongPoolDelayMs)
//            Timber.v("...Continue")
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

        fun getService(): SyncServiceOld = this@SyncServiceOld

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