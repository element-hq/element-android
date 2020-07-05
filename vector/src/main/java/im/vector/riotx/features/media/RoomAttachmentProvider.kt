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

package im.vector.riotx.features.media

import android.graphics.drawable.Drawable
import com.bumptech.glide.request.target.CustomViewTarget
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.message.MessageContent
import im.vector.matrix.android.api.session.room.model.message.MessageWithAttachmentContent
import im.vector.matrix.android.api.session.room.model.message.getFileUrl
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.matrix.android.internal.crypto.attachments.toElementToDecrypt
import im.vector.riotx.attachment_viewer.AttachmentInfo
import im.vector.riotx.attachment_viewer.AttachmentSourceProvider
import im.vector.riotx.attachment_viewer.ImageViewHolder
import javax.inject.Inject

class RoomAttachmentProvider(
        private val attachments: List<TimelineEvent>,
        private val initialIndex: Int,
        private val imageContentRenderer: ImageContentRenderer
) : AttachmentSourceProvider {

    override fun getItemCount(): Int {
        return attachments.size
    }

    override fun getAttachmentInfoAt(position: Int): AttachmentInfo {
        return attachments[position].let {
            val content = it.root.getClearContent().toModel<MessageContent>() as? MessageWithAttachmentContent
            val data = ImageContentRenderer.Data(
                    eventId = it.eventId,
                    filename = content?.body ?: "",
                    mimeType = content?.mimeType,
                    url = content?.getFileUrl(),
                    elementToDecrypt = content?.encryptedFileInfo?.toElementToDecrypt(),
                    maxHeight = -1,
                    maxWidth = -1,
                    width = null,
                    height = null
            )
            AttachmentInfo.Image(
                    content?.url ?: "",
                    data
            )
        }
    }

    override fun loadImage(holder: ImageViewHolder, info: AttachmentInfo.Image) {
        (info.data as? ImageContentRenderer.Data)?.let {
            imageContentRenderer.render(it, holder.touchImageView, holder.customTargetView as CustomViewTarget<*, Drawable>)
        }
    }
//    override fun loadImage(holder: ImageViewHolder, info: AttachmentInfo.Image) {
//        (info.data as? ImageContentRenderer.Data)?.let {
//            imageContentRenderer.render(it, ImageContentRenderer.Mode.FULL_SIZE, holder.touchImageView)
//        }
//    }
}

class RoomAttachmentProviderFactory @Inject constructor(
        private val imageContentRenderer: ImageContentRenderer
) {

    fun createProvider(attachments: List<TimelineEvent>, initialIndex: Int): RoomAttachmentProvider {
        return RoomAttachmentProvider(attachments, initialIndex, imageContentRenderer)
    }
}
