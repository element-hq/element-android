/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.image

import im.vector.app.features.media.ImageContentRenderer
import org.matrix.android.sdk.api.session.crypto.attachments.toElementToDecrypt
import org.matrix.android.sdk.api.session.events.model.isImageMessage
import org.matrix.android.sdk.api.session.events.model.isVideoMessage
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageImageContent
import org.matrix.android.sdk.api.session.room.model.message.MessageVideoContent
import org.matrix.android.sdk.api.session.room.model.message.getFileUrl
import org.matrix.android.sdk.api.session.room.model.message.getThumbnailUrl
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent

fun TimelineEvent.buildImageContentRendererData(maxHeight: Int): ImageContentRenderer.Data? {
    return when {
        root.isImageMessage() -> root.getClearContent().toModel<MessageImageContent>()
                ?.let { messageImageContent ->
                    ImageContentRenderer.Data(
                            eventId = eventId,
                            filename = messageImageContent.body,
                            mimeType = messageImageContent.mimeType,
                            url = messageImageContent.getFileUrl(),
                            elementToDecrypt = messageImageContent.encryptedFileInfo?.toElementToDecrypt(),
                            height = messageImageContent.info?.height,
                            maxHeight = maxHeight,
                            width = messageImageContent.info?.width,
                            maxWidth = maxHeight * 2,
                            allowNonMxcUrls = false
                    )
                }
        root.isVideoMessage() -> root.getClearContent().toModel<MessageVideoContent>()
                ?.let { messageVideoContent ->
                    val videoInfo = messageVideoContent.videoInfo
                    ImageContentRenderer.Data(
                            eventId = eventId,
                            filename = messageVideoContent.body,
                            mimeType = videoInfo?.thumbnailInfo?.mimeType,
                            url = videoInfo?.getThumbnailUrl(),
                            elementToDecrypt = videoInfo?.thumbnailFile?.toElementToDecrypt(),
                            height = videoInfo?.thumbnailInfo?.height,
                            maxHeight = maxHeight,
                            width = videoInfo?.thumbnailInfo?.width,
                            maxWidth = maxHeight * 2,
                            allowNonMxcUrls = false
                    )
                }
        else -> null
    }
}
