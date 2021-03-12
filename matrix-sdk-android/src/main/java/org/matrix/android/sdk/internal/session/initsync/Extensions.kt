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

internal inline fun <T> reportSubtask(reporter: ProgressReporter?,
                                      initSyncStep: InitSyncStep,
                                      totalProgress: Int,
                                      parentWeight: Float,
                                      block: () -> T): T {
    reporter?.startTask(initSyncStep, totalProgress, parentWeight)
    return block().also {
        reporter?.endTask()
    }
}

internal inline fun <K, V, R> Map<out K, V>.mapWithProgress(reporter: ProgressReporter?,
                                                            initSyncStep: InitSyncStep,
                                                            parentWeight: Float,
                                                            transform: (Map.Entry<K, V>) -> R): List<R> {
    var current = 0F
    reporter?.startTask(initSyncStep, count() + 1, parentWeight)
    return map {
        reporter?.reportProgress(current)
        current++
        transform.invoke(it)
    }.also {
        reporter?.endTask()
    }
}
