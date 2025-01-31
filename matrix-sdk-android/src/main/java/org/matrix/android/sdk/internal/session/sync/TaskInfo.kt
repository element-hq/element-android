/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.sync

import org.matrix.android.sdk.api.session.sync.InitialSyncStep
import timber.log.Timber

internal class TaskInfo(
        val initialSyncStep: InitialSyncStep,
        val totalProgress: Int,
        val parent: TaskInfo?,
        val parentWeight: Float
) {
    var child: TaskInfo? = null
    var currentProgress = 0F
        private set
    private val offset = parent?.currentProgress ?: 0F

    /**
     * Get the further child.
     */
    fun leaf(): TaskInfo {
        var last = this
        while (last.child != null) {
            last = last.child!!
        }
        return last
    }

    /**
     * Set progress of this task and update the parent progress iteratively.
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
