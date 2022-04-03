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

package im.vector.app.features.home.room.detail.timeline.factory

import im.vector.app.core.epoxy.TimelineEmptyItem
import im.vector.app.core.epoxy.TimelineEmptyItem_
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.features.analytics.DecryptionFailureTracker
import im.vector.app.features.home.room.detail.timeline.helper.TimelineEventVisibilityHelper
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import timber.log.Timber
import javax.inject.Inject

class TimelineItemFactory @Inject constructor(private val messageItemFactory: MessageItemFactory,
                                              private val encryptedItemFactory: EncryptedItemFactory,
                                              private val noticeItemFactory: NoticeItemFactory,
                                              private val defaultItemFactory: DefaultItemFactory,
                                              private val encryptionItemFactory: EncryptionItemFactory,
                                              private val roomCreateItemFactory: RoomCreateItemFactory,
                                              private val widgetItemFactory: WidgetItemFactory,
                                              private val verificationConclusionItemFactory: VerificationItemFactory,
                                              private val callItemFactory: CallItemFactory,
                                              private val decryptionFailureTracker: DecryptionFailureTracker,
                                              private val timelineEventVisibilityHelper: TimelineEventVisibilityHelper) {

    /**
     * Reminder: nextEvent is older and prevEvent is newer.
     */
    fun create(params: TimelineItemFactoryParams): VectorEpoxyModel<*> {
        val event = params.event
        val computedModel = try {
            if (!timelineEventVisibilityHelper.shouldShowEvent(
                            timelineEvent = event,
                            highlightedEventId = params.highlightedEventId,
                            isFromThreadTimeline = params.isFromThreadTimeline(),
                            rootThreadEventId = params.rootThreadEventId)) {
                return buildEmptyItem(
                        event,
                        params.prevEvent,
                        params.highlightedEventId,
                        params.rootThreadEventId,
                        params.isFromThreadTimeline())
            }

            // Manage state event differently, to check validity
            if (event.root.isStateEvent()) {
                // state event are not e2e
                when (event.root.type) {
                    EventType.STATE_ROOM_TOMBSTONE,
                    EventType.STATE_ROOM_NAME,
                    EventType.STATE_ROOM_TOPIC,
                    EventType.STATE_ROOM_AVATAR,
                    EventType.STATE_ROOM_MEMBER,
                    EventType.STATE_ROOM_THIRD_PARTY_INVITE,
                    EventType.STATE_ROOM_CANONICAL_ALIAS,
                    EventType.STATE_ROOM_JOIN_RULES,
                    EventType.STATE_ROOM_HISTORY_VISIBILITY,
                    EventType.STATE_ROOM_SERVER_ACL,
                    EventType.STATE_ROOM_GUEST_ACCESS,
                    EventType.STATE_ROOM_ALIASES,
                    EventType.STATE_SPACE_CHILD,
                    EventType.STATE_SPACE_PARENT,
                    EventType.STATE_ROOM_POWER_LEVELS -> {
                        noticeItemFactory.create(params)
                    }
                    EventType.STATE_ROOM_WIDGET_LEGACY,
                    EventType.STATE_ROOM_WIDGET       -> widgetItemFactory.create(params)
                    EventType.STATE_ROOM_ENCRYPTION   -> encryptionItemFactory.create(params)
                    // State room create
                    EventType.STATE_ROOM_CREATE       -> roomCreateItemFactory.create(params)
                    // Unhandled state event types
                    else                              -> {
                        // Should only happen when shouldShowHiddenEvents() settings is ON
                        Timber.v("State event type ${event.root.type} not handled")
                        defaultItemFactory.create(params)
                    }
                }
            } else {
                when (event.root.getClearType()) {
                    // Message itemsX
                    EventType.STICKER,
                    in EventType.POLL_START,
                    EventType.MESSAGE               -> messageItemFactory.create(params)
                    EventType.REDACTION,
                    EventType.KEY_VERIFICATION_ACCEPT,
                    EventType.KEY_VERIFICATION_START,
                    EventType.KEY_VERIFICATION_KEY,
                    EventType.KEY_VERIFICATION_READY,
                    EventType.KEY_VERIFICATION_MAC,
                    EventType.CALL_CANDIDATES,
                    EventType.CALL_REPLACES,
                    EventType.CALL_SELECT_ANSWER,
                    EventType.CALL_NEGOTIATE,
                    EventType.REACTION,
                    in EventType.POLL_RESPONSE,
                    in EventType.POLL_END           -> noticeItemFactory.create(params)
                    // Calls
                    EventType.CALL_INVITE,
                    EventType.CALL_HANGUP,
                    EventType.CALL_REJECT,
                    EventType.CALL_ANSWER           -> callItemFactory.create(params)
                    // Crypto
                    EventType.ENCRYPTED             -> {
                        if (event.root.isRedacted()) {
                            // Redacted event, let the MessageItemFactory handle it
                            messageItemFactory.create(params)
                        } else {
                            encryptedItemFactory.create(params)
                        }
                    }
                    EventType.KEY_VERIFICATION_CANCEL,
                    EventType.KEY_VERIFICATION_DONE -> {
                        verificationConclusionItemFactory.create(params)
                    }
                    // Unhandled event types
                    else                            -> {
                        // Should only happen when shouldShowHiddenEvents() settings is ON
                        Timber.v("Type ${event.root.getClearType()} not handled")
                        defaultItemFactory.create(params)
                    }
                }.also {
                    if (it != null && event.isEncrypted()) {
                        decryptionFailureTracker.e2eEventDisplayedInTimeline(event)
                    }
                }
            }
        } catch (throwable: Throwable) {
            Timber.e(throwable, "failed to create message item")
            defaultItemFactory.create(params, throwable)
        }
        return computedModel ?: buildEmptyItem(
                event,
                params.prevEvent,
                params.highlightedEventId,
                params.rootThreadEventId,
                params.isFromThreadTimeline())
    }

    private fun buildEmptyItem(timelineEvent: TimelineEvent,
                               prevEvent: TimelineEvent?,
                               highlightedEventId: String?,
                               rootThreadEventId: String?,
                               isFromThreadTimeline: Boolean): TimelineEmptyItem {
        val isNotBlank = prevEvent == null || timelineEventVisibilityHelper.shouldShowEvent(
                timelineEvent = prevEvent,
                highlightedEventId = highlightedEventId,
                isFromThreadTimeline = isFromThreadTimeline,
                rootThreadEventId = rootThreadEventId)
        return TimelineEmptyItem_()
                .id(timelineEvent.localId)
                .eventId(timelineEvent.eventId)
                .notBlank(isNotBlank)
    }
}
