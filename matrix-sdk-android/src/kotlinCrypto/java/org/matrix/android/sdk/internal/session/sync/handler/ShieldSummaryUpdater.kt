/*
 * Copyright 2023 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.sync.handler

import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import org.matrix.android.sdk.internal.crypto.crosssigning.UpdateTrustWorker
import org.matrix.android.sdk.internal.crypto.crosssigning.UpdateTrustWorkerDataRepository
import org.matrix.android.sdk.internal.di.SessionId
import org.matrix.android.sdk.internal.di.WorkManagerProvider
import org.matrix.android.sdk.internal.session.SessionScope
import org.matrix.android.sdk.internal.util.logLimit
import org.matrix.android.sdk.internal.worker.WorkerParamsFactory
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@SessionScope
internal class ShieldSummaryUpdater @Inject constructor(
        @SessionId private val sessionId: String,
        private val workManagerProvider: WorkManagerProvider,
        private val updateTrustWorkerDataRepository: UpdateTrustWorkerDataRepository,
) {

    fun refreshShieldsForRoomIds(roomIds: Set<String>) {
        Timber.d("## CrossSigning - checkAffectedRoomShields for roomIds: ${roomIds.logLimit()}")
        val workerParams = UpdateTrustWorker.Params(
                sessionId = sessionId,
                filename = updateTrustWorkerDataRepository.createParam(emptyList(), roomIds = roomIds.toList())
        )
        val workerData = WorkerParamsFactory.toData(workerParams)

        val workRequest = workManagerProvider.matrixOneTimeWorkRequestBuilder<UpdateTrustWorker>()
                .setInputData(workerData)
                .setBackoffCriteria(BackoffPolicy.LINEAR, WorkManagerProvider.BACKOFF_DELAY_MILLIS, TimeUnit.MILLISECONDS)
                .build()

        workManagerProvider.workManager
                .beginUniqueWork("TRUST_UPDATE_QUEUE", ExistingWorkPolicy.APPEND_OR_REPLACE, workRequest)
                .enqueue()
    }
}
