/*
 * Copyright (c) 2021 New Vector Ltd
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

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.graphics.drawable.IconCompat
import im.vector.app.R
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.home.room.detail.RoomDetailActivity
import me.gujun.android.span.Span
import me.gujun.android.span.span
import timber.log.Timber
import javax.inject.Inject

class RoomGroupMessageCreator @Inject constructor(
        private val iconLoader: IconLoader,
        private val bitmapLoader: BitmapLoader,
        private val stringProvider: StringProvider,
        private val notificationUtils: NotificationUtils,
        private val appContext: Context
) {

    fun createRoomMessage(events: List<NotifiableMessageEvent>, roomId: String, userDisplayName: String, userAvatarUrl: String?): RoomNotification.Message {
        val firstKnownRoomEvent = events[0]
        val roomName = firstKnownRoomEvent.roomName ?: firstKnownRoomEvent.senderName ?: ""
        val roomIsGroup = !firstKnownRoomEvent.roomIsDirect
        val style = NotificationCompat.MessagingStyle(Person.Builder()
                .setName(userDisplayName)
                .setIcon(iconLoader.getUserIcon(userAvatarUrl))
                .setKey(firstKnownRoomEvent.matrixID)
                .build()
        ).also {
            it.conversationTitle = roomName.takeIf { roomIsGroup }
            it.isGroupConversation = roomIsGroup
            it.addMessagesFromEvents(events)
        }

        val tickerText = if (roomIsGroup) {
            stringProvider.getString(R.string.notification_ticker_text_group, roomName, events.last().senderName, events.last().description)
        } else {
            stringProvider.getString(R.string.notification_ticker_text_dm, events.last().senderName, events.last().description)
        }

        val largeBitmap = getRoomBitmap(events)
        val shortcutInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val openRoomIntent = RoomDetailActivity.shortcutIntent(appContext, roomId)
            ShortcutInfoCompat.Builder(appContext, roomId)
                    .setLongLived(true)
                    .setIntent(openRoomIntent)
                    .setShortLabel(roomName)
                    .setIcon(largeBitmap?.let { IconCompat.createWithAdaptiveBitmap(it) } ?: iconLoader.getUserIcon(events.last().senderAvatarPath))
                    .build()
        } else {
            null
        }

        val lastMessageTimestamp = events.last().timestamp
        val smartReplyErrors = events.filter { it.isSmartReplyError() }
        val messageCount = (events.size - smartReplyErrors.size)
        val meta = RoomNotification.Message.Meta(
                summaryLine = createRoomMessagesGroupSummaryLine(events, roomName, roomIsDirect = !roomIsGroup),
                messageCount = messageCount,
                latestTimestamp = lastMessageTimestamp,
                roomId = roomId,
                shouldBing = events.any { it.noisy }
        )
        return RoomNotification.Message(
                notificationUtils.buildMessagesListNotification(
                        style,
                        RoomEventGroupInfo(roomId, roomName, isDirect = !roomIsGroup).also {
                            it.hasSmartReplyError = smartReplyErrors.isNotEmpty()
                            it.shouldBing = meta.shouldBing
                            it.customSound = events.last().soundName
                        },
                        largeIcon = largeBitmap,
                        lastMessageTimestamp,
                        userDisplayName,
                        tickerText
                ),
                shortcutInfo,
                meta
        )
    }

    private fun NotificationCompat.MessagingStyle.addMessagesFromEvents(events: List<NotifiableMessageEvent>) {
        events.forEach { event ->
            Log.e("!!!", "event: $event")
            val senderPerson = if (event.outGoingMessage) {
                null
            } else {
                Person.Builder()
                        .setName(event.senderName)
                        .setIcon(iconLoader.getUserIcon(event.senderAvatarPath))
                        .setKey(event.senderId)
                        .build()
            }
            when {
                event.isSmartReplyError() -> addMessage(stringProvider.getString(R.string.notification_inline_reply_failed), event.timestamp, senderPerson)
                else                      -> {
                    val message = NotificationCompat.MessagingStyle.Message(event.body, event.timestamp, senderPerson).also { message ->
                        event.imageUri?.let {
                            message.setData("image/", it)
                        }
                    }
                    addMessage(message)
                }
            }
        }
    }

    private fun createRoomMessagesGroupSummaryLine(events: List<NotifiableMessageEvent>, roomName: String, roomIsDirect: Boolean): CharSequence {
        return try {
            when (events.size) {
                1    -> createFirstMessageSummaryLine(events.first(), roomName, roomIsDirect)
                else -> {
                    stringProvider.getQuantityString(
                            R.plurals.notification_compat_summary_line_for_room,
                            events.size,
                            roomName,
                            events.size
                    )
                }
            }
        } catch (e: Throwable) {
            // String not found or bad format
            Timber.v("%%%%%%%% REFRESH NOTIFICATION DRAWER failed to resolve string")
            roomName
        }
    }

    private fun createFirstMessageSummaryLine(event: NotifiableMessageEvent, roomName: String, roomIsDirect: Boolean): Span {
        return if (roomIsDirect) {
            span {
                span {
                    textStyle = "bold"
                    +String.format("%s: ", event.senderName)
                }
                +(event.description)
            }
        } else {
            span {
                span {
                    textStyle = "bold"
                    +String.format("%s: %s ", roomName, event.senderName)
                }
                +(event.description)
            }
        }
    }

    private fun getRoomBitmap(events: List<NotifiableMessageEvent>): Bitmap? {
        // Use the last event (most recent?)
        return events.lastOrNull()
                ?.roomAvatarPath
                ?.let { bitmapLoader.getRoomBitmap(it) }
    }
}

private fun NotifiableMessageEvent.isSmartReplyError() = outGoingMessage && outGoingMessageFailed
