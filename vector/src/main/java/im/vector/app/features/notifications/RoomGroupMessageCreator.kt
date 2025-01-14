/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.notifications

import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import im.vector.app.core.resources.StringProvider
import im.vector.lib.strings.CommonPlurals
import im.vector.lib.strings.CommonStrings
import me.gujun.android.span.Span
import me.gujun.android.span.span
import timber.log.Timber
import javax.inject.Inject

class RoomGroupMessageCreator @Inject constructor(
        private val bitmapLoader: NotificationBitmapLoader,
        private val stringProvider: StringProvider,
        private val notificationUtils: NotificationUtils
) {

    fun createRoomMessage(events: List<NotifiableMessageEvent>, roomId: String, userDisplayName: String, userAvatarUrl: String?): RoomNotification.Message {
        val lastKnownRoomEvent = events.last()
        val roomName = lastKnownRoomEvent.roomName ?: lastKnownRoomEvent.senderName ?: ""
        val roomIsGroup = !lastKnownRoomEvent.roomIsDirect
        val style = NotificationCompat.MessagingStyle(
                Person.Builder()
                        .setName(userDisplayName)
                        .setIcon(bitmapLoader.getUserIcon(userAvatarUrl))
                        .setKey(lastKnownRoomEvent.matrixID)
                        .build()
        ).also {
            it.conversationTitle = roomName.takeIf { roomIsGroup }
            it.isGroupConversation = roomIsGroup
            it.addMessagesFromEvents(events)
        }

        val tickerText = if (roomIsGroup) {
            stringProvider.getString(CommonStrings.notification_ticker_text_group, roomName, events.last().senderName, events.last().description)
        } else {
            stringProvider.getString(CommonStrings.notification_ticker_text_dm, events.last().senderName, events.last().description)
        }

        val largeBitmap = getRoomBitmap(events)

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
                            it.isUpdated = events.last().isUpdated
                        },
                        threadId = lastKnownRoomEvent.threadId,
                        largeIcon = largeBitmap,
                        lastMessageTimestamp,
                        userDisplayName,
                        tickerText
                ),
                meta
        )
    }

    private fun NotificationCompat.MessagingStyle.addMessagesFromEvents(events: List<NotifiableMessageEvent>) {
        events.forEach { event ->
            val senderPerson = if (event.outGoingMessage) {
                null
            } else {
                Person.Builder()
                        .setName(event.senderName)
                        .setIcon(bitmapLoader.getUserIcon(event.senderAvatarPath))
                        .setKey(event.senderId)
                        .build()
            }
            when {
                event.isSmartReplyError() -> addMessage(stringProvider.getString(CommonStrings.notification_inline_reply_failed), event.timestamp, senderPerson)
                else -> {
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
                1 -> createFirstMessageSummaryLine(events.first(), roomName, roomIsDirect)
                else -> {
                    stringProvider.getQuantityString(
                            CommonPlurals.notification_compat_summary_line_for_room,
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
