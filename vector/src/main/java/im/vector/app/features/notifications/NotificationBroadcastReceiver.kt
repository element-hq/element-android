/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.features.analytics.AnalyticsTracker
import im.vector.app.features.analytics.extensions.toAnalyticsJoinedRoom
import im.vector.app.features.analytics.plan.JoinedRoom
import im.vector.app.features.session.coroutineScope
import im.vector.lib.core.utils.timer.Clock
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.read.ReadService
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

/**
 * Receives actions broadcast by notification (on click, on dismiss, inline replies, etc.).
 */
@AndroidEntryPoint
class NotificationBroadcastReceiver : BroadcastReceiver() {

    @Inject lateinit var notificationDrawerManager: NotificationDrawerManager
    @Inject lateinit var activeSessionHolder: ActiveSessionHolder
    @Inject lateinit var analyticsTracker: AnalyticsTracker
    @Inject lateinit var clock: Clock
    @Inject lateinit var actionIds: NotificationActionIds

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null || context == null) return
        Timber.v("NotificationBroadcastReceiver received : $intent")
        when (intent.action) {
            actionIds.smartReply ->
                handleSmartReply(intent, context)
            actionIds.dismissRoom ->
                intent.getStringExtra(KEY_ROOM_ID)?.let { roomId ->
                    notificationDrawerManager.updateEvents { it.clearMessagesForRoom(roomId) }
                }
            actionIds.dismissSummary ->
                notificationDrawerManager.clearAllEvents()
            actionIds.markRoomRead ->
                intent.getStringExtra(KEY_ROOM_ID)?.let { roomId ->
                    notificationDrawerManager.updateEvents { it.clearMessagesForRoom(roomId) }
                    handleMarkAsRead(roomId)
                }
            actionIds.join -> {
                intent.getStringExtra(KEY_ROOM_ID)?.let { roomId ->
                    notificationDrawerManager.updateEvents { it.clearMemberShipNotificationForRoom(roomId) }
                    handleJoinRoom(roomId)
                }
            }
            actionIds.reject -> {
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
                        session.roomService().joinRoom(room.roomId)
                        analyticsTracker.capture(room.roomSummary().toAnalyticsJoinedRoom(JoinedRoom.Trigger.Notification))
                    }
                }
            }
        }
    }

    private fun handleRejectRoom(roomId: String) {
        activeSessionHolder.getSafeActiveSession()?.let { session ->
            session.coroutineScope.launch {
                tryOrNull { session.roomService().leaveRoom(roomId) }
            }
        }
    }

    private fun handleMarkAsRead(roomId: String) {
        activeSessionHolder.getActiveSession().let { session ->
            val room = session.getRoom(roomId)
            if (room != null) {
                session.coroutineScope.launch {
                    tryOrNull { room.readService().markAsRead(ReadService.MarkAsReadParams.READ_RECEIPT, mainTimeLineOnly = false) }
                }
            }
        }
    }

    private fun handleSmartReply(intent: Intent, context: Context) {
        val message = getReplyMessage(intent)
        val roomId = intent.getStringExtra(KEY_ROOM_ID)
        val threadId = intent.getStringExtra(KEY_THREAD_ID)

        if (message.isNullOrBlank() || roomId.isNullOrBlank()) {
            // ignore this event
            // Can this happen? should we update notification?
            return
        }
        activeSessionHolder.getActiveSession().let { session ->
            session.getRoom(roomId)?.let { room ->
                sendMatrixEvent(message, threadId, session, room, context)
            }
        }
    }

    private fun sendMatrixEvent(message: String, threadId: String?, session: Session, room: Room, context: Context?) {
        if (threadId != null) {
            room.relationService().replyInThread(
                    rootThreadEventId = threadId,
                    replyInThreadText = message,
            )
        } else {
            room.sendService().sendTextMessage(message)
        }

        // Create a new event to be displayed in the notification drawer, right now

        val notifiableMessageEvent = NotifiableMessageEvent(
                // Generate a Fake event id
                eventId = UUID.randomUUID().toString(),
                editedEventId = null,
                noisy = false,
                timestamp = clock.epochMillis(),
                senderName = session.roomService().getRoomMember(session.myUserId, room.roomId)?.displayName
                        ?: context?.getString(CommonStrings.notification_sender_me),
                senderId = session.myUserId,
                body = message,
                imageUriString = null,
                roomId = room.roomId,
                threadId = threadId,
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
                        clock.epochMillis(),
                        session.myUser?.displayname
                                ?: context?.getString(CommonStrings.notification_sender_me),
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
        const val KEY_THREAD_ID = "threadID"
        const val KEY_TEXT_REPLY = "key_text_reply"
    }
}
