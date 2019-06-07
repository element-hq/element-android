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

package im.vector.riotredesign.features.home.room.detail.timeline.factory

import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.riotredesign.core.epoxy.EmptyItem_
import im.vector.riotredesign.core.epoxy.VectorEpoxyModel
import im.vector.riotredesign.features.home.room.detail.timeline.TimelineEventController
import timber.log.Timber

class TimelineItemFactory(private val messageItemFactory: MessageItemFactory,
                          private val encryptionItemFactory: EncryptionItemFactory,
                          private val encryptedItemFactory: EncryptedItemFactory,
                          private val noticeItemFactory: NoticeItemFactory,
                          private val defaultItemFactory: DefaultItemFactory) {

    fun create(event: TimelineEvent,
               nextEvent: TimelineEvent?,
               callback: TimelineEventController.Callback?): VectorEpoxyModel<*> {

        val computedModel = try {
            when (event.root.getClearType()) {
                EventType.MESSAGE           -> messageItemFactory.create(event, nextEvent, callback)
                // State and call
                EventType.STATE_ROOM_NAME,
                EventType.STATE_ROOM_TOPIC,
                EventType.STATE_ROOM_MEMBER,
                EventType.STATE_HISTORY_VISIBILITY,
                EventType.CALL_INVITE,
                EventType.CALL_HANGUP,
                EventType.CALL_ANSWER       -> noticeItemFactory.create(event)

                // Crypto
                EventType.ENCRYPTION        -> encryptionItemFactory.create(event)
                EventType.ENCRYPTED         -> encryptedItemFactory.create(event)

                // Unhandled event types (yet)
                EventType.STATE_ROOM_THIRD_PARTY_INVITE,
                EventType.STICKER,
                EventType.STATE_ROOM_CREATE -> defaultItemFactory.create(event)
                else                        -> {
                    Timber.w("Ignored event (type: ${event.root.type}")
                    null
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "failed to create message item")
            defaultItemFactory.create(event, e)
        }
        return (computedModel ?: EmptyItem_())
    }

}