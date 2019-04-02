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

package im.vector.riotredesign.features.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.room.Room
import org.koin.standalone.KoinComponent
import org.koin.standalone.inject
import timber.log.Timber

/**
 * Receives actions broadcast by notification (on click, on dismiss, inline replies, etc.)
 */
class NotificationBroadcastReceiver : BroadcastReceiver(), KoinComponent {

    private val notificationDrawerManager by inject<NotificationDrawerManager>()

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null || context == null) return

        Timber.d("ReplyNotificationBroadcastReceiver received : $intent")

        when (intent.action) {
            NotificationUtils.SMART_REPLY_ACTION ->
                handleSmartReply(intent, context)
            NotificationUtils.DISMISS_ROOM_NOTIF_ACTION ->
                intent.getStringExtra(KEY_ROOM_ID)?.let {
                    notificationDrawerManager.clearMessageEventOfRoom(it)
                }
            NotificationUtils.DISMISS_SUMMARY_ACTION ->
                notificationDrawerManager.clearAllEvents()
            NotificationUtils.MARK_ROOM_READ_ACTION ->
                intent.getStringExtra(KEY_ROOM_ID)?.let {
                    notificationDrawerManager.clearMessageEventOfRoom(it)
                    handleMarkAsRead(context, it)
                }
        }
    }

    private fun handleMarkAsRead(context: Context, roomId: String) {
        /*
        TODO
        Matrix.getInstance(context)?.defaultSession?.let { session ->
            session.dataHandler
                    ?.getRoom(roomId)
                    ?.markAllAsRead(object : SimpleApiCallback<Void>() {
                        override fun onSuccess(void: Void?) {
                            // Ignore
                        }
                    })
        }
         */
    }

    private fun handleSmartReply(intent: Intent, context: Context) {
        /*
        TODO
        val message = getReplyMessage(intent)
        val roomId = intent.getStringExtra(KEY_ROOM_ID)

        if (TextUtils.isEmpty(message) || TextUtils.isEmpty(roomId)) {
            //ignore this event
            //Can this happen? should we update notification?
            return
        }
        val matrixId = intent.getStringExtra(EXTRA_MATRIX_ID)
        Matrix.getInstance(context)?.getSession(matrixId)?.let { session ->
            session.dataHandler?.getRoom(roomId)?.let { room ->
                sendMatrixEvent(message!!, session, roomId!!, room, context)
            }
        }
        */
    }

    private fun sendMatrixEvent(message: String, session: Session, roomId: String, room: Room, context: Context?) {
        /*
        TODO

        val mxMessage = Message()
        mxMessage.msgtype = Message.MSGTYPE_TEXT
        mxMessage.body = message

        val event = Event(mxMessage, session.credentials.userId, roomId)
        room.storeOutgoingEvent(event)
        room.sendEvent(event, object : ApiCallback<Void?> {
            override fun onSuccess(info: Void?) {
                Timber.d("Send message : onSuccess ")
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
                VectorApp.getInstance().notificationDrawerManager.onNotifiableEventReceived(notifiableMessageEvent)
                VectorApp.getInstance().notificationDrawerManager.refreshNotificationDrawer(null)
            }

            override fun onNetworkError(e: Exception) {
                Timber.d("Send message : onNetworkError " + e.message, e)
                onSmartReplyFailed(e.localizedMessage)
            }

            override fun onMatrixError(e: MatrixError) {
                Timber.d("Send message : onMatrixError " + e.message)
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
            val remoteInput = RemoteInput.getResultsFromIntent(intent);
            if (remoteInput != null) {
                return remoteInput.getCharSequence(KEY_TEXT_REPLY)?.toString()
            }
        }
        return null
    }

    companion object {
        const val KEY_ROOM_ID = "roomID"
        const val KEY_TEXT_REPLY = "key_text_reply"
        const val EXTRA_MATRIX_ID = "EXTRA_MATRIX_ID"
    }
}