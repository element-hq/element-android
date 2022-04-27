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
        else                  -> null
    }
}
