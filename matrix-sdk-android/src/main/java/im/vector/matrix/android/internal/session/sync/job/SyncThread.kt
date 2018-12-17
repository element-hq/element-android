package im.vector.matrix.android.internal.session.sync.job

import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.task.TaskExecutor
import im.vector.matrix.android.internal.task.TaskThread
import im.vector.matrix.android.internal.task.configureWith
import im.vector.matrix.android.internal.network.NetworkConnectivityChecker
import im.vector.matrix.android.internal.session.sync.SyncTask
import im.vector.matrix.android.internal.session.sync.SyncTokenStore
import im.vector.matrix.android.internal.session.sync.model.SyncResponse
import im.vector.matrix.android.internal.util.BackgroundDetectionObserver
import timber.log.Timber
import java.util.concurrent.CountDownLatch

private const val RETRY_WAIT_TIME_MS = 10_000L

internal class SyncThread(private val syncTask: SyncTask,
                          private val networkConnectivityChecker: NetworkConnectivityChecker,
                          private val syncTokenStore: SyncTokenStore,
                          private val backgroundDetectionObserver: BackgroundDetectionObserver,
                          private val taskExecutor: TaskExecutor
) : Thread(), NetworkConnectivityChecker.Listener, BackgroundDetectionObserver.Listener {

    enum class State {
        IDLE,
        RUNNING,
        PAUSED,
        KILLING,
        KILLED
    }

    private var state: State = State.IDLE
    private val lock = Object()
    private var nextBatch = syncTokenStore.getLastToken()
    private var cancelableTask: Cancelable? = null

    fun restart() {
        synchronized(lock) {
            if (state != State.PAUSED) {
                return@synchronized
            }
            Timber.v("Unpause sync...")
            state = State.RUNNING
            lock.notify()
        }
    }

    fun pause() {
        synchronized(lock) {
            if (state != State.RUNNING) {
                return@synchronized
            }
            Timber.v("Pause sync...")
            state = State.PAUSED
        }
    }

    fun kill() {
        synchronized(lock) {
            Timber.v("Kill sync...")
            state = State.KILLING
            cancelableTask?.cancel()
            lock.notify()
        }
    }


    override fun run() {
        Timber.v("Start syncing...")
        networkConnectivityChecker.register(this)
        backgroundDetectionObserver.register(this)
        state = State.RUNNING
        while (state != State.KILLING) {
            if (!networkConnectivityChecker.isConnected() || state == State.PAUSED) {
                Timber.v("Waiting...")
                synchronized(lock) {
                    lock.wait()
                }
            } else {
                Timber.v("Execute sync request...")
                val latch = CountDownLatch(1)
                val params = SyncTask.Params(nextBatch)
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
                                if (failure !is Failure.NetworkConnection) {
                                    // Wait 10s before retrying
                                    sleep(RETRY_WAIT_TIME_MS)
                                }
                                latch.countDown()
                            }

                        })
                        .executeBy(taskExecutor)

                latch.await()
            }
        }
        Timber.v("Sync killed")
        state = State.KILLED
        backgroundDetectionObserver.unregister(this)
        networkConnectivityChecker.unregister(this)
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


