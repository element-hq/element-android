/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.room.uploads

import com.zhuinden.monarchy.Monarchy
import io.realm.Sort
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.model.message.MessageWithAttachmentContent
import org.matrix.android.sdk.api.session.room.sender.SenderInfo
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
        private val globalErrorReceiver: GlobalErrorReceiver
) : GetUploadsTask {

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
                        .sort(EventEntityFields.ORIGIN_SERVER_TS, Sort.DESCENDING)
                        .findAll()
                        .map { it.asDomain() }
                        // Exclude stickers
                        .filter { it.getClearType() != EventType.STICKER }
            }
            events = eventsFromRealm
        } else {
            val since = params.since ?: tokenStore.getLastToken() ?: throw IllegalStateException("No token available")

            val filter = FilterFactory.createUploadsFilter(params.numberOfEvents).toJSONString()
            val chunk = executeRequest(globalErrorReceiver) {
                roomAPI.getRoomMessagesFrom(params.roomId, since, PaginationDirection.BACKWARDS.value, params.numberOfEvents, filter)
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
