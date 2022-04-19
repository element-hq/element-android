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

import com.zhuinden.monarchy.Monarchy
import org.matrix.android.sdk.api.MatrixPatterns
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.user.model.User
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.session.profile.GetProfileInfoTask
import org.matrix.android.sdk.internal.session.sync.RoomSyncEphemeralTemporaryStore
import org.matrix.android.sdk.internal.session.sync.SyncResponsePostTreatmentAggregator
import org.matrix.android.sdk.internal.session.sync.model.accountdata.toMutable
import org.matrix.android.sdk.internal.session.user.UserEntityFactory
import org.matrix.android.sdk.internal.session.user.accountdata.DirectChatsHelper
import org.matrix.android.sdk.internal.session.user.accountdata.UpdateUserAccountDataTask
import javax.inject.Inject

internal class SyncResponsePostTreatmentAggregatorHandler @Inject constructor(
        private val directChatsHelper: DirectChatsHelper,
        private val ephemeralTemporaryStore: RoomSyncEphemeralTemporaryStore,
        private val updateUserAccountDataTask: UpdateUserAccountDataTask,
        private val getProfileInfoTask: GetProfileInfoTask,
        @SessionDatabase private val monarchy: Monarchy,
) {
    suspend fun handle(aggregator: SyncResponsePostTreatmentAggregator) {
        cleanupEphemeralFiles(aggregator.ephemeralFilesToDelete)
        updateDirectUserIds(aggregator.directChatsToCheck)
        fetchAndUpdateUsers(aggregator.userIdsToFetch)
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
            updateUserAccountDataTask.execute(UpdateUserAccountDataTask.DirectChatParams(directMessages = directChats))
        }
    }

    private suspend fun fetchAndUpdateUsers(userIdsToFetch: List<String>) {
        fetchUsers(userIdsToFetch)
                .takeIf { it.isNotEmpty() }
                ?.saveLocally()
    }

    private suspend fun fetchUsers(userIdsToFetch: List<String>) = userIdsToFetch.mapNotNull {
        tryOrNull {
            val profileJson = getProfileInfoTask.execute(GetProfileInfoTask.Params(it))
            User.fromJson(it, profileJson)
        }
    }

    private fun List<User>.saveLocally() {
        val userEntities = map { user -> UserEntityFactory.create(user) }
        monarchy.doWithRealm {
            it.insertOrUpdate(userEntities)
        }
    }
}
