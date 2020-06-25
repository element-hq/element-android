/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.matrix.android.internal.session.room.uploads

import com.zhuinden.monarchy.Monarchy
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.message.MessageContent
import im.vector.matrix.android.api.session.room.model.message.MessageWithAttachmentContent
import im.vector.matrix.android.api.session.room.sender.SenderInfo
import im.vector.matrix.android.api.session.room.uploads.GetUploadsResult
import im.vector.matrix.android.api.session.room.uploads.UploadEvent
import im.vector.matrix.android.internal.database.mapper.asDomain
import im.vector.matrix.android.internal.database.model.EventEntity
import im.vector.matrix.android.internal.database.model.EventEntityFields
import im.vector.matrix.android.internal.database.query.TimelineEventFilter
import im.vector.matrix.android.internal.database.query.whereType
import im.vector.matrix.android.internal.di.SessionDatabase
import im.vector.matrix.android.internal.network.executeRequest
import im.vector.matrix.android.internal.session.filter.FilterFactory
import im.vector.matrix.android.internal.session.room.RoomAPI
import im.vector.matrix.android.internal.session.room.membership.RoomMemberHelper
import im.vector.matrix.android.internal.session.room.timeline.PaginationDirection
import im.vector.matrix.android.internal.session.room.timeline.PaginationResponse
import im.vector.matrix.android.internal.session.sync.SyncTokenStore
import im.vector.matrix.android.internal.task.Task
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject

internal interface GetUploadsTask : Task<GetUploadsTask.Params, GetUploadsResult> {

    data class Params(
            val roomId: String,
            val isRoomEncrypted: Boolean,
            val numberOfEvents: Int,
            val since: String?
    )
}

internal class DefaultGetUploadsTask @Inject constructor(
        private val roomAPI: RoomAPI,
        private val tokenStore: SyncTokenStore,
        @SessionDatabase private val monarchy: Monarchy,
        private val eventBus: EventBus)
    : GetUploadsTask {

    override suspend fun execute(params: GetUploadsTask.Params): GetUploadsResult {
        val result: GetUploadsResult
        val events: List<Event>

        if (params.isRoomEncrypted) {
            // Get a chunk of events from cache for e2e rooms

            result = GetUploadsResult(
                    uploadEvents = emptyList(),
                    nextToken = "",
                    hasMore = false
            )

            var eventsFromRealm = emptyList<Event>()
            monarchy.doWithRealm { realm ->
                eventsFromRealm = EventEntity.whereType(realm, EventType.ENCRYPTED, params.roomId)
                        .like(EventEntityFields.DECRYPTION_RESULT_JSON, TimelineEventFilter.DecryptedContent.URL)
                        .findAll()
                        .map { it.asDomain() }
                        // Exclude stickers
                        .filter { it.getClearType() != EventType.STICKER }
            }
            events = eventsFromRealm
        } else {
            val since = params.since ?: tokenStore.getLastToken() ?: throw IllegalStateException("No token available")

            val filter = FilterFactory.createUploadsFilter(params.numberOfEvents).toJSONString()
            val chunk = executeRequest<PaginationResponse>(eventBus) {
                apiCall = roomAPI.getRoomMessagesFrom(params.roomId, since, PaginationDirection.BACKWARDS.value, params.numberOfEvents, filter)
            }

            result = GetUploadsResult(
                    uploadEvents = emptyList(),
                    nextToken = chunk.end ?: "",
                    hasMore = chunk.hasMore()
            )
            events = chunk.events
        }

        var uploadEvents = listOf<UploadEvent>()

        val cacheOfSenderInfos = mutableMapOf<String, SenderInfo>()

        // Get a snapshot of all room members
        monarchy.doWithRealm { realm ->
            val roomMemberHelper = RoomMemberHelper(realm, params.roomId)

            uploadEvents = events.mapNotNull { event ->
                val eventId = event.eventId ?: return@mapNotNull null
                val messageContent = event.getClearContent()?.toModel<MessageContent>() ?: return@mapNotNull null
                val messageWithAttachmentContent = (messageContent as? MessageWithAttachmentContent) ?: return@mapNotNull null
                val senderId = event.senderId ?: return@mapNotNull null

                val senderInfo = cacheOfSenderInfos.getOrPut(senderId) {
                    val roomMemberSummaryEntity = roomMemberHelper.getLastRoomMember(senderId)
                    SenderInfo(
                            userId = senderId,
                            displayName = roomMemberSummaryEntity?.displayName,
                            isUniqueDisplayName = roomMemberHelper.isUniqueDisplayName(roomMemberSummaryEntity?.displayName),
                            avatarUrl = roomMemberSummaryEntity?.avatarUrl
                    )
                }

                UploadEvent(
                        root = event,
                        eventId = eventId,
                        contentWithAttachmentContent = messageWithAttachmentContent,
                        senderInfo = senderInfo
                )
            }
        }

        return result.copy(uploadEvents = uploadEvents)
    }
}
