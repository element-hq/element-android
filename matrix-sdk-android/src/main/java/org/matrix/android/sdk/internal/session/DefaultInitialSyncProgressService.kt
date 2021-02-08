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
import org.matrix.android.sdk.internal.util.StringProvider
import timber.log.Timber
import javax.inject.Inject

@SessionScope
internal class DefaultInitialSyncProgressService @Inject constructor(
        private val stringProvider: StringProvider
) : InitialSyncProgressService {

    private val status = MutableLiveData<InitialSyncProgressService.Status>()

    private var rootTask: TaskInfo? = null

    override fun getInitialSyncProgressStatus(): LiveData<InitialSyncProgressService.Status> {
        return status
    }

    fun startTask(@StringRes nameRes: Int, totalProgress: Int, parentWeight: Float) {
        // Create a rootTask, or add a child to the leaf
        if (rootTask == null) {
            rootTask = TaskInfo(nameRes, totalProgress)
        } else {
            val currentLeaf = rootTask!!.leaf()

            val newTask = TaskInfo(
                    nameRes = nameRes,
                    totalProgress = totalProgress,
                    parent = currentLeaf,
                    parentWeight = parentWeight
            )

            currentLeaf.child = newTask
        }
        reportProgress(0F)
    }

    fun reportProgress(progress: Float) {
        rootTask?.leaf()?.setProgress(progress)
    }

    fun endTask(nameRes: Int) {
        val endedTask = rootTask?.leaf()
        if (endedTask?.nameRes == nameRes) {
            // Ensure the task progress is complete
            endedTask.setProgress(endedTask.totalProgress.toFloat())
            // And close it
            endedTask.parent?.child = null
        }
        if (endedTask?.parent == null) {
            status.postValue(InitialSyncProgressService.Status.Idle)
        }
    }

    fun endAll() {
        rootTask = null
        status.postValue(InitialSyncProgressService.Status.Idle)
    }

    private inner class TaskInfo(@StringRes val nameRes: Int,
                                 val totalProgress: Int,
                                 val parent: TaskInfo? = null,
                                 val parentWeight: Float = 1f,
                                 val offset: Float = parent?.currentProgress ?: 0F) {
        var child: TaskInfo? = null
        private var currentProgress: Float = 0F

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
         * Set progress of this task and update the parent progress. Last parent will post value.
         */
        fun setProgress(progress: Float) {
            Timber.v("setProgress: $progress / $totalProgress")
            currentProgress = progress
//            val newProgress = Math.min(currentProgress + progress, totalProgress)
            if (parent != null) {
                val parentProgress = (currentProgress / totalProgress) * (parentWeight * parent.totalProgress)
                parent.setProgress(offset + parentProgress)
            } else {
                Timber.v("--- ${stringProvider.getString(leaf().nameRes)}: ${currentProgress.toInt()}")
                status.postValue(InitialSyncProgressService.Status.Progressing(leaf().nameRes, currentProgress.toInt()))
            }
        }
    }
}

internal inline fun <T> reportSubtask(reporter: DefaultInitialSyncProgressService?,
                                      @StringRes nameRes: Int,
                                      totalProgress: Int,
                                      parentWeight: Float,
                                      block: () -> T): T {
    reporter?.startTask(nameRes, totalProgress, parentWeight)
    return block().also {
        reporter?.endTask(nameRes)
    }
}

internal inline fun <K, V, R> Map<out K, V>.mapWithProgress(reporter: DefaultInitialSyncProgressService?,
                                                            taskId: Int,
                                                            parentWeight: Float,
                                                            transform: (Map.Entry<K, V>) -> R): List<R> {
    var current = 0F
    reporter?.startTask(taskId, count() + 1, parentWeight)
    return map {
        reporter?.reportProgress(current)
        current++
        transform.invoke(it)
    }.also {
        reporter?.endTask(taskId)
    }
}
