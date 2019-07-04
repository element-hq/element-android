package im.vector.matrix.android.internal.session

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import im.vector.matrix.android.api.session.InitialSyncProgressService
import timber.log.Timber
import javax.inject.Inject


@SessionScope
class DefaultInitialSyncProgressService @Inject constructor() : InitialSyncProgressService {

    var status = MutableLiveData<InitialSyncProgressService.Status>()

    var rootTask: TaskInfo? = null

    override fun getLiveStatus(): LiveData<InitialSyncProgressService.Status?> {
        return status
    }


    fun startTask(nameRes: Int, totalProgress: Int, parentWeight: Float = 1f) {
        if (rootTask == null) {
            rootTask = TaskInfo(nameRes, totalProgress)
        } else {
            val currentLeaf = rootTask!!.leaf()
            val newTask = TaskInfo(nameRes, totalProgress)
            newTask.parent = currentLeaf
            newTask.offset = currentLeaf.currentProgress
            currentLeaf.child = newTask
            newTask.parentWeight = parentWeight
        }
        reportProgress(0)
    }

    fun reportProgress(progress: Int) {
        rootTask?.leaf()?.incrementProgress(progress)
    }

    fun endTask(nameRes: Int) {
        val endedTask = rootTask?.leaf()
        if (endedTask?.nameRes == nameRes) {
            //close it
            val parent = endedTask.parent
            parent?.child = null
            parent?.incrementProgress(endedTask.offset + (endedTask.totalProgress * endedTask.parentWeight).toInt())
        }
        if (endedTask?.parent == null) {
            this@DefaultInitialSyncProgressService.status.postValue(null)
        }
    }

    fun endAll() {
        this@DefaultInitialSyncProgressService.status.postValue(null)
    }


    inner class TaskInfo(var nameRes: Int,
                         var totalProgress: Int) {
        var parent: TaskInfo? = null
        var child: TaskInfo? = null
        var parentWeight: Float = 1f
        var currentProgress: Int = 0
        var offset: Int = 0

        fun leaf(): TaskInfo {
            var last = this
            while (last.child != null) {
                last = last.child!!
            }
            return last
        }

        fun incrementProgress(progress: Int) {
            currentProgress = progress
//            val newProgress = Math.min(currentProgress + progress, totalProgress)
            parent?.let {
                val parentProgress = (currentProgress * parentWeight).toInt()
                it.incrementProgress(offset + parentProgress)
            }
            if (parent == null) {
                Timber.e("--- ${leaf().nameRes}: ${currentProgress}")
                this@DefaultInitialSyncProgressService.status.postValue(
                        InitialSyncProgressService.Status(leaf().nameRes, currentProgress)
                )
            }
        }
    }

}

public inline fun <T> reportSubtask(reporter: DefaultInitialSyncProgressService?, nameRes: Int, totalProgress: Int, parentWeight: Float = 1f, block: () -> T): T {
    reporter?.startTask(nameRes, totalProgress, parentWeight)
    return block().also {
        reporter?.endTask(nameRes)
    }
}