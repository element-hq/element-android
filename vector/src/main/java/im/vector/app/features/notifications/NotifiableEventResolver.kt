/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.notifications

import android.net.Uri
import im.vector.app.core.extensions.getVectorLastMessageContent
import im.vector.app.core.extensions.takeAs
import im.vector.app.core.resources.BuildMeta
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.displayname.getBestName
import im.vector.app.features.home.room.detail.timeline.format.DisplayableEventFormatter
import im.vector.app.features.home.room.detail.timeline.format.NoticeEventFormatter
import im.vector.lib.core.utils.timer.Clock
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.content.ContentUrlResolver
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.api.session.crypto.model.OlmDecryptionResult
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.getRootThreadEventId
import org.matrix.android.sdk.api.session.events.model.isEdition
import org.matrix.android.sdk.api.session.events.model.isImageMessage
import org.matrix.android.sdk.api.session.events.model.supportsNotification
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.getRoomSummary
import org.matrix.android.sdk.api.session.getUserOrDefault
import org.matrix.android.sdk.api.session.room.getTimelineEvent
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.api.session.room.model.message.ElementCallNotifyContent
import org.matrix.android.sdk.api.session.room.model.message.MessageWithAttachmentContent
import org.matrix.android.sdk.api.session.room.model.message.isUserMentioned
import org.matrix.android.sdk.api.session.room.sender.SenderInfo
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.getEditedEventId
import org.matrix.android.sdk.api.util.toMatrixItem
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

/**
 * The notifiable event resolver is able to create a NotifiableEvent (view model for notifications) from an sdk Event.
 * It is used as a bridge between the Event Thread and the NotificationDrawerManager.
 * The NotifiableEventResolver is the only aware of session/store, the NotificationDrawerManager has no knowledge of that,
 * this pattern allow decoupling between the object responsible of displaying notifications and the matrix sdk.
 */
class NotifiableEventResolver @Inject constructor(
        private val stringProvider: StringProvider,
        private val noticeEventFormatter: NoticeEventFormatter,
        private val displayableEventFormatter: DisplayableEventFormatter,
        private val clock: Clock,
        private val buildMeta: BuildMeta,
) {

    suspend fun resolveEvent(event: Event, session: Session, isNoisy: Boolean): NotifiableEvent? {
        val roomID = event.roomId ?: return null
        val eventId = event.eventId ?: return null
        if (event.getClearType() == EventType.STATE_ROOM_MEMBER) {
            return resolveStateRoomEvent(event, session, canBeReplaced = false, isNoisy = isNoisy)
        }
        val timelineEvent = session.getRoom(roomID)?.getTimelineEvent(eventId) ?: return null
        return when {
            event.supportsNotification() || event.type == EventType.ENCRYPTED -> {
                resolveMessageEvent(timelineEvent, session, canBeReplaced = false, isNoisy = isNoisy)
            }
            else -> {
                // If the event can be displayed, display it as is
                Timber.w("NotifiableEventResolver Received an unsupported event matching a bing rule")
                // TODO Better event text display
                val bodyPreview = event.type ?: EventType.MISSING_TYPE

                SimpleNotifiableEvent(
                        session.myUserId,
                        eventId = event.eventId!!,
                        editedEventId = timelineEvent.getEditedEventId(),
                        noisy = false, // will be updated
                        timestamp = event.originServerTs ?: clock.epochMillis(),
                        description = bodyPreview,
                        title = stringProvider.getString(CommonStrings.notification_unknown_new_event),
                        soundName = null,
                        type = event.type,
                        canBeReplaced = false
                )
            }
        }
    }

    suspend fun resolveInMemoryEvent(session: Session, event: Event, canBeReplaced: Boolean): NotifiableEvent? {
        if (!event.supportsNotification()) return null

        // Ignore message edition
        if (event.isEdition()) return null

        val actions = session.pushRuleService().getActions(event)
        val notificationAction = actions.toNotificationAction()

        return if (notificationAction.shouldNotify) {
            val user = session.getUserOrDefault(event.senderId!!)

            val timelineEvent = TimelineEvent(
                    root = event,
                    localId = -1,
                    eventId = event.eventId!!,
                    displayIndex = 0,
                    senderInfo = SenderInfo(
                            userId = user.userId,
                            displayName = user.toMatrixItem().getBestName(),
                            isUniqueDisplayName = true,
                            avatarUrl = user.avatarUrl
                    )
            )
            resolveMessageEvent(timelineEvent, session, canBeReplaced = canBeReplaced, isNoisy = !notificationAction.soundName.isNullOrBlank())
        } else {
            Timber.d("Matched push rule is set to not notify")
            null
        }
    }

    private suspend fun resolveMessageEvent(event: TimelineEvent, session: Session, canBeReplaced: Boolean, isNoisy: Boolean): NotifiableMessageEvent? {
        // The event only contains an eventId, and roomId (type is m.room.*) , we need to get the displayable content (names, avatar, text, etc...)
        val room = session.getRoom(event.root.roomId!! /*roomID cannot be null*/)

        return if (room == null) {
            Timber.e("## Unable to resolve room for eventId [$event]")
            // Ok room is not known in store, but we can still display something
            val body = displayableEventFormatter.format(event, isDm = false, appendAuthor = false)
            val roomName = stringProvider.getString(CommonStrings.notification_unknown_room_name)
            val senderDisplayName = event.senderInfo.disambiguatedDisplayName

            NotifiableMessageEvent(
                    eventId = event.root.eventId!!,
                    editedEventId = event.getEditedEventId(),
                    canBeReplaced = canBeReplaced,
                    timestamp = event.root.originServerTs ?: 0,
                    noisy = isNoisy,
                    senderName = senderDisplayName,
                    senderId = event.root.senderId,
                    body = body.toString(),
                    imageUriString = event.fetchImageIfPresent(session)?.toString(),
                    roomId = event.root.roomId!!,
                    threadId = event.root.getRootThreadEventId(),
                    roomName = roomName,
                    matrixID = session.myUserId
            )
        } else {
            event.attemptToDecryptIfNeeded(session)
            // For incoming Element Call, check that the user is mentioned
            val isIncomingElementCall = event.root.getClearType() in EventType.ELEMENT_CALL_NOTIFY.values &&
                    event.root.getClearContent()?.toModel<ElementCallNotifyContent>()?.isUserMentioned(session.myUserId) == true
            when {
                isIncomingElementCall || event.root.supportsNotification() -> {
                    val body = displayableEventFormatter.format(event, isDm = room.roomSummary()?.isDirect.orFalse(), appendAuthor = false).toString()
                    val roomName = room.roomSummary()?.displayName ?: ""
                    val senderDisplayName = event.senderInfo.disambiguatedDisplayName

                    NotifiableMessageEvent(
                            eventId = event.root.eventId!!,
                            editedEventId = event.getEditedEventId(),
                            canBeReplaced = canBeReplaced,
                            timestamp = event.root.originServerTs ?: 0,
                            noisy = isNoisy,
                            senderName = senderDisplayName,
                            senderId = event.root.senderId,
                            body = body,
                            imageUriString = event.fetchImageIfPresent(session)?.toString(),
                            roomId = event.root.roomId!!,
                            threadId = event.root.getRootThreadEventId(),
                            roomName = roomName,
                            roomIsDirect = room.roomSummary()?.isDirect ?: false,
                            roomAvatarPath = session.contentUrlResolver()
                                    .resolveThumbnail(
                                            room.roomSummary()?.avatarUrl,
                                            250,
                                            250,
                                            ContentUrlResolver.ThumbnailMethod.SCALE
                                    ),
                            senderAvatarPath = session.contentUrlResolver()
                                    .resolveThumbnail(
                                            event.senderInfo.avatarUrl,
                                            250,
                                            250,
                                            ContentUrlResolver.ThumbnailMethod.SCALE
                                    ),
                            matrixID = session.myUserId,
                            soundName = null
                    )
                }
                else -> null
            }
        }
    }

    private suspend fun TimelineEvent.attemptToDecryptIfNeeded(session: Session) {
        if (root.isEncrypted() && root.mxDecryptionResult == null) {
            // TODO use a global event decryptor? attache to session and that listen to new sessionId?
            // for now decrypt sync
            try {
                val result = session.cryptoService().decryptEvent(root, root.roomId + UUID.randomUUID().toString())
                root.mxDecryptionResult = OlmDecryptionResult(
                        payload = result.clearEvent,
                        senderKey = result.senderCurve25519Key,
                        keysClaimed = result.claimedEd25519Key?.let { mapOf("ed25519" to it) },
                        forwardingCurve25519KeyChain = result.forwardingCurve25519KeyChain,
                        verificationState = result.messageVerificationState
                )
            } catch (ignore: MXCryptoError) {
            }
        }
    }

    private suspend fun TimelineEvent.fetchImageIfPresent(session: Session): Uri? {
        return when {
            root.isEncrypted() && root.mxDecryptionResult == null -> null
            root.isImageMessage() -> downloadAndExportImage(session)
            else -> null
        }
    }

    private suspend fun TimelineEvent.downloadAndExportImage(session: Session): Uri? {
        return kotlin.runCatching {
            getVectorLastMessageContent()?.takeAs<MessageWithAttachmentContent>()?.let { imageMessage ->
                val fileService = session.fileService()
                fileService.downloadFile(imageMessage)
                fileService.getTemporarySharableURI(imageMessage)
            }
        }.onFailure {
            Timber.e(it, "Failed to download and export image for notification")
        }.getOrNull()
    }

    private fun resolveStateRoomEvent(event: Event, session: Session, canBeReplaced: Boolean, isNoisy: Boolean): NotifiableEvent? {
        val content = event.content?.toModel<RoomMemberContent>() ?: return null
        val roomId = event.roomId ?: return null
        val dName = event.senderId?.let { session.roomService().getRoomMember(it, roomId)?.displayName }
        if (Membership.INVITE == content.membership) {
            val roomSummary = session.getRoomSummary(roomId)
            val body = noticeEventFormatter.format(event, dName, isDm = roomSummary?.isDirect.orFalse())
                    ?: stringProvider.getString(CommonStrings.notification_new_invitation)
            return InviteNotifiableEvent(
                    session.myUserId,
                    eventId = event.eventId!!,
                    editedEventId = null,
                    canBeReplaced = canBeReplaced,
                    roomId = roomId,
                    roomName = roomSummary?.displayName,
                    timestamp = event.originServerTs ?: 0,
                    noisy = isNoisy,
                    title = stringProvider.getString(CommonStrings.notification_new_invitation),
                    description = body.toString(),
                    soundName = null, // will be set later
                    type = event.getClearType()
            )
        } else {
            Timber.e("## unsupported notifiable event for eventId [${event.eventId}]")
            if (buildMeta.lowPrivacyLoggingEnabled) {
                Timber.e("## unsupported notifiable event for event [$event]")
            }
            // TODO generic handling?
        }
        return null
    }
}
