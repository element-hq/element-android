/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.media

import im.vector.app.core.date.VectorDateFormatter
import im.vector.app.core.resources.StringProvider
import im.vector.lib.attachmentviewer.AttachmentInfo
import kotlinx.coroutines.CoroutineScope
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.file.FileService
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.getTimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.util.MimeTypes
import java.io.File

class DataAttachmentRoomProvider(
        attachments: List<AttachmentData>,
        private val room: Room?,
        imageContentRenderer: ImageContentRenderer,
        dateFormatter: VectorDateFormatter,
        fileService: FileService,
        coroutineScope: CoroutineScope,
        stringProvider: StringProvider
) : BaseAttachmentProvider<AttachmentData>(
        attachments = attachments,
        imageContentRenderer = imageContentRenderer,
        fileService = fileService,
        coroutineScope = coroutineScope,
        dateFormatter = dateFormatter,
        stringProvider = stringProvider
) {

    override fun getAttachmentInfoAt(position: Int): AttachmentInfo {
        return getItem(position).let {
            when (it) {
                is ImageContentRenderer.Data -> {
                    if (it.mimeType == MimeTypes.Gif || it.mimeType == MimeTypes.Webp) {
                        AttachmentInfo.AnimatedImage(
                                uid = it.eventId,
                                url = it.url ?: "",
                                data = it
                        )
                    } else {
                        AttachmentInfo.Image(
                                uid = it.eventId,
                                url = it.url ?: "",
                                data = it
                        )
                    }
                }
                is VideoContentRenderer.Data -> {
                    AttachmentInfo.Video(
                            uid = it.eventId,
                            url = it.url ?: "",
                            data = it,
                            thumbnail = AttachmentInfo.Image(
                                    uid = it.eventId,
                                    url = it.thumbnailMediaData.url ?: "",
                                    data = it.thumbnailMediaData
                            )
                    )
                }
                else -> throw IllegalArgumentException()
            }
        }
    }

    override fun getTimelineEventAtPosition(position: Int): TimelineEvent? {
        val item = getItem(position)
        return room?.getTimelineEvent(item.eventId)
    }

    override suspend fun getFileForSharing(position: Int): File? {
        return getItem(position)
                .let { item ->
                    tryOrNull {
                        fileService.downloadFile(
                                fileName = item.filename,
                                mimeType = item.mimeType,
                                url = item.url,
                                elementToDecrypt = item.elementToDecrypt
                        )
                    }
                }
    }
}
