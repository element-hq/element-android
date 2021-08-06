/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.media

import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Parcelable
import android.view.View
import android.widget.ImageView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.target.Target
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView.ORIENTATION_USE_EXIF
import com.github.piasy.biv.view.BigImageView
import im.vector.app.R
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.files.LocalFilesHelper
import im.vector.app.core.glide.GlideApp
import im.vector.app.core.glide.GlideRequest
import im.vector.app.core.glide.GlideRequests
import im.vector.app.core.ui.model.Size
import im.vector.app.core.utils.DimensionConverter
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.content.ContentUrlResolver
import org.matrix.android.sdk.internal.crypto.attachments.ElementToDecrypt
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import kotlin.math.min

interface AttachmentData : Parcelable {
    val eventId: String
    val filename: String
    val mimeType: String?
    val url: String?
    val elementToDecrypt: ElementToDecrypt?

    // If true will load non mxc url, be careful to set it only for attachments sent by you
    val allowNonMxcUrls: Boolean
}

class ImageContentRenderer @Inject constructor(private val localFilesHelper: LocalFilesHelper,
                                               private val activeSessionHolder: ActiveSessionHolder,
                                               private val dimensionConverter: DimensionConverter) {

    @Parcelize
    data class Data(
            override val eventId: String,
            override val filename: String,
            override val mimeType: String?,
            override val url: String?,
            override val elementToDecrypt: ElementToDecrypt?,
            val height: Int?,
            val maxHeight: Int,
            val width: Int?,
            val maxWidth: Int,
            // If true will load non mxc url, be careful to set it only for images sent by you
            override val allowNonMxcUrls: Boolean = false
    ) : AttachmentData

    enum class Mode {
        FULL_SIZE,
        THUMBNAIL,
        // Animated thumbnail: scale like a thumbnail, but use full_size resource,
        // since the thumbnail resource might not support animating.
        ANIMATED_THUMBNAIL,
        STICKER
    }

    /**
     * For url preview
     */
    fun render(mxcUrl: String, imageView: ImageView, hideOnFail: Boolean = false): Boolean {
        val contentUrlResolver = activeSessionHolder.getActiveSession().contentUrlResolver()
        val imageUrl = contentUrlResolver.resolveFullSize(mxcUrl) ?: return false

        GlideApp.with(imageView)
                .load(imageUrl)
                .listener(object: RequestListener<Drawable> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                        Timber.e("Rendering url $imageUrl failed: $e")
                        if (hideOnFail) {
                            imageView.isGone = true
                        }
                        return false
                    }

                    override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                        if (hideOnFail) {
                            imageView.isVisible = true
                        }
                        return false
                    }
                })
                .into(imageView)
        return true
    }

    /**
     * For gallery
     */
    fun render(data: Data, imageView: ImageView, size: Int) {
        // a11y
        imageView.contentDescription = data.filename

        createGlideRequest(data, Mode.THUMBNAIL, imageView, Size(size, size))
                .placeholder(R.drawable.ic_image)
                .into(imageView)
    }

    fun render(data: Data, mode: Mode, imageView: ImageView, onImageSizeListener: OnImageSizeListener? = null, animate: Boolean = false) {
        val size = processSize(data, mode)
        imageView.updateLayoutParams {
            width = size.width
            height = size.height
        }
        onImageSizeListener?.onImageSizeUpdated(size.width, size.height)
        // a11y
        imageView.contentDescription = data.filename

        var request = createGlideRequest(data, mode, imageView, size)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                        return false
                    }

                    override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                        if (resource != null && (data.width == null || data.height == null || data.width == 0 || data.height == 0)) {
                            val updatedData = data.copy(width = resource.intrinsicWidth, height = resource.intrinsicHeight)
                            val newSize = processSize(updatedData, mode)
                            imageView.updateLayoutParams {
                                width = newSize.width
                                height = newSize.height
                            }
                            onImageSizeListener?.onImageSizeUpdated(newSize.width, newSize.height)
                        }
                        return false
                    }
                })
        request = if (animate) {
            // Glide seems to already do some dp to px calculation for animated gifs?
            request.transform(RoundedCorners(3))
            //request.apply(RequestOptions.bitmapTransform(RoundedCorners(3)))
        } else {
            request.dontAnimate()
                    .transform(RoundedCorners(dimensionConverter.dpToPx(3)))
        }
        request
                // .thumbnail(0.3f)
                .into(imageView)
    }

    fun clear(imageView: ImageView) {
        // It can be called after recycler view is destroyed, just silently catch
        // We'd better keep ref to requestManager, but we don't have it
        tryOrNull {
            GlideApp
                    .with(imageView).clear(imageView)
        }
    }

    /**
     * Used by Attachment Viewer
     */
    fun render(data: Data, contextView: View, target: CustomViewTarget<*, Drawable>) {
        val req = if (data.elementToDecrypt != null) {
            // Encrypted image
            GlideApp
                    .with(contextView)
                    .load(data)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
        } else {
            // Clear image
            val resolvedUrl = resolveUrl(data)
            GlideApp
                    .with(contextView)
                    .load(resolvedUrl)
        }

        req.override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                .fitCenter()
                .into(target)
    }

    fun renderFitTarget(data: Data, mode: Mode, imageView: ImageView, callback: ((Boolean) -> Unit)? = null) {
        val size = processSize(data, mode)

        // a11y
        imageView.contentDescription = data.filename

        createGlideRequest(data, mode, imageView, size)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(e: GlideException?,
                                              model: Any?,
                                              target: Target<Drawable>?,
                                              isFirstResource: Boolean): Boolean {
                        callback?.invoke(false)
                        return false
                    }

                    override fun onResourceReady(resource: Drawable?,
                                                 model: Any?,
                                                 target: Target<Drawable>?,
                                                 dataSource: DataSource?,
                                                 isFirstResource: Boolean): Boolean {
                        callback?.invoke(true)
                        return false
                    }
                })
                .fitCenter()
                .into(imageView)
    }

    /**
     * onlyRetrieveFromCache is true!
     */
    fun renderForSharedElementTransition(data: Data, imageView: ImageView, callback: ((Boolean) -> Unit)? = null) {
        // a11y
        imageView.contentDescription = data.filename

        val req = if (data.elementToDecrypt != null) {
            // Encrypted image
            GlideApp
                    .with(imageView)
                    .load(data)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
        } else {
            // Clear image
            val resolvedUrl = resolveUrl(data)
            GlideApp
                    .with(imageView)
                    .load(resolvedUrl)
        }

        req.listener(object : RequestListener<Drawable> {
            override fun onLoadFailed(e: GlideException?,
                                      model: Any?,
                                      target: Target<Drawable>?,
                                      isFirstResource: Boolean): Boolean {
                callback?.invoke(false)
                return false
            }

            override fun onResourceReady(resource: Drawable?,
                                         model: Any?,
                                         target: Target<Drawable>?,
                                         dataSource: DataSource?,
                                         isFirstResource: Boolean): Boolean {
                callback?.invoke(true)
                return false
            }
        })
                .onlyRetrieveFromCache(true)
                .fitCenter()
                .into(imageView)
    }

    private fun createGlideRequest(data: Data, mode: Mode, imageView: ImageView, size: Size): GlideRequest<Drawable> {
        return createGlideRequest(data, mode, GlideApp.with(imageView), size)
    }

    fun createGlideRequest(data: Data, mode: Mode, glideRequests: GlideRequests, size: Size = processSize(data, mode)): GlideRequest<Drawable> {
        return if (data.elementToDecrypt != null) {
            // Encrypted image
            glideRequests
                    .load(data)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
        } else {
            // Clear image
            val contentUrlResolver = activeSessionHolder.getActiveSession().contentUrlResolver()
            val resolvedUrl = when (mode) {
                Mode.FULL_SIZE,
                Mode.ANIMATED_THUMBNAIL,
                Mode.STICKER   -> resolveUrl(data)
                Mode.THUMBNAIL -> contentUrlResolver.resolveThumbnail(data.url, size.width, size.height, ContentUrlResolver.ThumbnailMethod.SCALE)
            }
            // Fallback to base url
                    ?: data.url.takeIf { it?.startsWith("content://") == true }

            glideRequests
                    .load(resolvedUrl)
                    .apply {
                        if (mode == Mode.THUMBNAIL) {
                            error(
                                    glideRequests.load(resolveUrl(data))
                            )
                        }
                    }
        }
    }

    fun render(data: Data, imageView: BigImageView) {
        // a11y
        imageView.contentDescription = data.filename

        val (width, height) = processSize(data, Mode.THUMBNAIL)
        val contentUrlResolver = activeSessionHolder.getActiveSession().contentUrlResolver()
        val fullSize = resolveUrl(data)
        val thumbnail = contentUrlResolver.resolveThumbnail(data.url, width, height, ContentUrlResolver.ThumbnailMethod.SCALE)

        if (fullSize.isNullOrBlank() || thumbnail.isNullOrBlank()) {
            Timber.w("Invalid urls")
            return
        }

        imageView.setImageLoaderCallback(object : DefaultImageLoaderCallback {
            override fun onSuccess(image: File?) {
                imageView.ssiv?.orientation = ORIENTATION_USE_EXIF
            }
        })

        imageView.showImage(
                Uri.parse(thumbnail),
                Uri.parse(fullSize)
        )
    }

    private fun resolveUrl(data: Data) =
            (activeSessionHolder.getActiveSession().contentUrlResolver().resolveFullSize(data.url)
                    ?: data.url?.takeIf { localFilesHelper.isLocalFile(data.url) && data.allowNonMxcUrls })

    private fun processSize(data: Data, mode: Mode): Size {
        var maxImageWidth = data.maxWidth
        var maxImageHeight = data.maxHeight
        val width = data.width ?: maxImageWidth
        val height = data.height ?: maxImageHeight

        // Avoid excessive upscaling
        val maxUpscale = dimensionConverter.dpToPx(4)
        if (width in 1 until maxImageWidth) {
            maxImageWidth = min(maxImageWidth, width*maxUpscale)
        }
        if (height in 1 until maxImageHeight) {
            maxImageHeight = min(maxImageHeight, height*maxUpscale)
        }

        var finalWidth = -1
        var finalHeight = -1

        // if the image size is known
        // compute the expected height
        if (width > 0 && height > 0) {
            when (mode) {
                Mode.FULL_SIZE -> {
                    finalHeight = height
                    finalWidth = width
                }
                Mode.ANIMATED_THUMBNAIL,
                Mode.THUMBNAIL -> {
                    finalHeight = min(maxImageWidth * height / width, maxImageHeight)
                    finalWidth = finalHeight * width / height
                }
                Mode.STICKER   -> {
                    // limit on width
                    val maxWidthDp = min(dimensionConverter.dpToPx(240), maxImageWidth)
                    finalWidth = min(dimensionConverter.dpToPx(width), maxWidthDp)
                    finalHeight = finalWidth * height / width
                }
            }
        }
        // ensure that some values are properly initialized
        if (finalHeight < 0) {
            finalHeight = maxImageHeight
        }
        if (finalWidth < 0) {
            finalWidth = maxImageWidth
        }
        return Size(finalWidth, finalHeight)
    }

    interface OnImageSizeListener {
        fun onImageSizeUpdated(width: Int, height: Int)
    }
}
