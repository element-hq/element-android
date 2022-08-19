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
package im.vector.app.features.notifications

import android.net.Uri
import im.vector.app.R
import im.vector.app.core.extensions.takeAs
import im.vector.app.core.resources.BuildMeta
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.time.Clock
import im.vector.app.features.displayname.getBestName
import im.vector.app.features.home.room.detail.timeline.format.DisplayableEventFormatter
import im.vector.app.features.home.room.detail.timeline.format.NoticeEventFormatter
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.content.ContentUrlResolver
import org.matrix.android.sdk.api.session.crypto.MXCryptoError
import org.matrix.android.sdk.api.session.crypto.model.OlmDecryptionResult
import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.isEdition
import org.matrix.android.sdk.api.session.events.model.isImageMessage
import org.matrix.android.sdk.api.session.events.model.supportsNotification
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.getRoomSummary
import org.matrix.android.sdk.api.session.getUser
import org.matrix.android.sdk.api.session.room.getTimelineEvent
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomMemberContent
import org.matrix.android.sdk.api.session.room.model.message.MessageWithAttachmentContent
import org.matrix.android.sdk.api.session.room.sender.SenderInfo
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.getEditedEventId
import org.matrix.android.sdk.api.session.room.timeline.getLastMessageContent
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

    private val nonEncryptedNotifiableEventTypes: List<String> =
            listOf(EventType.MESSAGE) + EventType.POLL_START + EventType.STATE_ROOM_BEACON_INFO

    suspend fun resolveEvent(event: Event, session: Session, isNoisy: Boolean): NotifiableEvent? {
        val roomID = event.roomId ?: return null
        val eventId = event.eventId ?: return null
        if (event.getClearType() == EventType.STATE_ROOM_MEMBER) {
            return resolveStateRoomEvent(event, session, canBeReplaced = false, isNoisy = isNoisy)
        }
        val timelineEvent = session.getRoom(roomID)?.getTimelineEvent(eventId) ?: return null
        return when (event.getClearType()) {
            in nonEncryptedNotifiableEventTypes,
            EventType.ENCRYPTED -> {
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
                        title = stringProvider.getString(R.string.notification_unknown_new_event),
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
            val user = session.getUser(event.senderId!!) ?: return null

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

    private suspend fun resolveMessageEvent(event: TimelineEvent, session: Session, canBeReplaced: Boolean, isNoisy: Boolean): NotifiableEvent? {
        // The event only contains an eventId, and roomId (type is m.room.*) , we need to get the displayable content (names, avatar, text, etc...)
        val room = session.getRoom(event.root.roomId!! /*roomID cannot be null*/)

        return if (room == null) {
            Timber.e("## Unable to resolve room for eventId [$event]")
            // Ok room is not known in store, but we can still display something
            val body = displayableEventFormatter.format(event, isDm = false, appendAuthor = false)
            val roomName = stringProvider.getString(R.string.notification_unknown_room_name)
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
                    roomName = roomName,
                    matrixID = session.myUserId
            )
        } else {
            event.attemptToDecryptIfNeeded(session)
            // only convert encrypted messages to NotifiableMessageEvents
            when (event.root.getClearType()) {
                in nonEncryptedNotifiableEventTypes -> {
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
                        forwardingCurve25519KeyChain = result.forwardingCurve25519KeyChain
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
            getLastMessageContent()?.takeAs<MessageWithAttachmentContent>()?.let { imageMessage ->
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
                    ?: stringProvider.getString(R.string.notification_new_invitation)
            return InviteNotifiableEvent(
                    session.myUserId,
                    eventId = event.eventId!!,
                    editedEventId = null,
                    canBeReplaced = canBeReplaced,
                    roomId = roomId,
                    roomName = roomSummary?.displayName,
                    timestamp = event.originServerTs ?: 0,
                    noisy = isNoisy,
                    title = stringProvider.getString(R.string.notification_new_invitation),
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
