/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.riotx.features.home.room.detail.timeline.factory

import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.riotx.core.epoxy.EmptyItem_
import im.vector.riotx.core.epoxy.VectorEpoxyModel
import im.vector.riotx.features.home.room.detail.timeline.TimelineEventController
import timber.log.Timber
import javax.inject.Inject

class TimelineItemFactory @Inject constructor(private val messageItemFactory: MessageItemFactory,
                                              private val encryptedItemFactory: EncryptedItemFactory,
                                              private val noticeItemFactory: NoticeItemFactory,
                                              private val defaultItemFactory: DefaultItemFactory,
                                              private val roomCreateItemFactory: RoomCreateItemFactory) {

    fun create(event: TimelineEvent,
               nextEvent: TimelineEvent?,
               eventIdToHighlight: String?,
               callback: TimelineEventController.Callback?): VectorEpoxyModel<*> {
        val highlight = event.root.eventId == eventIdToHighlight

        val computedModel = try {
            when (event.root.getClearType()) {
                EventType.STICKER,
                EventType.MESSAGE                       -> messageItemFactory.create(event, nextEvent, highlight, callback)
                // State and call
                EventType.STATE_ROOM_TOMBSTONE,
                EventType.STATE_ROOM_NAME,
                EventType.STATE_ROOM_TOPIC,
                EventType.STATE_ROOM_MEMBER,
                EventType.STATE_ROOM_JOIN_RULES,
                EventType.STATE_HISTORY_VISIBILITY,
                EventType.CALL_INVITE,
                EventType.CALL_HANGUP,
                EventType.CALL_ANSWER,
                EventType.REACTION,
                EventType.REDACTION,
                EventType.ENCRYPTION                    -> noticeItemFactory.create(event, highlight, callback)
                // State room create
                EventType.STATE_ROOM_CREATE             -> roomCreateItemFactory.create(event, callback)
                // Crypto
                EventType.ENCRYPTED                     -> {
                    if (event.root.isRedacted()) {
                        // Redacted event, let the MessageItemFactory handle it
                        messageItemFactory.create(event, nextEvent, highlight, callback)
                    } else {
                        encryptedItemFactory.create(event, nextEvent, highlight, callback)
                    }
                }

                // Unhandled event types (yet)
                EventType.STATE_ROOM_THIRD_PARTY_INVITE -> defaultItemFactory.create(event, highlight, callback)
                else                                    -> {
                    Timber.v("Type ${event.root.getClearType()} not handled")
                    null
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "failed to create message item")
            defaultItemFactory.create(event, highlight, callback, e)
        }
        return (computedModel ?: EmptyItem_())
    }
}
