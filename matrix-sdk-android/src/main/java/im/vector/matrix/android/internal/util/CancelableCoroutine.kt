package im.vector.matrix.android.internal.util

import im.vector.matrix.android.api.util.Cancelable
import kotlinx.coroutines.Job

internal class CancelableCoroutine(private val job: Job) : Cancelable {

    override fun cancel() {
        job.cancel()
    }

}