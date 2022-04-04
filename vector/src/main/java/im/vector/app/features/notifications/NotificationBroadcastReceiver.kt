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

package im.vector.app.features.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.features.analytics.AnalyticsTracker
import im.vector.app.features.analytics.extensions.toAnalyticsJoinedRoom
import im.vector.app.features.session.coroutineScope
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.read.ReadService
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

/**
 * Receives actions broadcast by notification (on click, on dismiss, inline replies, etc.)
 */
@AndroidEntryPoint
class NotificationBroadcastReceiver : BroadcastReceiver() {

    @Inject lateinit var notificationDrawerManager: NotificationDrawerManager
    @Inject lateinit var activeSessionHolder: ActiveSessionHolder
    @Inject lateinit var analyticsTracker: AnalyticsTracker

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null || context == null) return
        Timber.v("NotificationBroadcastReceiver received : $intent")
        when (intent.action) {
            NotificationUtils.SMART_REPLY_ACTION        ->
                handleSmartReply(intent, context)
            NotificationUtils.DISMISS_ROOM_NOTIF_ACTION ->
                intent.getStringExtra(KEY_ROOM_ID)?.let { roomId ->
                    notificationDrawerManager.updateEvents { it.clearMessagesForRoom(roomId) }
                }
            NotificationUtils.DISMISS_SUMMARY_ACTION    ->
                notificationDrawerManager.clearAllEvents()
            NotificationUtils.MARK_ROOM_READ_ACTION     ->
                intent.getStringExtra(KEY_ROOM_ID)?.let { roomId ->
                    notificationDrawerManager.updateEvents { it.clearMessagesForRoom(roomId) }
                    handleMarkAsRead(roomId)
                }
            NotificationUtils.JOIN_ACTION               -> {
                intent.getStringExtra(KEY_ROOM_ID)?.let { roomId ->
                    notificationDrawerManager.updateEvents { it.clearMemberShipNotificationForRoom(roomId) }
                    handleJoinRoom(roomId)
                }
            }
            NotificationUtils.REJECT_ACTION             -> {
                intent.getStringExtra(KEY_ROOM_ID)?.let { roomId ->
                    notificationDrawerManager.updateEvents { it.clearMemberShipNotificationForRoom(roomId) }
                    handleRejectRoom(roomId)
                }
            }
        }
    }

    private fun handleJoinRoom(roomId: String) {
        activeSessionHolder.getSafeActiveSession()?.let { session ->
            val room = session.getRoom(roomId)
            if (room != null) {
                session.coroutineScope.launch {
                    tryOrNull {
                        session.joinRoom(room.roomId)
                        analyticsTracker.capture(room.roomSummary().toAnalyticsJoinedRoom())
                    }
                }
            }
        }
    }

    private fun handleRejectRoom(roomId: String) {
        activeSessionHolder.getSafeActiveSession()?.let { session ->
            session.coroutineScope.launch {
                tryOrNull { session.leaveRoom(roomId) }
            }
        }
    }

    private fun handleMarkAsRead(roomId: String) {
        activeSessionHolder.getActiveSession().let { session ->
            val room = session.getRoom(roomId)
            if (room != null) {
                session.coroutineScope.launch {
                    tryOrNull { room.markAsRead(ReadService.MarkAsReadParams.READ_RECEIPT) }
                }
            }
        }
    }

    private fun handleSmartReply(intent: Intent, context: Context) {
        val message = getReplyMessage(intent)
        val roomId = intent.getStringExtra(KEY_ROOM_ID)

        if (message.isNullOrBlank() || roomId.isNullOrBlank()) {
            // ignore this event
            // Can this happen? should we update notification?
            return
        }
        activeSessionHolder.getActiveSession().let { session ->
            session.getRoom(roomId)?.let { room ->
                sendMatrixEvent(message, session, room, context)
            }
        }
    }

    private fun sendMatrixEvent(message: String, session: Session, room: Room, context: Context?) {
        room.sendTextMessage(message)

        // Create a new event to be displayed in the notification drawer, right now

        val notifiableMessageEvent = NotifiableMessageEvent(
                // Generate a Fake event id
                eventId = UUID.randomUUID().toString(),
                editedEventId = null,
                noisy = false,
                timestamp = System.currentTimeMillis(),
                senderName = session.getRoomMember(session.myUserId, room.roomId)?.displayName
                        ?: context?.getString(R.string.notification_sender_me),
                senderId = session.myUserId,
                body = message,
                imageUri = null,
                roomId = room.roomId,
                roomName = room.roomSummary()?.displayName ?: room.roomId,
                roomIsDirect = room.roomSummary()?.isDirect == true,
                outGoingMessage = true,
                canBeReplaced = false
        )

        notificationDrawerManager.updateEvents { it.onNotifiableEventReceived(notifiableMessageEvent) }

        /*
        // TODO Error cannot be managed the same way than in Riot

        val event = Event(mxMessage, session.credentials.userId, roomId)
        room.storeOutgoingEvent(event)
        room.sendEvent(event, object : MatrixCallback<Void?> {
            override fun onSuccess(info: Void?) {
                Timber.v("Send message : onSuccess ")
            }

            override fun onNetworkError(e: Exception) {
                Timber.e(e, "Send message : onNetworkError")
                onSmartReplyFailed(e.localizedMessage)
            }

            override fun onMatrixError(e: MatrixError) {
                Timber.v("Send message : onMatrixError " + e.message)
                if (e is MXCryptoError) {
                    Toast.makeText(context, e.detailedErrorDescription, Toast.LENGTH_SHORT).show()
                    onSmartReplyFailed(e.detailedErrorDescription)
                } else {
                    Toast.makeText(context, e.localizedMessage, Toast.LENGTH_SHORT).show()
                    onSmartReplyFailed(e.localizedMessage)
                }
            }

            override fun onUnexpectedError(e: Exception) {
                Timber.e(e, "Send message : onUnexpectedError " + e.message)
                onSmartReplyFailed(e.message)
            }


            fun onSmartReplyFailed(reason: String?) {
                val notifiableMessageEvent = NotifiableMessageEvent(
                        event.eventId,
                        false,
                        System.currentTimeMillis(),
                        session.myUser?.displayname
                                ?: context?.getString(R.string.notification_sender_me),
                        session.myUserId,
                        message,
                        roomId,
                        room.getRoomDisplayName(context),
                        room.isDirect)
                notifiableMessageEvent.outGoingMessage = true
                notifiableMessageEvent.outGoingMessageFailed = true

                VectorApp.getInstance().notificationDrawerManager.onNotifiableEventReceived(notifiableMessageEvent)
                VectorApp.getInstance().notificationDrawerManager.refreshNotificationDrawer(null)
            }
        })
         */
    }

    private fun getReplyMessage(intent: Intent?): String? {
        if (intent != null) {
            val remoteInput = RemoteInput.getResultsFromIntent(intent)
            if (remoteInput != null) {
                return remoteInput.getCharSequence(KEY_TEXT_REPLY)?.toString()
            }
        }
        return null
    }

    companion object {
        const val KEY_ROOM_ID = "roomID"
        const val KEY_TEXT_REPLY = "key_text_reply"
    }
}
