/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.sync.InitialSyncStep
import org.matrix.android.sdk.api.session.sync.SyncRequestState
import org.matrix.android.sdk.internal.session.SessionScope
import javax.inject.Inject

@SessionScope
internal class SyncRequestStateTracker @Inject constructor(
        private val coroutineScope: CoroutineScope
) : ProgressReporter {

    val syncRequestState = MutableSharedFlow<SyncRequestState>()

    private var rootTask: TaskInfo? = null

    // Only to be used for incremental sync
    fun setSyncRequestState(newSyncRequestState: SyncRequestState.IncrementalSyncRequestState) {
        emitSyncState(newSyncRequestState)
    }

    /**
     * Create a rootTask.
     */
    fun startRoot(
            initialSyncStep: InitialSyncStep,
            totalProgress: Int
    ) {
        if (rootTask != null) {
            endAll()
        }
        rootTask = TaskInfo(initialSyncStep, totalProgress, null, 1F)
        reportProgress(0F)
    }

    /**
     * Add a child to the leaf.
     */
    override fun startTask(
            initialSyncStep: InitialSyncStep,
            totalProgress: Int,
            parentWeight: Float
    ) {
        val currentLeaf = rootTask?.leaf() ?: return
        currentLeaf.child = TaskInfo(
                initialSyncStep = initialSyncStep,
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
                emitSyncState(SyncRequestState.InitialSyncProgressing(leaf.initialSyncStep, root.currentProgress.toInt()))
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
                emitSyncState(SyncRequestState.Idle)
            }
        }
    }

    fun endAll() {
        rootTask = null
        emitSyncState(SyncRequestState.Idle)
    }

    private fun emitSyncState(state: SyncRequestState) {
        coroutineScope.launch {
            syncRequestState.emit(state)
        }
    }
}
