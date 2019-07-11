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
            status.postValue(null)
        }
    }

    fun endAll() {
        rootTask = null
        status.postValue(null)
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
                status.postValue(
                        InitialSyncProgressService.Status(leaf().nameRes, currentProgress)
                )
            }
        }
    }

}

inline fun <T> reportSubtask(reporter: DefaultInitialSyncProgressService?,
                             nameRes: Int,
                             totalProgress: Int,
                             parentWeight: Float = 1f,
                             block: () -> T): T {
    reporter?.startTask(nameRes, totalProgress, parentWeight)
    return block().also {
        reporter?.endTask(nameRes)
    }
}


inline fun <K, V, R> Map<out K, V>.mapWithProgress(reporter: DefaultInitialSyncProgressService?,
                                                   taskId: Int,
                                                   weight: Float,
                                                   transform: (Map.Entry<K, V>) -> R): List<R> {
    val total = count()
    var current = 0
    reporter?.startTask(taskId, 100, weight)
    return this.map {
        reporter?.reportProgress((current / total.toFloat() * 100).toInt())
        current++
        transform.invoke(it)
    }.also {
        reporter?.endTask(taskId)
    }
}

