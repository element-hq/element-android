package im.vector.matrix.android.internal.events.sync.job

import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.failure.Failure
import im.vector.matrix.android.api.util.Cancelable
import im.vector.matrix.android.internal.events.sync.SyncRequest
import im.vector.matrix.android.internal.events.sync.SyncTokenStore
import im.vector.matrix.android.internal.events.sync.data.SyncResponse
import im.vector.matrix.android.internal.network.NetworkConnectivityChecker
import timber.log.Timber
import java.util.concurrent.CountDownLatch

private const val RETRY_WAIT_TIME_MS = 10_000L

class SyncThread(private val syncRequest: SyncRequest,
                 private val networkConnectivityChecker: NetworkConnectivityChecker,
                 private val syncTokenStore: SyncTokenStore
) : Thread(), NetworkConnectivityChecker.Listener {

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
    private var cancelableRequest: Cancelable? = null

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
            cancelableRequest?.cancel()
            lock.notify()
        }
    }


    override fun run() {
        Timber.v("Start syncing...")
        state = State.RUNNING
        networkConnectivityChecker.register(this)
        while (state != State.KILLING) {
            if (!networkConnectivityChecker.isConnected() || state == State.PAUSED) {
                Timber.v("Waiting...")
                synchronized(lock) {
                    lock.wait()
                }
            } else {
                Timber.v("Execute sync request...")
                val latch = CountDownLatch(1)
                cancelableRequest = syncRequest.execute(nextBatch, object : MatrixCallback<SyncResponse> {
                    override fun onSuccess(data: SyncResponse) {
                        nextBatch = data.nextBatch
                        syncTokenStore.saveToken(nextBatch)
                        latch.countDown()
                    }

                    override fun onFailure(failure: Failure) {
                        if (failure !is Failure.NetworkConnection) {
                            // Wait 10s before retrying
                            sleep(RETRY_WAIT_TIME_MS)
                        }
                        latch.countDown()
                    }
                })
                latch.await()
            }
        }
        Timber.v("Sync killed")
        state = State.KILLED
        networkConnectivityChecker.unregister(this)
    }

    override fun onConnect() {
        synchronized(lock) {
            lock.notify()
        }
    }

}


