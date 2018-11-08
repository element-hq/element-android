package im.vector.matrix.android.internal.util

import com.google.common.util.concurrent.ListenableFuture
import im.vector.matrix.android.api.util.Cancelable
import kotlinx.coroutines.Job

internal class CancelableWork(private val work: ListenableFuture<Void>) : Cancelable {

    override fun cancel() {
        work.cancel(true)
    }

}