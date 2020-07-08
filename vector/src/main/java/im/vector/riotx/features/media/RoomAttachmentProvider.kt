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
import android.widget.ImageView
import androidx.core.view.isVisible
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import im.vector.matrix.android.api.MatrixCallback
import im.vector.matrix.android.api.session.Session
import im.vector.matrix.android.api.session.events.model.isVideoMessage
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.file.FileService
import im.vector.matrix.android.api.session.room.model.message.MessageContent
import im.vector.matrix.android.api.session.room.model.message.MessageImageContent
import im.vector.matrix.android.api.session.room.model.message.MessageVideoContent
import im.vector.matrix.android.api.session.room.model.message.MessageWithAttachmentContent
import im.vector.matrix.android.api.session.room.model.message.getFileUrl
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.matrix.android.internal.crypto.attachments.toElementToDecrypt
import im.vector.riotx.attachmentviewer.AnimatedImageViewHolder
import im.vector.riotx.attachmentviewer.AttachmentInfo
import im.vector.riotx.attachmentviewer.AttachmentSourceProvider
import im.vector.riotx.attachmentviewer.VideoViewHolder
import im.vector.riotx.attachmentviewer.ZoomableImageViewHolder
import im.vector.riotx.core.date.VectorDateFormatter
import im.vector.riotx.core.extensions.localDateTime
import java.io.File
import javax.inject.Inject

class RoomAttachmentProvider(
        private val attachments: List<TimelineEvent>,
        private val initialIndex: Int,
        private val imageContentRenderer: ImageContentRenderer,
        private val videoContentRenderer: VideoContentRenderer,
        private val dateFormatter: VectorDateFormatter,
        private val fileService: FileService
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
                        height = null
                )
                if (content.mimeType == "image/gif") {
                    AttachmentInfo.AnimatedImage(
                            content.url ?: "",
                            data
                    )
                } else {
                    AttachmentInfo.Image(
                            content.url ?: "",
                            data
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
                        maxWidth = -1
                )
                val data = VideoContentRenderer.Data(
                        eventId = it.eventId,
                        filename = content.body,
                        mimeType = content.mimeType,
                        url = content.getFileUrl(),
                        elementToDecrypt = content.encryptedFileInfo?.toElementToDecrypt(),
                        thumbnailMediaData = thumbnailData
                )
                AttachmentInfo.Video(
                        content.getFileUrl() ?: "",
                        data,
                        AttachmentInfo.Image(
                                url = content.videoInfo?.thumbnailFile?.url
                                        ?: content.videoInfo?.thumbnailUrl ?: "",
                                data = thumbnailData

                        )
                )
            } else {
                AttachmentInfo.Image(
                        "",
                        null
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

    override fun loadVideo(holder: VideoViewHolder, info: AttachmentInfo.Video) {
        val data = info.data as? VideoContentRenderer.Data ?: return
//        videoContentRenderer.render(data,
//                holder.thumbnailImage,
//                holder.loaderProgressBar,
//                holder.videoView,
//                holder.errorTextView)
        imageContentRenderer.render(data.thumbnailMediaData, holder.thumbnailImage, object : CustomViewTarget<ImageView, Drawable>(holder.thumbnailImage) {
            override fun onLoadFailed(errorDrawable: Drawable?) {
                holder.thumbnailImage.setImageDrawable(errorDrawable)
            }

            override fun onResourceCleared(placeholder: Drawable?) {
            }

            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                holder.thumbnailImage.setImageDrawable(resource)
            }
        })

        holder.thumbnailImage.isVisible = false
        holder.loaderProgressBar.isVisible = false
        holder.videoView.isVisible = false

        fileService.downloadFile(
                downloadMode = FileService.DownloadMode.FOR_INTERNAL_USE,
                id = data.eventId,
                mimeType = data.mimeType,
                elementToDecrypt = data.elementToDecrypt,
                fileName = data.filename,
                url = data.url,
                callback = object : MatrixCallback<File> {
                    override fun onSuccess(data: File) {
                        holder.videoReady(data)
                    }

                    override fun onFailure(failure: Throwable) {
                        holder.videoView.isVisible = false
                    }
                }
        )
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
        overlayView?.videoControlsGroup?.isVisible = item.root.isVideoMessage()
        return overlayView
    }
}

class RoomAttachmentProviderFactory @Inject constructor(
        private val imageContentRenderer: ImageContentRenderer,
        private val vectorDateFormatter: VectorDateFormatter,
        private val videoContentRenderer: VideoContentRenderer,
        private val session: Session
) {

    fun createProvider(attachments: List<TimelineEvent>, initialIndex: Int): RoomAttachmentProvider {
        return RoomAttachmentProvider(attachments, initialIndex, imageContentRenderer, videoContentRenderer, vectorDateFormatter, session.fileService())
    }
}
