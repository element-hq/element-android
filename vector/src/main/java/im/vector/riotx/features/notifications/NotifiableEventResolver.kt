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
package im.vector.riotx.features.notifications

import androidx.core.app.NotificationCompat
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.content.ContentUrlResolver
import im.vector.matrix.android.api.session.crypto.MXCryptoError
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.Membership
import im.vector.matrix.android.api.session.room.model.RoomMember
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.matrix.android.api.session.room.timeline.getEditedEventId
import im.vector.matrix.android.api.session.room.timeline.getLastMessageBody
import im.vector.matrix.android.internal.crypto.algorithms.olm.OlmDecryptionResult
import im.vector.riotx.BuildConfig
import im.vector.riotx.R
import im.vector.riotx.core.resources.StringProvider
import im.vector.riotx.features.home.room.detail.timeline.format.NoticeEventFormatter
import timber.log.Timber
import java.util.*
import javax.inject.Inject

/**
 * The notifiable event resolver is able to create a NotifiableEvent (view model for notifications) from an sdk Event.
 * It is used as a bridge between the Event Thread and the NotificationDrawerManager.
 * The NotifiableEventResolver is the only aware of session/store, the NotificationDrawerManager has no knowledge of that,
 * this pattern allow decoupling between the object responsible of displaying notifications and the matrix sdk.
 */
class NotifiableEventResolver @Inject constructor(private val stringProvider: StringProvider,
                                                  private val noticeEventFormatter: NoticeEventFormatter) {

    //private val eventDisplay = RiotEventDisplay(context)

    fun resolveEvent(event: Event/*, roomState: RoomState?, bingRule: PushRule?*/, session: Session): NotifiableEvent? {
        val roomID = event.roomId ?: return null
        val eventId = event.eventId ?: return null
        val timelineEvent = session.getRoom(roomID)?.getTimeLineEvent(eventId) ?: return null
        when (event.getClearType()) {
            EventType.MESSAGE           -> {
                return resolveMessageEvent(timelineEvent, session)
            }
            EventType.ENCRYPTED         -> {
                val messageEvent = resolveMessageEvent(timelineEvent, session)
                messageEvent?.lockScreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
                return messageEvent
            }
            EventType.STATE_ROOM_MEMBER -> {
                return resolveStateRoomEvent(event, session)
            }
            else                        -> {

                //If the event can be displayed, display it as is
                Timber.w("NotifiableEventResolver Received an unsupported event matching a bing rule")
                //TODO Better event text display
                val bodyPreview = event.type

                return SimpleNotifiableEvent(
                        session.myUserId,
                        eventId = event.eventId!!,
                        editedEventId = timelineEvent.getEditedEventId(),
                        noisy = false,//will be updated
                        timestamp = event.originServerTs ?: System.currentTimeMillis(),
                        description = bodyPreview,
                        title = stringProvider.getString(R.string.notification_unknown_new_event),
                        soundName = null,
                        type = event.type)
            }
        }
    }

    private fun resolveMessageEvent(event: TimelineEvent, session: Session): NotifiableEvent? {

        //The event only contains an eventId, and roomId (type is m.room.*) , we need to get the displayable content (names, avatar, text, etc...)
        val room = session.getRoom(event.root.roomId!! /*roomID cannot be null*/)


        if (room == null) {
            Timber.e("## Unable to resolve room for eventId [${event}]")
            // Ok room is not known in store, but we can still display something
            val body =
                    event.getLastMessageBody()
                            ?: stringProvider.getString(R.string.notification_unknown_new_event)
            val roomName = stringProvider.getString(R.string.notification_unknown_room_name)
            val senderDisplayName = event.senderName ?: event.root.senderId

            val notifiableEvent = NotifiableMessageEvent(
                    eventId = event.root.eventId!!,
                    editedEventId = event.getEditedEventId(),
                    timestamp = event.root.originServerTs ?: 0,
                    noisy = false,//will be updated
                    senderName = senderDisplayName,
                    senderId = event.root.senderId,
                    body = body,
                    roomId = event.root.roomId!!,
                    roomName = roomName)

            notifiableEvent.matrixID = session.myUserId
            return notifiableEvent
        } else {
            if (event.root.isEncrypted() && event.root.mxDecryptionResult == null) {
                //TODO use a global event decryptor? attache to session and that listen to new sessionId?
                //for now decrypt sync
                try {
                    val result = session.decryptEvent(event.root, event.root.roomId + UUID.randomUUID().toString())
                    event.root.mxDecryptionResult = OlmDecryptionResult(
                            payload = result.clearEvent,
                            senderKey = result.senderCurve25519Key,
                            keysClaimed = result.claimedEd25519Key?.let { mapOf("ed25519" to it) },
                            forwardingCurve25519KeyChain = result.forwardingCurve25519KeyChain
                    )
                } catch (e: MXCryptoError) {

                }
            }

            val body = event.getLastMessageBody()
                    ?: stringProvider.getString(R.string.notification_unknown_new_event)
            val roomName = room.roomSummary()?.displayName ?: ""
            val senderDisplayName = event.senderName ?: event.root.senderId

            val notifiableEvent = NotifiableMessageEvent(
                    eventId = event.root.eventId!!,
                    editedEventId = event.getEditedEventId(),
                    timestamp = event.root.originServerTs ?: 0,
                    noisy = false,//will be updated
                    senderName = senderDisplayName,
                    senderId = event.root.senderId,
                    body = body,
                    roomId = event.root.roomId!!,
                    roomName = roomName,
                    roomIsDirect = room.roomSummary()?.isDirect ?: false)

            notifiableEvent.matrixID = session.myUserId
            notifiableEvent.soundName = null

            // Get the avatars URL
            notifiableEvent.roomAvatarPath = session.contentUrlResolver()
                    .resolveThumbnail(room.roomSummary()?.avatarUrl,
                            250,
                            250,
                            ContentUrlResolver.ThumbnailMethod.SCALE)

            notifiableEvent.senderAvatarPath = session.contentUrlResolver()
                    .resolveThumbnail(event.senderAvatar,
                            250,
                            250,
                            ContentUrlResolver.ThumbnailMethod.SCALE)

            return notifiableEvent
        }
    }


    private fun resolveStateRoomEvent(event: Event, session: Session): NotifiableEvent? {
        val content = event.content?.toModel<RoomMember>() ?: return null
        val roomId = event.roomId ?: return null
        val dName = event.senderId?.let { session.getUser(it)?.displayName }
        if (Membership.INVITE == content.membership) {
            val body = noticeEventFormatter.format(event, dName)
                    ?: stringProvider.getString(R.string.notification_new_invitation)
            return InviteNotifiableEvent(
                    session.myUserId,
                    eventId = event.eventId!!,
                    editedEventId = null,
                    roomId = roomId,
                    timestamp = event.originServerTs ?: 0,
                    noisy = false,//will be set later
                    title = stringProvider.getString(R.string.notification_new_invitation),
                    description = body.toString(),
                    soundName = null, //will be set later
                    type = event.getClearType(),
                    isPushGatewayEvent = false)
        } else {
            Timber.e("## unsupported notifiable event for eventId [${event.eventId}]")
            if (BuildConfig.LOW_PRIVACY_LOG_ENABLE) {
                Timber.e("## unsupported notifiable event for event [${event}]")
            }
            //TODO generic handling?
        }
        return null
    }
}

