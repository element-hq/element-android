/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room.threads

import com.zhuinden.monarchy.Monarchy
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.model.message.MessageWithAttachmentContent
import org.matrix.android.sdk.api.session.room.sender.SenderInfo
import org.matrix.android.sdk.api.session.room.threads.FetchRootThreadsResult
import org.matrix.android.sdk.api.session.room.threads.RootThreadEvent
import org.matrix.android.sdk.api.session.room.uploads.GetUploadsResult
import org.matrix.android.sdk.api.session.room.uploads.UploadEvent
import org.matrix.android.sdk.internal.database.mapper.asDomain
import org.matrix.android.sdk.internal.database.model.EventEntity
import org.matrix.android.sdk.internal.database.model.EventEntityFields
import org.matrix.android.sdk.internal.database.query.TimelineEventFilter
import org.matrix.android.sdk.internal.database.query.whereType
import org.matrix.android.sdk.internal.di.SessionDatabase
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.filter.FilterFactory
import org.matrix.android.sdk.internal.session.room.RoomAPI
import org.matrix.android.sdk.internal.session.room.membership.RoomMemberHelper
import org.matrix.android.sdk.internal.session.room.timeline.PaginationDirection
import org.matrix.android.sdk.internal.session.sync.SyncTokenStore
import org.matrix.android.sdk.internal.task.Task
import javax.inject.Inject

internal interface FetchRootThreadsTask : Task<FetchRootThreadsTask.Params, FetchRootThreadsResult> {

    data class Params(
            val roomId: String,
            val numberOfEvents: Int,
            val since: String?
    )
}

internal class DefaultFetchRootThreadsTask @Inject constructor(
        private val roomAPI: RoomAPI,
        private val tokenStore: SyncTokenStore,
        @SessionDatabase private val monarchy: Monarchy,
        private val globalErrorReceiver: GlobalErrorReceiver
) : FetchRootThreadsTask {

    override suspend fun execute(params: FetchRootThreadsTask.Params): FetchRootThreadsResult {
        val result: FetchRootThreadsResult

        val filter = FilterFactory.createThreadsFilter(params.numberOfEvents).toJSONString()
        val since = params.since ?: tokenStore.getLastToken() ?: throw IllegalStateException("No token available")

        val chunk = executeRequest(globalErrorReceiver) {
            roomAPI.getRoomMessagesFrom(params.roomId, since, PaginationDirection.BACKWARDS.value, params.numberOfEvents, filter)
        }
        result = FetchRootThreadsResult(
                events = chunk.events.map {
                    RootThreadEvent(it, it.eventId!!, null)
                },
                nextToken = chunk.end ?: "",
                hasMore = chunk.hasMore()
        )
        return result
    }
}
