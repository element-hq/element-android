/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.sync

import org.matrix.android.sdk.api.session.sync.InitialSyncStep

internal inline fun <T> reportSubtask(
        reporter: ProgressReporter?,
        initialSyncStep: InitialSyncStep,
        totalProgress: Int,
        parentWeight: Float,
        block: () -> T
): T {
    reporter?.startTask(initialSyncStep, totalProgress, parentWeight)
    return block().also {
        reporter?.endTask()
    }
}

internal inline fun <K, V, R> Map<out K, V>.mapWithProgress(
        reporter: ProgressReporter?,
        initialSyncStep: InitialSyncStep,
        parentWeight: Float,
        transform: (Map.Entry<K, V>) -> R
): List<R> {
    var current = 0F
    reporter?.startTask(initialSyncStep, count() + 1, parentWeight)
    return map {
        reporter?.reportProgress(current)
        current++
        transform.invoke(it)
    }.also {
        reporter?.endTask()
    }
}
