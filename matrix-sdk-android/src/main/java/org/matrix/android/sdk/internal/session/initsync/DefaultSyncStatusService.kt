/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.matrix.android.sdk.internal.session.initsync

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.matrix.android.sdk.api.session.initsync.InitSyncStep
import org.matrix.android.sdk.api.session.initsync.SyncStatusService
import org.matrix.android.sdk.internal.session.SessionScope
import javax.inject.Inject

@SessionScope
internal class DefaultSyncStatusService @Inject constructor() :
    SyncStatusService,
        ProgressReporter {

    private val status = MutableLiveData<SyncStatusService.Status>()

    private var rootTask: TaskInfo? = null

    override fun getSyncStatusLive(): LiveData<SyncStatusService.Status> {
        return status
    }

    // Only to be used for incremental sync
    fun setStatus(newStatus: SyncStatusService.Status.IncrementalSyncStatus) {
        status.postValue(newStatus)
    }

    /**
     * Create a rootTask
     */
    fun startRoot(initSyncStep: InitSyncStep,
                  totalProgress: Int) {
        endAll()
        rootTask = TaskInfo(initSyncStep, totalProgress, null, 1F)
        reportProgress(0F)
    }

    /**
     * Add a child to the leaf
     */
    override fun startTask(initSyncStep: InitSyncStep,
                           totalProgress: Int,
                           parentWeight: Float) {
        val currentLeaf = rootTask?.leaf() ?: return
        currentLeaf.child = TaskInfo(
                initSyncStep = initSyncStep,
                totalProgress = totalProgress,
                parent = currentLeaf,
                parentWeight = parentWeight
        )
        reportProgress(0F)
    }

    override fun reportProgress(progress: Float) {
        rootTask?.let { root ->
            root.leaf().let { leaf ->
                // Update the progress of the leaf and all its parents
                leaf.setProgress(progress)
                // Then update the live data using leaf wording and root progress
                status.postValue(SyncStatusService.Status.InitialSyncProgressing(leaf.initSyncStep, root.currentProgress.toInt()))
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
                status.postValue(SyncStatusService.Status.Idle)
            }
        }
    }

    fun endAll() {
        rootTask = null
        status.postValue(SyncStatusService.Status.Idle)
    }
}
