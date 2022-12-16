/*
 * Copyright 2022 The Matrix.org Foundation C.I.C.
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
package org.matrix.android.sdk.internal.session.room.relation.threads

import com.zhuinden.monarchy.Monarchy
import io.realm.RealmList
import org.matrix.android.sdk.api.session.crypto.CryptoService
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.api.session.room.threads.FetchThreadsResult
import org.matrix.android.sdk.api.session.room.threads.ThreadFilter
import org.matrix.android.sdk.api.session.room.threads.model.ThreadSummaryUpdateType
import org.matrix.android.sdk.internal.database.helper.createOrUpdate
import org.matrix.android.sdk.internal.database.model.RoomEntity
import org.matrix.android.sdk.internal.database.model.threads.ThreadListPageEntity
import org.matrix.android.sdk.internal.database.model.threads.ThreadSummaryEntity
import org.matrix.android.sdk.internal.database.query.getOrCreate
import org.matrix.android.sdk.internal.database.query.where
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.room.RoomAPI
import org.matrix.android.sdk.internal.task.Task
import org.matrix.android.sdk.internal.util.awaitTransaction
import org.matrix.android.sdk.internal.util.time.Clock
import javax.inject.Inject

/***
 * This class is responsible to Fetch all the thread in the current room,
 * To fetch all threads in a room, the /messages API is used with newly added filtering options.
 */
internal interface FetchThreadSummariesTask : Task<FetchThreadSummariesTask.Params, FetchThreadsResult> {
    data class Params(
            val roomId: String,
            val from: String? = null,
            val limit: Int = 5,
            val filter: ThreadFilter? = null,
    )
}

internal class DefaultFetchThreadSummariesTask @Inject constructor(
        private val roomAPI: RoomAPI,
        private val globalErrorReceiver: GlobalErrorReceiver,
        @SessionDatabase private val monarchy: Monarchy,
        private val cryptoService: CryptoService,
        @UserId private val userId: String,
        private val clock: Clock,
) : FetchThreadSummariesTask {

    override suspend fun execute(params: FetchThreadSummariesTask.Params): FetchThreadsResult {
        val response = executeRequest(globalErrorReceiver) {
            roomAPI.getThreadsList(
                    roomId = params.roomId,
                    include = params.filter?.toString()?.lowercase(),
                    from = params.from,
                    limit = params.limit
            )
        }

        handleResponse(response, params)

        return when {
            response.nextBatch != null -> FetchThreadsResult.ShouldFetchMore(response.nextBatch)
            else -> FetchThreadsResult.ReachedEnd
        }
    }

    private suspend fun handleResponse(
            response: ThreadSummariesResponse,
            params: FetchThreadSummariesTask.Params
    ) {
        val rootThreadList = response.chunk

        val threadSummaries = RealmList<ThreadSummaryEntity>()

        monarchy.awaitTransaction { realm ->
            val roomEntity = RoomEntity.where(realm, roomId = params.roomId).findFirst() ?: return@awaitTransaction

            val roomMemberContentsByUser = HashMap<String, RoomMemberContent?>()

            for (rootThreadEvent in rootThreadList) {
                if (rootThreadEvent.eventId == null || rootThreadEvent.senderId == null || rootThreadEvent.type == null) {
                    continue
                }

                val threadSummary = ThreadSummaryEntity.createOrUpdate(
                        threadSummaryType = ThreadSummaryUpdateType.REPLACE,
                        realm = realm,
                        roomId = params.roomId,
                        rootThreadEvent = rootThreadEvent,
                        roomMemberContentsByUser = roomMemberContentsByUser,
                        roomEntity = roomEntity,
                        userId = userId,
                        cryptoService = cryptoService,
                        currentTimeMillis = clock.epochMillis(),
                )

                threadSummaries.add(threadSummary)
            }

            val page = ThreadListPageEntity.getOrCreate(realm, params.roomId)
            threadSummaries.forEach {
                if (!page.threadSummaries.contains(it)) {
                    page.threadSummaries.add(it)
                }
            }
        }
    }
}
