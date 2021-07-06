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
import im.vector.app.core.extensions.localDateTime
import im.vector.app.features.home.room.detail.timeline.factory.TimelineItemFactoryParams
import im.vector.app.features.home.room.detail.timeline.item.E2EDecoration
import im.vector.app.features.home.room.detail.timeline.item.MessageInformationData
import im.vector.app.features.home.room.detail.timeline.item.PollResponseData
import im.vector.app.features.home.room.detail.timeline.item.ReactionInfoData
import im.vector.app.features.home.room.detail.timeline.item.ReferencesInfoData
import im.vector.app.features.home.room.detail.timeline.item.SendStateDecoration
import im.vector.app.features.settings.VectorPreferences
import org.matrix.android.sdk.api.crypto.VerificationState
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.isAttachmentMessage
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.ReferencesAggregatedContent
import org.matrix.android.sdk.api.session.room.model.message.MessageVerificationRequestContent
import org.matrix.android.sdk.api.session.room.send.SendState
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.getLastMessageContent
import org.matrix.android.sdk.api.session.room.timeline.hasBeenEdited
import org.matrix.android.sdk.api.session.room.timeline.isEdition
import org.matrix.android.sdk.internal.crypto.model.event.EncryptedEventContent
import javax.inject.Inject

/**
 * TODO Update this comment
 * This class compute if data of an event (such has avatar, display name, ...) should be displayed, depending on the previous event in the timeline
 */
class MessageInformationDataFactory @Inject constructor(private val session: Session,
                                                        private val roomSummariesHolder: RoomSummariesHolder,
                                                        private val dateFormatter: VectorDateFormatter,
                                                        private val visibilityHelper: TimelineEventVisibilityHelper,
                                                        private val vectorPreferences: VectorPreferences) {

    fun create(params: TimelineItemFactoryParams): MessageInformationData {
        val event = params.event
        val nextDisplayableEvent = params.nextDisplayableEvent
        val eventId = event.eventId

        val date = event.root.localDateTime()
        val nextDate = nextDisplayableEvent?.root?.localDateTime()
        val addDaySeparator = date.toLocalDate() != nextDate?.toLocalDate()
        val isNextMessageReceivedMoreThanOneHourAgo = nextDate?.isBefore(date.minusMinutes(60))
                ?: false

        val showInformation =
                addDaySeparator
                        || event.senderInfo.avatarUrl != nextDisplayableEvent?.senderInfo?.avatarUrl
                        || event.senderInfo.disambiguatedDisplayName != nextDisplayableEvent?.senderInfo?.disambiguatedDisplayName
                        || nextDisplayableEvent.root.getClearType() !in listOf(EventType.MESSAGE, EventType.STICKER, EventType.ENCRYPTED)
                        || isNextMessageReceivedMoreThanOneHourAgo
                        || isTileTypeMessage(nextDisplayableEvent)
                        || nextDisplayableEvent.isEdition()

        val time = dateFormatter.format(event.root.originServerTs, DateFormatKind.MESSAGE_SIMPLE)
        val e2eDecoration = getE2EDecoration(event)

        // SendState Decoration
        val isSentByMe = event.root.senderId == session.myUserId
        val sendStateDecoration = if (isSentByMe) {
            getSendStateDecoration(
                    event = event,
                    lastSentEventWithoutReadReceipts = params.lastSentEventIdWithoutReadReceipts,
                    isMedia = event.root.isAttachmentMessage()
            )
        } else {
            SendStateDecoration.NONE
        }

        return MessageInformationData(
                eventId = eventId,
                senderId = event.root.senderId ?: "",
                sendState = event.root.sendState,
                time = time,
                ageLocalTS = event.root.ageLocalTs,
                avatarUrl = event.senderInfo.avatarUrl,
                memberName = event.senderInfo.disambiguatedDisplayName,
                showInformation = showInformation,
                forceShowTimestamp = vectorPreferences.alwaysShowTimeStamps(),
                orderedReactionList = event.annotations?.reactionsSummary
                        // ?.filter { isSingleEmoji(it.key) }
                        ?.map {
                            ReactionInfoData(it.key, it.count, it.addedByMe, it.localEchoEvents.isEmpty())
                        },
                pollResponseAggregatedSummary = event.annotations?.pollResponseSummary?.let {
                    PollResponseData(
                            myVote = it.aggregatedContent?.myVote,
                            isClosed = it.closedTime ?: Long.MAX_VALUE > System.currentTimeMillis(),
                            votes = it.aggregatedContent?.votes
                                    ?.groupBy({ it.optionIndex }, { it.userId })
                                    ?.mapValues { it.value.size }
                    )
                },
                hasBeenEdited = event.hasBeenEdited(),
                hasPendingEdits = event.annotations?.editSummary?.localEchos?.any() ?: false,
                referencesInfoData = event.annotations?.referencesAggregatedSummary?.let { referencesAggregatedSummary ->
                    val verificationState = referencesAggregatedSummary.content.toModel<ReferencesAggregatedContent>()?.verificationState
                            ?: VerificationState.REQUEST
                    ReferencesInfoData(verificationState)
                },
                sentByMe = isSentByMe,
                e2eDecoration = e2eDecoration,
                sendStateDecoration = sendStateDecoration
        )
    }

    private fun getSendStateDecoration(event: TimelineEvent,
                                       lastSentEventWithoutReadReceipts: String?,
                                       isMedia: Boolean): SendStateDecoration {
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

    private fun getE2EDecoration(event: TimelineEvent): E2EDecoration {
        val roomSummary = roomSummariesHolder.get(event.roomId)
        return if (
                event.root.sendState == SendState.SYNCED
                && roomSummary?.isEncrypted.orFalse()
                // is user verified
                && session.cryptoService().crossSigningService().getUserCrossSigningKeys(event.root.senderId ?: "")?.isTrusted() == true) {
            val ts = roomSummary?.encryptionEventTs ?: 0
            val eventTs = event.root.originServerTs ?: 0
            if (event.isEncrypted()) {
                // Do not decorate failed to decrypt, or redaction (we lost sender device info)
                if (event.root.getClearType() == EventType.ENCRYPTED || event.root.isRedacted()) {
                    E2EDecoration.NONE
                } else {
                    val sendingDevice = event.root.content
                            .toModel<EncryptedEventContent>()
                            ?.deviceId
                            ?.let { deviceId ->
                                session.cryptoService().getDeviceInfo(event.root.senderId ?: "", deviceId)
                            }
                    when {
                        sendingDevice == null                            -> {
                            // For now do not decorate this with warning
                            // maybe it's a deleted session
                            E2EDecoration.NONE
                        }
                        sendingDevice.trustLevel == null                 -> {
                            E2EDecoration.WARN_SENT_BY_UNKNOWN
                        }
                        sendingDevice.trustLevel?.isVerified().orFalse() -> {
                            E2EDecoration.NONE
                        }
                        else                                             -> {
                            E2EDecoration.WARN_SENT_BY_UNVERIFIED
                        }
                    }
                }
            } else {
                if (event.root.isStateEvent()) {
                    // Do not warn for state event, they are always in clear
                    E2EDecoration.NONE
                } else {
                    // If event is in clear after the room enabled encryption we should warn
                    if (eventTs > ts) E2EDecoration.WARN_IN_CLEAR else E2EDecoration.NONE
                }
            }
        } else {
            E2EDecoration.NONE
        }
    }

    /**
     * Tiles type message never show the sender information (like verification request), so we should repeat it for next message
     * even if same sender
     */
    private fun isTileTypeMessage(event: TimelineEvent?): Boolean {
        return when (event?.root?.getClearType()) {
            EventType.KEY_VERIFICATION_DONE,
            EventType.KEY_VERIFICATION_CANCEL -> true
            EventType.MESSAGE                 -> {
                event.getLastMessageContent() is MessageVerificationRequestContent
            }
            else                              -> false
        }
    }
}
