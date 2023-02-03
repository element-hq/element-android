/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.app.features.home.room.detail.timeline.helper

import im.vector.app.core.date.DateFormatKind
import im.vector.app.core.date.VectorDateFormatter
import im.vector.app.core.extensions.getVectorLastMessageContent
import im.vector.app.core.extensions.localDateTime
import im.vector.app.features.home.room.detail.timeline.factory.TimelineItemFactoryParams
import im.vector.app.features.home.room.detail.timeline.item.E2EDecoration
import im.vector.app.features.home.room.detail.timeline.item.MessageInformationData
import im.vector.app.features.home.room.detail.timeline.item.ReferencesInfoData
import im.vector.app.features.home.room.detail.timeline.item.SendStateDecoration
import im.vector.app.features.home.room.detail.timeline.style.TimelineMessageLayoutFactory
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.verification.VerificationState
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.getMsgType
import org.matrix.android.sdk.api.session.events.model.isAttachmentMessage
import org.matrix.android.sdk.api.session.events.model.isSticker
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.events.model.toValidDecryptedEvent
import org.matrix.android.sdk.api.session.room.model.ReferencesAggregatedContent
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.message.MessageType
import org.matrix.android.sdk.api.session.room.model.message.MessageVerificationRequestContent
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.hasBeenEdited
import javax.inject.Inject

/**
 * This class is responsible of building extra information data associated to a given event.
 */
class MessageInformationDataFactory @Inject constructor(
        private val session: Session,
        private val dateFormatter: VectorDateFormatter,
        private val messageLayoutFactory: TimelineMessageLayoutFactory,
        private val reactionsSummaryFactory: ReactionsSummaryFactory,
        private val pollResponseDataFactory: PollResponseDataFactory,
) {

    fun create(params: TimelineItemFactoryParams): MessageInformationData {
        val event = params.event
        val nextDisplayableEvent = params.nextDisplayableEvent
        val prevDisplayableEvent = params.prevDisplayableEvent
        val eventId = event.eventId
        val isSentByMe = event.root.senderId == session.myUserId
        val roomSummary = params.partialState.roomSummary

        val date = event.root.localDateTime()
        val nextDate = nextDisplayableEvent?.root?.localDateTime()
        val addDaySeparator = date.toLocalDate() != nextDate?.toLocalDate()

        val isFirstFromThisSender = nextDisplayableEvent?.root?.senderId != event.root.senderId || addDaySeparator
        val isLastFromThisSender = prevDisplayableEvent?.root?.senderId != event.root.senderId ||
                prevDisplayableEvent?.root?.localDateTime()?.toLocalDate() != date.toLocalDate()

        val time = dateFormatter.format(event.root.originServerTs, DateFormatKind.MESSAGE_SIMPLE)
        val e2eDecoration = getE2EDecoration(roomSummary, params.lastEdit ?: event.root)
        val senderId = getSenderId(event)
        // SendState Decoration
        val sendStateDecoration = if (isSentByMe) {
            getSendStateDecoration(
                    event = event,
                    lastSentEventWithoutReadReceipts = params.lastSentEventIdWithoutReadReceipts,
                    isMedia = event.root.isAttachmentMessage()
            )
        } else {
            SendStateDecoration.NONE
        }

        val messageLayout = messageLayoutFactory.create(params)

        return MessageInformationData(
                eventId = eventId,
                senderId = senderId,
                sendState = event.root.sendState,
                time = time,
                ageLocalTS = event.root.ageLocalTs,
                avatarUrl = event.senderInfo.avatarUrl,
                memberName = event.senderInfo.disambiguatedDisplayName,
                messageLayout = messageLayout,
                reactionsSummary = reactionsSummaryFactory.create(event),
                pollResponseAggregatedSummary = pollResponseDataFactory.create(event),
                hasBeenEdited = event.hasBeenEdited(),
                hasPendingEdits = event.annotations?.editSummary?.localEchos?.any() ?: false,
                referencesInfoData = event.annotations?.referencesAggregatedSummary?.let { referencesAggregatedSummary ->
                    val verificationState = referencesAggregatedSummary.content.toModel<ReferencesAggregatedContent>()?.verificationState
                            ?: VerificationState.REQUEST
                    ReferencesInfoData(verificationState)
                },
                sentByMe = isSentByMe,
                isFirstFromThisSender = isFirstFromThisSender,
                isLastFromThisSender = isLastFromThisSender,
                e2eDecoration = e2eDecoration,
                sendStateDecoration = sendStateDecoration,
                messageType = if (event.root.isSticker()) {
                    MessageType.MSGTYPE_STICKER_LOCAL
                } else {
                    event.root.getMsgType()
                }
        )
    }

    private fun getSenderId(event: TimelineEvent) = if (event.isEncrypted()) {
        event.root.toValidDecryptedEvent()?.let {
            session.cryptoService().deviceWithIdentityKey(it.cryptoSenderKey, it.algorithm)?.userId
        } ?: event.root.senderId.orEmpty()
    } else {
        event.root.senderId.orEmpty()
    }

    private fun getSendStateDecoration(
            event: TimelineEvent,
            lastSentEventWithoutReadReceipts: String?,
            isMedia: Boolean
    ): SendStateDecoration {
        val eventSendState = event.root.sendState
        return if (eventSendState.isSending()) {
            if (isMedia) SendStateDecoration.SENDING_MEDIA else SendStateDecoration.SENDING_NON_MEDIA
        } else if (eventSendState.hasFailed()) {
            SendStateDecoration.FAILED
        } else if (lastSentEventWithoutReadReceipts == event.eventId) {
            SendStateDecoration.SENT
        } else {
            SendStateDecoration.NONE
        }
    }

    private fun getE2EDecoration(roomSummary: RoomSummary?, event: Event): E2EDecoration {
        if (roomSummary?.isEncrypted != true) {
            // No decoration for clear room
            // Questionable? what if the event is E2E?
            return E2EDecoration.NONE
        }
        if (event.sendState != SendState.SYNCED) {
            // we don't display e2e decoration if event not synced back
            return E2EDecoration.NONE
        }
        val userCrossSigningInfo = session.cryptoService()
                .crossSigningService()
                .getUserCrossSigningKeys(event.senderId.orEmpty())

        if (userCrossSigningInfo?.isTrusted() == true) {
            return if (event.isEncrypted()) {
                // Do not decorate failed to decrypt, or redaction (we lost sender device info)
                if (event.getClearType() == EventType.ENCRYPTED || event.isRedacted()) {
                    E2EDecoration.NONE
                } else {
                    val sendingDevice = event.getSenderKey()
                            ?.let {
                                session.cryptoService().deviceWithIdentityKey(
                                        it,
                                        event.content?.get("algorithm") as? String ?: ""
                                )
                            }
                    if (event.mxDecryptionResult?.isSafe == false) {
                        E2EDecoration.WARN_UNSAFE_KEY
                    } else {
                        when {
                            sendingDevice == null -> {
                                // For now do not decorate this with warning
                                // maybe it's a deleted session
                                E2EDecoration.WARN_SENT_BY_DELETED_SESSION
                            }
                            sendingDevice.trustLevel == null -> {
                                E2EDecoration.WARN_SENT_BY_UNKNOWN
                            }
                            sendingDevice.trustLevel?.isVerified().orFalse() -> {
                                E2EDecoration.NONE
                            }
                            else -> {
                                E2EDecoration.WARN_SENT_BY_UNVERIFIED
                            }
                        }
                    }
                }
            } else {
                e2EDecorationForClearEventInE2ERoom(event, roomSummary)
            }
        } else {
            return if (!event.isEncrypted()) {
                e2EDecorationForClearEventInE2ERoom(event, roomSummary)
            } else if (event.mxDecryptionResult != null) {
                if (event.mxDecryptionResult?.isSafe == true) {
                    E2EDecoration.NONE
                } else {
                    E2EDecoration.WARN_UNSAFE_KEY
                }
            } else {
                E2EDecoration.NONE
            }
        }
    }

    private fun e2EDecorationForClearEventInE2ERoom(event: Event, roomSummary: RoomSummary) =
            if (event.isStateEvent()) {
                // Do not warn for state event, they are always in clear
                E2EDecoration.NONE
            } else {
                val ts = roomSummary.encryptionEventTs ?: 0
                val eventTs = event.originServerTs ?: 0
                // If event is in clear after the room enabled encryption we should warn
                if (eventTs > ts) E2EDecoration.WARN_IN_CLEAR else E2EDecoration.NONE
            }

    /**
     * Tiles type message never show the sender information (like verification request), so we should repeat it for next message
     * even if same sender.
     */
    private fun isTileTypeMessage(event: TimelineEvent?): Boolean {
        return when (event?.root?.getClearType()) {
            EventType.KEY_VERIFICATION_DONE,
            EventType.KEY_VERIFICATION_CANCEL -> true
            EventType.MESSAGE -> {
                event.getVectorLastMessageContent() is MessageVerificationRequestContent
            }
            else -> false
        }
    }
}
