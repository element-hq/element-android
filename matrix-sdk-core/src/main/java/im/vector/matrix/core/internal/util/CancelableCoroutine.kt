package im.vector.matrix.core.internal.util

import im.vector.matrix.core.api.util.Cancelable
import kotlinx.coroutines.Job

class CancelableCoroutine(private val job: Job) : Cancelable {

    override fun cancel() {
        job.cancel()
    }

}