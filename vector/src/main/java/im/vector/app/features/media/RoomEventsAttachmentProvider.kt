/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.media

import android.content.Context
import android.view.View
import androidx.core.view.isVisible
import im.vector.app.core.date.VectorDateFormatter
import im.vector.app.core.extensions.localDateTime
import im.vector.lib.attachmentviewer.AttachmentInfo
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.isVideoMessage
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.file.FileService
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.model.message.MessageImageContent
import org.matrix.android.sdk.api.session.room.model.message.MessageVideoContent
import org.matrix.android.sdk.api.session.room.model.message.MessageWithAttachmentContent
import org.matrix.android.sdk.api.session.room.model.message.getFileUrl
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.internal.crypto.attachments.toElementToDecrypt
import java.io.File
import javax.inject.Inject

class RoomEventsAttachmentProvider(
        private val attachments: List<TimelineEvent>,
        private val initialIndex: Int,
        imageContentRenderer: ImageContentRenderer,
        private val dateFormatter: VectorDateFormatter,
        fileService: FileService
) : BaseAttachmentProvider(imageContentRenderer, fileService) {

    override fun getItemCount(): Int {
        return attachments.size
    }

    override fun getAttachmentInfoAt(position: Int): AttachmentInfo {
        return attachments[position].let {
            val content = it.root.getClearContent().toModel<MessageContent>() as? MessageWithAttachmentContent
            if (content is MessageImageContent) {
                val data = ImageContentRenderer.Data(
                        eventId = it.eventId,
                        filename = content.body,
                        mimeType = content.mimeType,
                        url = content.getFileUrl(),
                        elementToDecrypt = content.encryptedFileInfo?.toElementToDecrypt(),
                        maxHeight = -1,
                        maxWidth = -1,
                        width = null,
                        height = null,
                        allowNonMxcUrls = it.root.sendState.isSending()

                )
                if (content.mimeType == "image/gif") {
                    AttachmentInfo.AnimatedImage(
                            uid = it.eventId,
                            url = content.url ?: "",
                            data = data
                    )
                } else {
                    AttachmentInfo.Image(
                            uid = it.eventId,
                            url = content.url ?: "",
                            data = data
                    )
                }
            } else if (content is MessageVideoContent) {
                val thumbnailData = ImageContentRenderer.Data(
                        eventId = it.eventId,
                        filename = content.body,
                        mimeType = content.mimeType,
                        url = content.videoInfo?.thumbnailFile?.url
                                ?: content.videoInfo?.thumbnailUrl,
                        elementToDecrypt = content.videoInfo?.thumbnailFile?.toElementToDecrypt(),
                        height = content.videoInfo?.height,
                        maxHeight = -1,
                        width = content.videoInfo?.width,
                        maxWidth = -1,
                        allowNonMxcUrls = it.root.sendState.isSending()
                )
                val data = VideoContentRenderer.Data(
                        eventId = it.eventId,
                        filename = content.body,
                        mimeType = content.mimeType,
                        url = content.getFileUrl(),
                        elementToDecrypt = content.encryptedFileInfo?.toElementToDecrypt(),
                        thumbnailMediaData = thumbnailData,
                        allowNonMxcUrls = it.root.sendState.isSending()
                )
                AttachmentInfo.Video(
                        uid = it.eventId,
                        url = content.getFileUrl() ?: "",
                        data = data,
                        thumbnail = AttachmentInfo.Image(
                                uid = it.eventId,
                                url = content.videoInfo?.thumbnailFile?.url
                                        ?: content.videoInfo?.thumbnailUrl ?: "",
                                data = thumbnailData

                        )
                )
            } else {
                AttachmentInfo.Image(
                        uid = it.eventId,
                        url = "",
                        data = null
                )
            }
        }
    }

    override fun overlayViewAtPosition(context: Context, position: Int): View? {
        super.overlayViewAtPosition(context, position)
        val item = attachments[position]
        val dateString = item.root.localDateTime().let {
            "${dateFormatter.formatMessageDay(it)} at ${dateFormatter.formatMessageHour(it)} "
        }
        overlayView?.updateWith("${position + 1} of ${attachments.size}", "${item.senderInfo.displayName} $dateString")
        overlayView?.videoControlsGroup?.isVisible = item.root.isVideoMessage()
        return overlayView
    }

    override fun getFileForSharing(position: Int, callback: (File?) -> Unit) {
        attachments[position].let { timelineEvent ->

            val messageContent = timelineEvent.root.getClearContent().toModel<MessageContent>()
                    as? MessageWithAttachmentContent
                    ?: return@let
            fileService.downloadFile(
                    downloadMode = FileService.DownloadMode.FOR_EXTERNAL_SHARE,
                    id = timelineEvent.eventId,
                    fileName = messageContent.body,
                    mimeType = messageContent.mimeType,
                    url = messageContent.getFileUrl(),
                    elementToDecrypt = messageContent.encryptedFileInfo?.toElementToDecrypt(),
                    callback = object : MatrixCallback<File> {
                        override fun onSuccess(data: File) {
                            callback(data)
                        }

                        override fun onFailure(failure: Throwable) {
                            callback(null)
                        }
                    }
            )
        }
    }
}

class AttachmentProviderFactory @Inject constructor(
        private val imageContentRenderer: ImageContentRenderer,
        private val vectorDateFormatter: VectorDateFormatter,
        private val session: Session
) {

    fun createProvider(attachments: List<TimelineEvent>, initialIndex: Int): RoomEventsAttachmentProvider {
        return RoomEventsAttachmentProvider(attachments, initialIndex, imageContentRenderer, vectorDateFormatter, session.fileService())
    }

    fun createProvider(attachments: List<AttachmentData>, room: Room?, initialIndex: Int): DataAttachmentRoomProvider {
        return DataAttachmentRoomProvider(attachments, room, initialIndex, imageContentRenderer, vectorDateFormatter, session.fileService())
    }
}
