package im.vector.matrix.android.internal.util

import androidx.work.WorkManager
import im.vector.matrix.android.api.util.Cancelable
import java.util.*

internal class CancelableWork(private val workId: UUID) : Cancelable {

    override fun cancel() {
        WorkManager.getInstance().cancelWorkById(workId)
    }

}