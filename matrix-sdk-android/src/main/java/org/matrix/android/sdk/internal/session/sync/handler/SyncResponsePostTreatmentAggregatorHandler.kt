/*
 * Copyright (c) 2021 The Matrix.org Foundation C.I.C.
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
import org.matrix.android.sdk.api.MatrixPatterns
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.internal.crypto.crosssigning.UpdateTrustWorker
import org.matrix.android.sdk.internal.crypto.crosssigning.UpdateTrustWorkerDataRepository
import org.matrix.android.sdk.internal.di.SessionId
import org.matrix.android.sdk.internal.di.WorkManagerProvider
import org.matrix.android.sdk.internal.session.sync.RoomSyncEphemeralTemporaryStore
import org.matrix.android.sdk.internal.session.sync.SyncResponsePostTreatmentAggregator
import org.matrix.android.sdk.internal.session.sync.model.accountdata.toMutable
import org.matrix.android.sdk.internal.session.user.accountdata.DirectChatsHelper
import org.matrix.android.sdk.internal.session.user.accountdata.UpdateUserAccountDataTask
import org.matrix.android.sdk.internal.util.logLimit
import org.matrix.android.sdk.internal.worker.WorkerParamsFactory
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

internal class SyncResponsePostTreatmentAggregatorHandler @Inject constructor(
        private val directChatsHelper: DirectChatsHelper,
        private val ephemeralTemporaryStore: RoomSyncEphemeralTemporaryStore,
        private val updateUserAccountDataTask: UpdateUserAccountDataTask,
        private val updateTrustWorkerDataRepository: UpdateTrustWorkerDataRepository,
        private val workManagerProvider: WorkManagerProvider,
        private val roomShieldSummaryUpdater: ShieldSummaryUpdater,
        @SessionId private val sessionId: String,
) {
    suspend fun handle(aggregator: SyncResponsePostTreatmentAggregator) {
        cleanupEphemeralFiles(aggregator.ephemeralFilesToDelete)
        updateDirectUserIds(aggregator.directChatsToCheck)
        fetchAndUpdateUsers(aggregator.userIdsToFetch)
        handleRefreshRoomShieldsForRooms(aggregator.roomsWithMembershipChangesForShieldUpdate)
    }

    private fun cleanupEphemeralFiles(ephemeralFilesToDelete: List<String>) {
        ephemeralFilesToDelete.forEach {
            ephemeralTemporaryStore.delete(it)
        }
    }

    private suspend fun updateDirectUserIds(directUserIdsToUpdate: Map<String, String>) {
        val directChats = directChatsHelper.getLocalDirectMessages().toMutable()
        var hasUpdate = false
        directUserIdsToUpdate.forEach { (roomId, candidateUserId) ->
            // consider room is a DM if referenced in the DM dictionary
            val currentDirectUserId = directChats.firstNotNullOfOrNull { (userId, roomIds) -> userId.takeIf { roomId in roomIds } }
            // update directUserId with the given candidateUserId if it mismatches the current one
            if (currentDirectUserId != null && !MatrixPatterns.isUserId(currentDirectUserId)) {
                // link roomId with the matrix id
                directChats
                        .getOrPut(candidateUserId) { arrayListOf() }
                        .apply {
                            if (!contains(roomId)) {
                                hasUpdate = true
                                add(roomId)
                            }
                        }

                // remove roomId from currentDirectUserId entry
                hasUpdate = hasUpdate or (directChats[currentDirectUserId]?.remove(roomId) == true)
                // remove currentDirectUserId entry if there is no attached room anymore
                hasUpdate = hasUpdate or (directChats.takeIf { it[currentDirectUserId].isNullOrEmpty() }?.remove(currentDirectUserId) != null)
            }
        }
        if (hasUpdate) {
            tryOrNull("Unable to update user account data") {
                updateUserAccountDataTask.execute(UpdateUserAccountDataTask.DirectChatParams(directMessages = directChats))
            }
        }
    }

    private fun fetchAndUpdateUsers(userIdsToFetch: Collection<String>) {
        if (userIdsToFetch.isEmpty()) return
        Timber.d("## Configure Worker to update users: ${userIdsToFetch.logLimit()}")
        val workerParams = UpdateTrustWorker.Params(
                sessionId = sessionId,
                filename = updateTrustWorkerDataRepository.createParam(userIdsToFetch.toList())
        )
        val workerData = WorkerParamsFactory.toData(workerParams)

        val workRequest = workManagerProvider.matrixOneTimeWorkRequestBuilder<UpdateUserWorker>()
                .setInputData(workerData)
                .setBackoffCriteria(BackoffPolicy.LINEAR, WorkManagerProvider.BACKOFF_DELAY_MILLIS, TimeUnit.MILLISECONDS)
                .build()

        workManagerProvider.workManager
                .beginUniqueWork("USER_UPDATE_QUEUE", ExistingWorkPolicy.APPEND_OR_REPLACE, workRequest)
                .enqueue()
    }

    private fun handleRefreshRoomShieldsForRooms(roomIds: Set<String>) {
        if (roomIds.isEmpty()) return
        roomShieldSummaryUpdater.refreshShieldsForRoomIds(roomIds)
    }
}
