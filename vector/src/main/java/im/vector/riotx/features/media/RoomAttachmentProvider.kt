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

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import com.bumptech.glide.request.target.CustomViewTarget
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.message.MessageContent
import im.vector.matrix.android.api.session.room.model.message.MessageWithAttachmentContent
import im.vector.matrix.android.api.session.room.model.message.getFileUrl
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.matrix.android.internal.crypto.attachments.toElementToDecrypt
import im.vector.riotx.attachmentviewer.AnimatedImageViewHolder
import im.vector.riotx.attachmentviewer.AttachmentInfo
import im.vector.riotx.attachmentviewer.AttachmentSourceProvider
import im.vector.riotx.attachmentviewer.ZoomableImageViewHolder
import im.vector.riotx.core.date.VectorDateFormatter
import im.vector.riotx.core.extensions.localDateTime
import javax.inject.Inject

class RoomAttachmentProvider(
        private val attachments: List<TimelineEvent>,
        private val initialIndex: Int,
        private val imageContentRenderer: ImageContentRenderer,
        private val dateFormatter: VectorDateFormatter
) : AttachmentSourceProvider {

    interface InteractionListener {
        fun onDismissTapped()
        fun onShareTapped()
    }

    var interactionListener: InteractionListener? = null

    private var overlayView: AttachmentOverlayView? = null

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
            if (content?.mimeType == "image/gif") {
                AttachmentInfo.AnimatedImage(
                        content.url ?: "",
                        data
                )
            } else {
                AttachmentInfo.Image(
                        content?.url ?: "",
                        data
                )
            }
        }
    }

    override fun loadImage(holder: ZoomableImageViewHolder, info: AttachmentInfo.Image) {
        (info.data as? ImageContentRenderer.Data)?.let {
            imageContentRenderer.render(it, holder.touchImageView, holder.customTargetView as CustomViewTarget<*, Drawable>)
        }
    }

    override fun loadImage(holder: AnimatedImageViewHolder, info: AttachmentInfo.AnimatedImage) {
        (info.data as? ImageContentRenderer.Data)?.let {
            imageContentRenderer.render(it, holder.touchImageView, holder.customTargetView as CustomViewTarget<*, Drawable>)
        }
    }

    override fun overlayViewAtPosition(context: Context, position: Int): View? {
        if (overlayView == null) {
            overlayView = AttachmentOverlayView(context)
            overlayView?.onBack = {
                interactionListener?.onDismissTapped()
            }
            overlayView?.onShareCallback = {
                interactionListener?.onShareTapped()
            }
        }
        val item = attachments[position]
        val dateString = item.root.localDateTime().let {
            "${dateFormatter.formatMessageDay(it)} at ${dateFormatter.formatMessageHour(it)} "
        }
        overlayView?.updateWith("${position + 1} of ${attachments.size}", "${item.senderInfo.displayName} $dateString")
        return overlayView
    }

//    override fun loadImage(holder: ImageViewHolder, info: AttachmentInfo.Image) {
//        (info.data as? ImageContentRenderer.Data)?.let {
//            imageContentRenderer.render(it, ImageContentRenderer.Mode.FULL_SIZE, holder.touchImageView)
//        }
//    }
}

class RoomAttachmentProviderFactory @Inject constructor(
        private val imageContentRenderer: ImageContentRenderer,
        private val vectorDateFormatter: VectorDateFormatter
) {

    fun createProvider(attachments: List<TimelineEvent>, initialIndex: Int): RoomAttachmentProvider {
        return RoomAttachmentProvider(attachments, initialIndex, imageContentRenderer, vectorDateFormatter)
    }
}
