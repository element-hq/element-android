/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.factory

import im.vector.app.core.epoxy.TimelineEmptyItem
import im.vector.app.core.epoxy.TimelineEmptyItem_
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.features.analytics.DecryptionFailureTracker
import im.vector.app.features.home.room.detail.timeline.helper.TimelineEventVisibilityHelper
import im.vector.app.features.voicebroadcast.VoiceBroadcastConstants
import im.vector.app.features.voicebroadcast.model.isVoiceBroadcast
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.RelationType
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.getRelationContent
import timber.log.Timber
import javax.inject.Inject

class TimelineItemFactory @Inject constructor(
        private val messageItemFactory: MessageItemFactory,
        private val encryptedItemFactory: EncryptedItemFactory,
        private val noticeItemFactory: NoticeItemFactory,
        private val defaultItemFactory: DefaultItemFactory,
        private val encryptionItemFactory: EncryptionItemFactory,
        private val roomCreateItemFactory: RoomCreateItemFactory,
        private val widgetItemFactory: WidgetItemFactory,
        private val verificationConclusionItemFactory: VerificationItemFactory,
        private val callItemFactory: CallItemFactory,
        private val elementCallItemFactory: ElementCallItemFactory,
        private val decryptionFailureTracker: DecryptionFailureTracker,
        private val timelineEventVisibilityHelper: TimelineEventVisibilityHelper,
        private val session: Session,
) {

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
                            rootThreadEventId = params.rootThreadEventId
                    )) {
                return buildEmptyItem(
                        event,
                        params.prevEvent,
                        params.highlightedEventId,
                        params.rootThreadEventId,
                        params.isFromThreadTimeline()
                )
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
                    EventType.STATE_ROOM_WIDGET -> widgetItemFactory.create(params)
                    EventType.STATE_ROOM_ENCRYPTION -> encryptionItemFactory.create(params)
                    // State room create
                    EventType.STATE_ROOM_CREATE -> roomCreateItemFactory.create(params)
                    in EventType.STATE_ROOM_BEACON_INFO.values -> messageItemFactory.create(params)
                    VoiceBroadcastConstants.STATE_ROOM_VOICE_BROADCAST_INFO -> messageItemFactory.create(params)
                    // Unhandled state event types
                    else -> {
                        // Should only happen when shouldShowHiddenEvents() settings is ON
                        Timber.v("State event type ${event.root.type} not handled")
                        defaultItemFactory.create(params)
                    }
                }
            } else {
                when (event.root.getClearType()) {
                    // Message itemsX
                    EventType.STICKER,
                    in EventType.POLL_START.values,
                    in EventType.POLL_END.values,
                    EventType.MESSAGE -> messageItemFactory.create(params)
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
                    in EventType.POLL_RESPONSE.values -> noticeItemFactory.create(params)
                    in EventType.BEACON_LOCATION_DATA.values -> {
                        if (event.root.isRedacted()) {
                            messageItemFactory.create(params)
                        } else {
                            noticeItemFactory.create(params)
                        }
                    }
                    // Element Call
                    in EventType.ELEMENT_CALL_NOTIFY.values -> elementCallItemFactory.create(params)
                    // Calls
                    EventType.CALL_INVITE,
                    EventType.CALL_HANGUP,
                    EventType.CALL_REJECT,
                    EventType.CALL_ANSWER -> callItemFactory.create(params)
                    // Crypto
                    EventType.ENCRYPTED -> {
                        val relationContent = event.getRelationContent()
                        when {
                            // Redacted event, let the MessageItemFactory handle it
                            event.root.isRedacted() -> messageItemFactory.create(params)
                            relationContent?.type == RelationType.REFERENCE -> {
                                // Hide the decryption error for VoiceBroadcast chunks
                                val relatedEvent = relationContent.eventId?.let { session.eventService().getEventFromCache(event.roomId, it) }
                                if (relatedEvent?.isVoiceBroadcast() != true) encryptedItemFactory.create(params) else null
                            }
                            else -> encryptedItemFactory.create(params)
                        }
                    }
                    EventType.KEY_VERIFICATION_CANCEL,
                    EventType.KEY_VERIFICATION_DONE -> {
                        verificationConclusionItemFactory.create(params)
                    }
                    // Unhandled event types
                    else -> {
                        // Should only happen when shouldShowHiddenEvents() settings is ON
                        Timber.v("Type ${event.root.getClearType()} not handled")
                        defaultItemFactory.create(params)
                    }
                }.also {
                    if (it != null && event.isEncrypted() && event.root.mCryptoError != null) {
                        decryptionFailureTracker.utdDisplayedInTimeline(event)
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
                params.isFromThreadTimeline()
        )
    }

    private fun buildEmptyItem(
            timelineEvent: TimelineEvent,
            prevEvent: TimelineEvent?,
            highlightedEventId: String?,
            rootThreadEventId: String?,
            isFromThreadTimeline: Boolean
    ): TimelineEmptyItem {
        val isNotBlank = prevEvent == null || timelineEventVisibilityHelper.shouldShowEvent(
                timelineEvent = prevEvent,
                highlightedEventId = highlightedEventId,
                isFromThreadTimeline = isFromThreadTimeline,
                rootThreadEventId = rootThreadEventId
        )
        return TimelineEmptyItem_()
                .id(timelineEvent.localId)
                .eventId(timelineEvent.eventId)
                .notBlank(isNotBlank)
    }
}
