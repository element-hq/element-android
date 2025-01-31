/*
 * Copyright 2020-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.media

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import androidx.core.view.isVisible
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import im.vector.app.R
import im.vector.app.core.date.DateFormatKind
import im.vector.app.core.date.VectorDateFormatter
import im.vector.app.core.resources.StringProvider
import im.vector.lib.attachmentviewer.AttachmentInfo
import im.vector.lib.attachmentviewer.AttachmentSourceProvider
import im.vector.lib.attachmentviewer.ImageLoaderTarget
import im.vector.lib.attachmentviewer.VideoLoaderTarget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.matrix.android.sdk.api.session.events.model.isVideoMessage
import org.matrix.android.sdk.api.session.file.FileService
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import java.io.File

abstract class BaseAttachmentProvider<Type>(
        private val attachments: List<Type>,
        private val imageContentRenderer: ImageContentRenderer,
        protected val fileService: FileService,
        private val coroutineScope: CoroutineScope,
        private val dateFormatter: VectorDateFormatter,
        private val stringProvider: StringProvider
) : AttachmentSourceProvider {

    var interactionListener: AttachmentInteractionListener? = null

    private var overlayView: AttachmentOverlayView? = null

    final override fun getItemCount() = attachments.size

    protected fun getItem(position: Int) = attachments[position]

    final override fun overlayViewAtPosition(context: Context, position: Int): View? {
        if (position == -1) return null
        if (overlayView == null) {
            overlayView = AttachmentOverlayView(context)
            overlayView?.interactionListener = interactionListener
        }

        val timelineEvent = getTimelineEventAtPosition(position)
        if (timelineEvent != null) {
            val dateString = dateFormatter.format(timelineEvent.root.originServerTs, DateFormatKind.DEFAULT_DATE_AND_TIME)
            overlayView?.updateWith(
                    counter = stringProvider.getString(R.string.attachment_viewer_item_x_of_y, position + 1, getItemCount()),
                    senderInfo = "${timelineEvent.senderInfo.disambiguatedDisplayName} $dateString"
            )
            overlayView?.views?.overlayVideoControlsGroup?.isVisible = timelineEvent.root.isVideoMessage()
        } else {
            overlayView?.updateWith("", "")
        }

        return overlayView
    }

    abstract fun getTimelineEventAtPosition(position: Int): TimelineEvent?

    override fun loadImage(target: ImageLoaderTarget, info: AttachmentInfo.Image) {
        (info.data as? ImageContentRenderer.Data)?.let {
            imageContentRenderer.render(it, target.contextView(), object : CustomViewTarget<ImageView, Drawable>(target.contextView()) {
                override fun onLoadFailed(errorDrawable: Drawable?) {
                    target.onLoadFailed(info.uid, errorDrawable)
                }

                override fun onResourceCleared(placeholder: Drawable?) {
                    target.onResourceCleared(info.uid, placeholder)
                }

                override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                    target.onResourceReady(info.uid, resource)
                }
            })
        }
    }

    override fun loadImage(target: ImageLoaderTarget, info: AttachmentInfo.AnimatedImage) {
        (info.data as? ImageContentRenderer.Data)?.let {
            imageContentRenderer.render(it, target.contextView(), object : CustomViewTarget<ImageView, Drawable>(target.contextView()) {
                override fun onLoadFailed(errorDrawable: Drawable?) {
                    target.onLoadFailed(info.uid, errorDrawable)
                }

                override fun onResourceCleared(placeholder: Drawable?) {
                    target.onResourceCleared(info.uid, placeholder)
                }

                override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                    target.onResourceReady(info.uid, resource)
                }
            })
        }
    }

    override fun loadVideo(target: VideoLoaderTarget, info: AttachmentInfo.Video) {
        val data = info.data as? VideoContentRenderer.Data ?: return

        imageContentRenderer.render(data.thumbnailMediaData, target.contextView(), object : CustomViewTarget<ImageView, Drawable>(target.contextView()) {
            override fun onLoadFailed(errorDrawable: Drawable?) {
                target.onThumbnailLoadFailed(info.uid, errorDrawable)
            }

            override fun onResourceCleared(placeholder: Drawable?) {
                target.onThumbnailResourceCleared(info.uid, placeholder)
            }

            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                target.onThumbnailResourceReady(info.uid, resource)
            }
        })

        if (data.url?.startsWith("content://") == true && data.allowNonMxcUrls) {
            target.onVideoURLReady(info.uid, data.url)
        } else {
            target.onVideoFileLoading(info.uid)
            coroutineScope.launch(Dispatchers.IO) {
                val result = runCatching {
                    fileService.downloadFile(
                            fileName = data.filename,
                            mimeType = data.mimeType,
                            url = data.url,
                            elementToDecrypt = data.elementToDecrypt
                    )
                }
                withContext(Dispatchers.Main) {
                    result.fold(
                            { target.onVideoFileReady(info.uid, it) },
                            { target.onVideoFileLoadFailed(info.uid) }
                    )
                }
            }
        }
    }

    override fun clear(id: String) {
        // TODO("Not yet implemented")
    }

    abstract suspend fun getFileForSharing(position: Int): File?
}
