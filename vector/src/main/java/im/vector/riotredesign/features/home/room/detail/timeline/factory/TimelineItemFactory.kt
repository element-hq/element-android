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
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.message.MessageContent
import im.vector.matrix.android.api.session.room.model.message.MessageDefaultContent
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.riotredesign.core.epoxy.EmptyItem_
import im.vector.riotredesign.core.epoxy.VectorEpoxyModel
import im.vector.riotredesign.features.home.room.detail.timeline.TimelineEventController
import im.vector.riotredesign.features.home.room.detail.timeline.helper.TimelineDisplayableEvents
import im.vector.riotredesign.features.home.room.detail.timeline.item.MessageInformationData
import im.vector.riotredesign.features.home.room.detail.timeline.item.MessageTextItem_
import timber.log.Timber

class TimelineItemFactory(private val messageItemFactory: MessageItemFactory,
                          private val encryptionItemFactory: EncryptionItemFactory,
                          private val encryptedItemFactory: EncryptedItemFactory,
                          private val noticeItemFactory: NoticeItemFactory,
                          private val defaultItemFactory: DefaultItemFactory) {

    fun create(event: TimelineEvent,
               nextEvent: TimelineEvent?,
               eventIdToHighlight: String?,
               callback: TimelineEventController.Callback?): VectorEpoxyModel<*> {
        val highlight = event.root.eventId == eventIdToHighlight

        val computedModel = try {
            when (event.root.getClearType()) {
                EventType.MESSAGE           -> messageItemFactory.create(event, nextEvent, highlight, callback)
                // State and call
                EventType.STATE_ROOM_NAME,
                EventType.STATE_ROOM_TOPIC,
                EventType.STATE_ROOM_MEMBER,
                EventType.STATE_HISTORY_VISIBILITY,
                EventType.CALL_INVITE,
                EventType.CALL_HANGUP,
                EventType.CALL_ANSWER       -> noticeItemFactory.create(event, highlight, callback)

                // Crypto
                EventType.ENCRYPTION        -> encryptionItemFactory.create(event, highlight, callback)
                EventType.ENCRYPTED         -> encryptedItemFactory.create(event, nextEvent, highlight, callback)

                // Unhandled event types (yet)
                EventType.STATE_ROOM_THIRD_PARTY_INVITE,
                EventType.STICKER,
                EventType.STATE_ROOM_CREATE -> defaultItemFactory.create(event, highlight)

                else                        -> {
                    //These are just for debug to display hidden event, they should be filtered out in normal mode
                    if (TimelineDisplayableEvents.DEBUG_HIDDEN_EVENT) {
                        val informationData = MessageInformationData(eventId = event.root.eventId
                                ?: "?",
                                senderId = event.root.senderId ?: "",
                                sendState = event.sendState,
                                time = "",
                                avatarUrl = null,
                                memberName = "",
                                showInformation = false
                        )
                        val messageContent = event.root.content.toModel<MessageContent>()
                                ?: MessageDefaultContent("", "", null, null)
                        MessageTextItem_()
                                .informationData(informationData)
                                .message("{ \"type\": ${event.root.type} }")
                                .highlighted(highlight)
                                .longClickListener { view ->
                                    return@longClickListener callback?.onEventLongClicked(informationData, messageContent, view)
                                            ?: false
                                }
                    } else {
                        Timber.w("Ignored event (type: ${event.root.type}")
                        null
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "failed to create message item")
            defaultItemFactory.create(event, highlight, e)
        }
        return (computedModel ?: EmptyItem_())
    }

}