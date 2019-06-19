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
package im.vector.riotredesign.features.notifications

import android.content.Context
import im.vector.matrix.android.api.pushrules.rest.PushRule
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toContent
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.message.MessageContent
import im.vector.riotredesign.R
import im.vector.riotredesign.core.preference.BingRule
import timber.log.Timber

// TODO Remove
class RoomState {

}


/**
 * The notifiable event resolver is able to create a NotifiableEvent (view model for notifications) from an sdk Event.
 * It is used as a bridge between the Event Thread and the NotificationDrawerManager.
 * The NotifiableEventResolver is the only aware of session/store, the NotificationDrawerManager has no knowledge of that,
 * this pattern allow decoupling between the object responsible of displaying notifications and the matrix sdk.
 */
class NotifiableEventResolver(val context: Context) {

    //private val eventDisplay = RiotEventDisplay(context)

    fun resolveEvent(event: Event/*, roomState: RoomState?*/, bingRule: PushRule?, session: Session): NotifiableEvent? {


//        val store = session.dataHandler.store
//        if (store == null) {
//            Log.e("## NotifiableEventResolver, unable to get store")
//            //TODO notify somehow that something did fail?
//            return null
//        }

        when (event.getClearType()) {
            EventType.MESSAGE           -> {
                return resolveMessageEvent(event, bingRule, session)
            }
//            EventType.ENCRYPTED         -> {
//                val messageEvent = resolveMessageEvent(event, bingRule, session, store)
//                messageEvent?.lockScreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
//                return messageEvent
//            }
//            EventType.STATE_ROOM_MEMBER -> {
//                return resolveStateRoomEvent(event, bingRule, session, store)
//            }
            else -> {

                //If the event can be displayed, display it as is
//                eventDisplay.getTextualDisplay(event, roomState)?.toString()?.let { body ->
//                    return SimpleNotifiableEvent(
//                            session.myUserId,
//                            eventId = event.eventId,
//                            noisy = bingRule?.notificationSound != null,
//                            timestamp = event.originServerTs,
//                            description = body,
//                            soundName = bingRule?.notificationSound,
//                            title = context.getString(R.string.notification_unknown_new_event),
//                            type = event.type)
//                }

                //Unsupported event
                Timber.w("NotifiableEventResolver Received an unsupported event matching a bing rule")
                return null
            }
        }
    }


    private fun resolveMessageEvent(event: Event, pushRule: PushRule?, session: Session): NotifiableEvent? {
        //If we are here, that means that the event should be notified to the user, we check now how it should be presented (sound)
//        val soundName = pushRule?.notificationSound

        val noisy =  true//pushRule?.notificationSound != null

        //The event only contains an eventId, and roomId (type is m.room.*) , we need to get the displayable content (names, avatar, text, etc...)
        val room = session.getRoom(event.roomId!! /*roomID cannot be null (see Matrix SDK code)*/)


        if (room == null) {
            Timber.e("## Unable to resolve room for eventId [${event.eventId}] and roomID [${event.roomId}]")
            // Ok room is not known in store, but we can still display something
            val body = event.content?.toModel<MessageContent>()?.body
                    ?: context.getString(R.string.notification_unknown_new_event)
            val roomName = context.getString(R.string.notification_unknown_room_name)
            val senderDisplayName = event.senderId ?: ""

            val notifiableEvent = NotifiableMessageEvent(
                    eventId = event.eventId ?: "",
                    timestamp = event.originServerTs ?: 0,
                    noisy = noisy,
                    senderName = senderDisplayName,
                    senderId = event.senderId,
                    body = body,
                    roomId = event.roomId ?: "",
                    roomName = roomName)

            notifiableEvent.matrixID = session.sessionParams.credentials.userId
//            notifiableEvent.soundName = soundName

            return notifiableEvent
        } else {
            val tEvent = room.getTimeLineEvent(event.eventId!!)
            val body = event.content?.toModel<MessageContent>()?.body
                    ?: context.getString(R.string.notification_unknown_new_event)
            val roomName = event.roomId ?: "room"
            val senderDisplayName = tEvent?.senderName ?: "?"

            val notifiableEvent = NotifiableMessageEvent(
                    eventId = event.eventId!!,
                    timestamp = event.originServerTs ?: 0,
                    noisy = noisy,
                    senderName = senderDisplayName,
                    senderId = event.senderId,
                    body = body,
                    roomId = event.roomId ?: "00",
                    roomName = roomName,
                    roomIsDirect = true)

            notifiableEvent.matrixID = session.sessionParams.credentials.userId
            notifiableEvent.soundName = null


//            val roomAvatarPath = session.mediaCache?.thumbnailCacheFile(room.avatarUrl, 50)
//            if (roomAvatarPath != null) {
//                notifiableEvent.roomAvatarPath = roomAvatarPath.path
//            } else {
//                // prepare for the next time
//                session.mediaCache?.loadAvatarThumbnail(session.homeServerConfig, ImageView(context), room.avatarUrl, 50)
//            }
//
//            room.state.getMember(event.sender)?.avatarUrl?.let {
//                val size = context.resources.getDimensionPixelSize(R.dimen.profile_avatar_size)
//                val userAvatarUrlPath = session.mediaCache?.thumbnailCacheFile(it, size)
//                if (userAvatarUrlPath != null) {
//                    notifiableEvent.senderAvatarPath = userAvatarUrlPath.path
//                } else {
//                    // prepare for the next time
//                    session.mediaCache?.loadAvatarThumbnail(session.homeServerConfig, ImageView(context), it, size)
//                }
//            }

            return notifiableEvent
        }
    }
    /*
    private fun resolveStateRoomEvent(event: Event, bingRule: BingRule?, session: MXSession, store: IMXStore): NotifiableEvent? {
        if (RoomMember.MEMBERSHIP_INVITE == event.contentAsJsonObject?.getAsJsonPrimitive("membership")?.asString) {
            val room = store.getRoom(event.roomId /*roomID cannot be null (see Matrix SDK code)*/)
            val body = eventDisplay.getTextualDisplay(event, room.state)?.toString()
                    ?: context.getString(R.string.notification_new_invitation)
            return InviteNotifiableEvent(
                    session.myUserId,
                    eventId = event.eventId,
                    roomId = event.roomId,
                    timestamp = event.originServerTs,
                    noisy = bingRule?.notificationSound != null,
                    title = context.getString(R.string.notification_new_invitation),
                    description = body,
                    soundName = bingRule?.notificationSound,
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
    }    */
}

