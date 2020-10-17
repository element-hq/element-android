/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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
package org.matrix.android.sdk.internal.session

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.matrix.android.sdk.api.session.InitialSyncProgressService
import timber.log.Timber
import javax.inject.Inject

@SessionScope
class DefaultInitialSyncProgressService @Inject constructor() : InitialSyncProgressService {

    private val status = MutableLiveData<InitialSyncProgressService.Status>()

    private var rootTask: TaskInfo? = null

    override fun getInitialSyncProgressStatus(): LiveData<InitialSyncProgressService.Status> {
        return status
    }

    fun startTask(@StringRes nameRes: Int, totalProgress: Int, parentWeight: Float = 1f) {
        // Create a rootTask, or add a child to the leaf
        if (rootTask == null) {
            rootTask = TaskInfo(nameRes, totalProgress)
        } else {
            val currentLeaf = rootTask!!.leaf()

            val newTask = TaskInfo(nameRes,
                    totalProgress,
                    currentLeaf,
                    parentWeight)

            currentLeaf.child = newTask
        }
        reportProgress(0)
    }

    fun reportProgress(progress: Int) {
        rootTask?.leaf()?.setProgress(progress)
    }

    fun endTask(nameRes: Int) {
        val endedTask = rootTask?.leaf()
        if (endedTask?.nameRes == nameRes) {
            // close it
            val parent = endedTask.parent
            parent?.child = null
            parent?.setProgress(endedTask.offset + (endedTask.totalProgress * endedTask.parentWeight).toInt())
        }
        if (endedTask?.parent == null) {
            status.postValue(InitialSyncProgressService.Status.Idle)
        }
    }

    fun endAll() {
        rootTask = null
        status.postValue(InitialSyncProgressService.Status.Idle)
    }

    private inner class TaskInfo(@StringRes var nameRes: Int,
                                 var totalProgress: Int,
                                 var parent: TaskInfo? = null,
                                 var parentWeight: Float = 1f,
                                 var offset: Int = parent?.currentProgress ?: 0) {
        var child: TaskInfo? = null
        var currentProgress: Int = 0

        /**
         * Get the further child
         */
        fun leaf(): TaskInfo {
            var last = this
            while (last.child != null) {
                last = last.child!!
            }
            return last
        }

        /**
         * Set progress of the parent if any (which will post value), or post the value
         */
        fun setProgress(progress: Int) {
            currentProgress = progress
//            val newProgress = Math.min(currentProgress + progress, totalProgress)
            parent?.let {
                val parentProgress = (currentProgress * parentWeight).toInt()
                it.setProgress(offset + parentProgress)
            } ?: run {
                Timber.v("--- ${leaf().nameRes}: $currentProgress")
                status.postValue(InitialSyncProgressService.Status.Progressing(leaf().nameRes, currentProgress))
            }
        }
    }
}

inline fun <T> reportSubtask(reporter: DefaultInitialSyncProgressService?,
                             @StringRes nameRes: Int,
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
    val total = count().toFloat()
    var current = 0
    reporter?.startTask(taskId, 100, weight)
    return map {
        reporter?.reportProgress((current / total * 100).toInt())
        current++
        transform.invoke(it)
    }.also {
        reporter?.endTask(taskId)
    }
}
