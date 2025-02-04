/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.roomprofile.uploads.media

import android.view.View
import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.epoxy.VisibilityState
import im.vector.app.core.epoxy.squareLoadingItem
import im.vector.app.core.error.ErrorFormatter
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.utils.DimensionConverter
import im.vector.app.features.media.ImageContentRenderer
import im.vector.app.features.media.VideoContentRenderer
import im.vector.app.features.roomprofile.uploads.RoomUploadsViewState
import org.matrix.android.sdk.api.session.crypto.attachments.toElementToDecrypt
import org.matrix.android.sdk.api.session.room.model.message.MessageImageContent
import org.matrix.android.sdk.api.session.room.model.message.MessageType
import org.matrix.android.sdk.api.session.room.model.message.MessageVideoContent
import org.matrix.android.sdk.api.session.room.model.message.getFileUrl
import org.matrix.android.sdk.api.session.room.model.message.getThumbnailUrl
import org.matrix.android.sdk.api.session.room.uploads.UploadEvent
import javax.inject.Inject

class UploadsMediaController @Inject constructor(
        private val errorFormatter: ErrorFormatter,
        private val imageContentRenderer: ImageContentRenderer,
        private val stringProvider: StringProvider,
        dimensionConverter: DimensionConverter
) : TypedEpoxyController<RoomUploadsViewState>() {

    interface Listener {
        fun onOpenImageClicked(view: View, mediaData: ImageContentRenderer.Data)
        fun onOpenVideoClicked(view: View, mediaData: VideoContentRenderer.Data)
        fun loadMore()
    }

    var listener: Listener? = null

    private var idx = 0

    private val itemSize = dimensionConverter.dpToPx(IMAGE_SIZE_DP)

    override fun buildModels(data: RoomUploadsViewState?) {
        data ?: return
        val host = this

        buildMediaItems(data.mediaEvents)

        if (data.hasMore) {
            squareLoadingItem {
                // Always use a different id, because we can be notified several times of visibility state changed
                id("loadMore${host.idx++}")
                onVisibilityStateChanged { _, _, visibilityState ->
                    if (visibilityState == VisibilityState.VISIBLE) {
                        host.listener?.loadMore()
                    }
                }
            }
        }
    }

    private fun buildMediaItems(mediaEvents: List<UploadEvent>) {
        val host = this
        mediaEvents.forEach { uploadEvent ->
            when (uploadEvent.contentWithAttachmentContent.msgType) {
                MessageType.MSGTYPE_IMAGE -> {
                    val data = uploadEvent.toImageContentRendererData() ?: return@forEach
                    uploadsImageItem {
                        id(uploadEvent.eventId)
                        imageContentRenderer(host.imageContentRenderer)
                        data(data)
                        listener {
                            host.listener?.onOpenImageClicked(it, data)
                        }
                    }
                }
                MessageType.MSGTYPE_VIDEO -> {
                    val data = uploadEvent.toVideoContentRendererData() ?: return@forEach
                    uploadsVideoItem {
                        id(uploadEvent.eventId)
                        imageContentRenderer(host.imageContentRenderer)
                        data(data)
                        listener {
                            host.listener?.onOpenVideoClicked(it, data)
                        }
                    }
                }
            }
        }
    }

    private fun UploadEvent.toImageContentRendererData(): ImageContentRenderer.Data? {
        val messageContent = (contentWithAttachmentContent as? MessageImageContent) ?: return null

        return ImageContentRenderer.Data(
                eventId = eventId,
                filename = messageContent.body,
                url = messageContent.getFileUrl(),
                mimeType = messageContent.mimeType,
                elementToDecrypt = messageContent.encryptedFileInfo?.toElementToDecrypt(),
                height = messageContent.info?.height,
                maxHeight = itemSize,
                width = messageContent.info?.width,
                maxWidth = itemSize
        )
    }

    private fun UploadEvent.toVideoContentRendererData(): VideoContentRenderer.Data? {
        val messageContent = (contentWithAttachmentContent as? MessageVideoContent) ?: return null

        val thumbnailData = ImageContentRenderer.Data(
                eventId = eventId,
                filename = messageContent.body,
                mimeType = messageContent.mimeType,
                url = messageContent.videoInfo?.getThumbnailUrl(),
                elementToDecrypt = messageContent.videoInfo?.thumbnailFile?.toElementToDecrypt(),
                height = messageContent.videoInfo?.height,
                maxHeight = itemSize,
                width = messageContent.videoInfo?.width,
                maxWidth = itemSize
        )

        return VideoContentRenderer.Data(
                eventId = eventId,
                filename = messageContent.body,
                mimeType = messageContent.mimeType,
                url = messageContent.getFileUrl(),
                elementToDecrypt = messageContent.encryptedFileInfo?.toElementToDecrypt(),
                thumbnailMediaData = thumbnailData
        )
    }
}
