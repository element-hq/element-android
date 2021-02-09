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

internal interface ProgressReporter {
    fun startTask(@StringRes nameRes: Int,
                  totalProgress: Int,
                  parentWeight: Float)

    fun reportProgress(progress: Float)

    fun endTask()
}

@SessionScope
internal class DefaultInitialSyncProgressService @Inject constructor()
    : InitialSyncProgressService,
        ProgressReporter {

    private val status = MutableLiveData<InitialSyncProgressService.Status>()

    private var rootTask: TaskInfo? = null

    override fun getInitialSyncProgressStatus(): LiveData<InitialSyncProgressService.Status> {
        return status
    }

    override fun startTask(@StringRes nameRes: Int,
                           totalProgress: Int,
                           parentWeight: Float) {
        // Create a rootTask, or add a child to the leaf
        if (rootTask == null) {
            rootTask = TaskInfo(nameRes, totalProgress, null, 1F)
        } else {
            val currentLeaf = rootTask!!.leaf()
            currentLeaf.child = TaskInfo(
                    nameRes = nameRes,
                    totalProgress = totalProgress,
                    parent = currentLeaf,
                    parentWeight = parentWeight
            )
        }
        reportProgress(0F)
    }

    override fun reportProgress(progress: Float) {
        rootTask?.let { root ->
            root.leaf().let { leaf ->
                // Update the progress of the leaf and all its parents
                leaf.setProgress(progress)
                // Then update the live data using leaf wording and root progress
                status.postValue(InitialSyncProgressService.Status.Progressing(leaf.nameRes, root.currentProgress.toInt()))
            }
        }
    }

    override fun endTask() {
        rootTask?.leaf()?.let { endedTask ->
            // Ensure the task progress is complete
            reportProgress(endedTask.totalProgress.toFloat())
            endedTask.parent?.child = null

            if (endedTask.parent != null) {
                // And close it
                endedTask.parent.child = null
            } else {
                status.postValue(InitialSyncProgressService.Status.Idle)
            }
        }
    }

    fun endAll() {
        rootTask = null
        status.postValue(InitialSyncProgressService.Status.Idle)
    }
}

private class TaskInfo(@StringRes val nameRes: Int,
                       val totalProgress: Int,
                       val parent: TaskInfo?,
                       val parentWeight: Float) {
    var child: TaskInfo? = null
    var currentProgress = 0F
        private set
    private val offset = parent?.currentProgress ?: 0F

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
     * Set progress of this task and update the parent progress iteratively
     */
    fun setProgress(progress: Float) {
        Timber.v("setProgress: $progress / $totalProgress")
        currentProgress = progress

        parent?.let {
            val parentProgress = (currentProgress / totalProgress) * (parentWeight * it.totalProgress)
            it.setProgress(offset + parentProgress)
        }
    }
}

internal inline fun <T> reportSubtask(reporter: ProgressReporter?,
                                      @StringRes nameRes: Int,
                                      totalProgress: Int,
                                      parentWeight: Float,
                                      block: () -> T): T {
    reporter?.startTask(nameRes, totalProgress, parentWeight)
    return block().also {
        reporter?.endTask()
    }
}

internal inline fun <K, V, R> Map<out K, V>.mapWithProgress(reporter: ProgressReporter?,
                                                            @StringRes nameRes: Int,
                                                            parentWeight: Float,
                                                            transform: (Map.Entry<K, V>) -> R): List<R> {
    var current = 0F
    reporter?.startTask(nameRes, count() + 1, parentWeight)
    return map {
        reporter?.reportProgress(current)
        current++
        transform.invoke(it)
    }.also {
        reporter?.endTask()
    }
}
