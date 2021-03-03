/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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

import org.matrix.android.sdk.api.session.initsync.InitSyncStep
import timber.log.Timber

internal class TaskInfo(val initSyncStep: InitSyncStep,
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
