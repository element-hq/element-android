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

package im.vector.matrix.android.internal.session.room.send

import android.media.MediaMetadataRetriever
import im.vector.matrix.android.api.auth.data.Credentials
import im.vector.matrix.android.api.session.content.ContentAttachmentData
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.EventType
import im.vector.matrix.android.api.session.events.model.toContent
import im.vector.matrix.android.api.session.room.model.message.AudioInfo
import im.vector.matrix.android.api.session.room.model.message.FileInfo
import im.vector.matrix.android.api.session.room.model.message.ImageInfo
import im.vector.matrix.android.api.session.room.model.message.MessageAudioContent
import im.vector.matrix.android.api.session.room.model.message.MessageFileContent
import im.vector.matrix.android.api.session.room.model.message.MessageImageContent
import im.vector.matrix.android.api.session.room.model.message.MessageTextContent
import im.vector.matrix.android.api.session.room.model.message.MessageType
import im.vector.matrix.android.api.session.room.model.message.MessageVideoContent
import im.vector.matrix.android.api.session.room.model.message.ThumbnailInfo
import im.vector.matrix.android.api.session.room.model.message.VideoInfo
import im.vector.matrix.android.internal.session.content.ThumbnailExtractor

internal class LocalEchoEventFactory(private val credentials: Credentials) {

    fun createTextEvent(roomId: String, msgType: String, text: String): Event {
        val content = MessageTextContent(type = msgType, body = text)
        return createEvent(roomId, content)
    }

    fun createMediaEvent(roomId: String, attachment: ContentAttachmentData): Event {
        return when (attachment.type) {
            ContentAttachmentData.Type.IMAGE -> createImageEvent(roomId, attachment)
            ContentAttachmentData.Type.VIDEO -> createVideoEvent(roomId, attachment)
            ContentAttachmentData.Type.AUDIO -> createAudioEvent(roomId, attachment)
            ContentAttachmentData.Type.FILE  -> createFileEvent(roomId, attachment)
        }
    }

    private fun createImageEvent(roomId: String, attachment: ContentAttachmentData): Event {
        val content = MessageImageContent(
                type = MessageType.MSGTYPE_IMAGE,
                body = attachment.name ?: "image",
                info = ImageInfo(
                        mimeType = attachment.mimeType,
                        width = attachment.width?.toInt() ?: 0,
                        height = attachment.height?.toInt() ?: 0,
                        size = attachment.size.toInt()
                ),
                url = attachment.path
        )
        return createEvent(roomId, content)
    }

    private fun createVideoEvent(roomId: String, attachment: ContentAttachmentData): Event {
        val mediaDataRetriever = MediaMetadataRetriever()
        mediaDataRetriever.setDataSource(attachment.path)

        // Use frame to calculate height and width as we are sure to get the right ones
        val firstFrame = mediaDataRetriever.frameAtTime
        val height = firstFrame.height
        val width = firstFrame.width
        mediaDataRetriever.release()

        val thumbnailInfo = ThumbnailExtractor.extractThumbnail(attachment)?.let {
            ThumbnailInfo(
                    width = it.width,
                    height = it.height,
                    size = it.size,
                    mimeType = it.mimeType
            )
        }
        val content = MessageVideoContent(
                type = MessageType.MSGTYPE_VIDEO,
                body = attachment.name ?: "video",
                info = VideoInfo(
                        mimeType = attachment.mimeType,
                        width = width,
                        height = height,
                        size = attachment.size,
                        duration = attachment.duration?.toInt() ?: 0,
                        // Glide will be able to use the local path and extract a thumbnail.
                        thumbnailUrl = attachment.path,
                        thumbnailInfo = thumbnailInfo
                ),
                url = attachment.path
        )
        return createEvent(roomId, content)
    }

    private fun createAudioEvent(roomId: String, attachment: ContentAttachmentData): Event {
        val content = MessageAudioContent(
                type = MessageType.MSGTYPE_AUDIO,
                body = attachment.name ?: "audio",
                info = AudioInfo(
                        mimeType = attachment.mimeType ?: "audio/mpeg",
                        size = attachment.size
                ),
                url = attachment.path
        )
        return createEvent(roomId, content)
    }

    private fun createFileEvent(roomId: String, attachment: ContentAttachmentData): Event {
        val content = MessageFileContent(
                type = MessageType.MSGTYPE_FILE,
                body = attachment.name ?: "file",
                info = FileInfo(
                        mimeType = attachment.mimeType ?: "application/octet-stream",
                        size = attachment.size
                ),
                url = attachment.path
        )
        return createEvent(roomId, content)
    }

    private fun createEvent(roomId: String, content: Any? = null): Event {
        return Event(
                roomId = roomId,
                originServerTs = dummyOriginServerTs(),
                sender = credentials.userId,
                eventId = dummyEventId(roomId),
                type = EventType.MESSAGE,
                content = content.toContent()
        )
    }

    private fun dummyOriginServerTs(): Long {
        return System.currentTimeMillis()
    }

    private fun dummyEventId(roomId: String): String {
        return roomId + "-" + dummyOriginServerTs()
    }
}
