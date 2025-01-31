/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package org.matrix.android.sdk.internal.session.room.timeline

import androidx.work.BackoffPolicy
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import org.matrix.android.sdk.api.util.Cancelable
import org.matrix.android.sdk.internal.di.WorkManagerProvider
import org.matrix.android.sdk.internal.util.CancelableWork
import org.matrix.android.sdk.internal.worker.startChain
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Helper class for sending event related works.
 * All send event from a room are using the same workchain, in order to ensure order.
 * WorkRequest must always return success (even if server error, in this case marking the event as failed to send),
 * if not the chain will be doomed in failed state.
 */
internal class TimelineSendEventWorkCommon @Inject constructor(
        private val workManagerProvider: WorkManagerProvider
) {

    fun postWork(roomId: String, workRequest: OneTimeWorkRequest, policy: ExistingWorkPolicy = ExistingWorkPolicy.APPEND_OR_REPLACE): Cancelable {
        workManagerProvider.workManager
                .beginUniqueWork(buildWorkName(roomId), policy, workRequest)
                .enqueue()

        return CancelableWork(workManagerProvider.workManager, workRequest.id)
    }

    inline fun <reified W : ListenableWorker> createWork(data: Data, startChain: Boolean): OneTimeWorkRequest {
        return workManagerProvider.matrixOneTimeWorkRequestBuilder<W>()
                .setConstraints(WorkManagerProvider.workConstraints)
                .startChain(startChain)
                .setInputData(data)
                .setBackoffCriteria(BackoffPolicy.LINEAR, WorkManagerProvider.BACKOFF_DELAY_MILLIS, TimeUnit.MILLISECONDS)
                .build()
    }

    private fun buildWorkName(roomId: String): String {
        return "${roomId}_$SEND_WORK"
    }

    companion object {
        private const val SEND_WORK = "SEND_WORK"
    }
}
