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

package im.vector.riotx.features.roomprofile.uploads.media

import android.view.View
import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import im.vector.matrix.android.api.session.events.model.Event
import im.vector.matrix.android.api.session.events.model.isImageMessage
import im.vector.matrix.android.api.session.events.model.isVideoMessage
import im.vector.matrix.android.api.session.events.model.toModel
import im.vector.matrix.android.api.session.room.model.message.MessageImageContent
import im.vector.matrix.android.api.session.room.model.message.MessageImageInfoContent
import im.vector.matrix.android.api.session.room.model.message.MessageVideoContent
import im.vector.matrix.android.api.session.room.model.message.getFileUrl
import im.vector.matrix.android.internal.crypto.attachments.toElementToDecrypt
import im.vector.riotx.R
import im.vector.riotx.core.epoxy.errorWithRetryItem
import im.vector.riotx.core.epoxy.squareLoadingItem
import im.vector.riotx.core.error.ErrorFormatter
import im.vector.riotx.core.resources.ColorProvider
import im.vector.riotx.core.utils.DimensionConverter
import im.vector.riotx.features.media.ImageContentRenderer
import im.vector.riotx.features.media.VideoContentRenderer
import im.vector.riotx.features.roomprofile.uploads.RoomUploadsViewState
import javax.inject.Inject

class UploadsMediaController @Inject constructor(
        private val errorFormatter: ErrorFormatter,
        private val imageContentRenderer: ImageContentRenderer,
        private val dimensionConverter: DimensionConverter,
        colorProvider: ColorProvider
) : TypedEpoxyController<RoomUploadsViewState>() {

    interface Listener {
        fun onRetry()
        fun onOpenImageClicked(view: View, mediaData: ImageContentRenderer.Data)
        fun onOpenVideoClicked(view: View, mediaData: VideoContentRenderer.Data)
    }

    private val dividerColor = colorProvider.getColorFromAttribute(R.attr.vctr_list_divider_color)

    var listener: Listener? = null

    private val itemSize = dimensionConverter.dpToPx(64)

    init {
        setData(null)
    }

    override fun buildModels(data: RoomUploadsViewState?) {
        data ?: return

        if (data.mediaEvents.isEmpty()) {
            when (data.asyncEventsRequest) {
                is Loading -> {
                    squareLoadingItem {
                        id("loading")
                    }
                }
                is Fail    -> {
                    errorWithRetryItem {
                        id("error")
                        text(errorFormatter.toHumanReadable(data.asyncEventsRequest.error))
                        listener { listener?.onRetry() }
                    }
                }
            }
        } else {
            buildMediaItems(data.mediaEvents)

            if (data.hasMore) {
                squareLoadingItem {
                    id("loadMore")
                }
            }
        }
    }

    private fun buildMediaItems(mediaEvents: List<Event>) {
        mediaEvents.forEach { event ->
            when {
                event.isImageMessage() -> {
                    val data = event.toImageContentRendererData() ?: return@forEach
                    uploadsImageItem {
                        id(event.eventId ?: "")
                        imageContentRenderer(imageContentRenderer)
                        data(data)
                        listener(object : UploadsImageItem.Listener {
                            override fun onItemClicked(view: View, data: ImageContentRenderer.Data) {
                                listener?.onOpenImageClicked(view, data)
                            }
                        })
                    }
                }
                event.isVideoMessage() -> {
                    val data = event.toVideoContentRendererData() ?: return@forEach
                    uploadsVideoItem {
                        id(event.eventId ?: "")
                        imageContentRenderer(imageContentRenderer)
                        data(data)
                        listener(object : UploadsVideoItem.Listener {
                            override fun onItemClicked(view: View, data: VideoContentRenderer.Data) {
                                listener?.onOpenVideoClicked(view, data)
                            }
                        })
                    }
                }
            }
        }
    }

    private fun Event.toImageContentRendererData(): ImageContentRenderer.Data? {
        val messageContent = getClearContent()?.toModel<MessageImageContent>() ?: return null

        return ImageContentRenderer.Data(
                eventId = eventId ?: "",
                filename = messageContent.body,
                url = messageContent.getFileUrl(),
                elementToDecrypt = messageContent.encryptedFileInfo?.toElementToDecrypt(),
                height = messageContent.info?.height,
                maxHeight = itemSize,
                width = messageContent.info?.width,
                maxWidth = itemSize
        )
    }

    private fun Event.toVideoContentRendererData(): VideoContentRenderer.Data? {
        val messageContent = getClearContent()?.toModel<MessageVideoContent>() ?: return null

        val thumbnailData = ImageContentRenderer.Data(
                eventId = eventId ?: "",
                filename = messageContent.body,
                url = messageContent.videoInfo?.thumbnailFile?.url ?: messageContent.videoInfo?.thumbnailUrl,
                elementToDecrypt = messageContent.videoInfo?.thumbnailFile?.toElementToDecrypt(),
                height = messageContent.videoInfo?.height,
                maxHeight = itemSize,
                width = messageContent.videoInfo?.width,
                maxWidth = itemSize
        )

        return VideoContentRenderer.Data(
                eventId = eventId ?: "",
                filename = messageContent.body,
                url = messageContent.getFileUrl(),
                elementToDecrypt = messageContent.encryptedFileInfo?.toElementToDecrypt(),
                thumbnailMediaData = thumbnailData
        )
    }
}
