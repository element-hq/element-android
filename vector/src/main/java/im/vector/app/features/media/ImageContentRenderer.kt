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

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Parcelable
import android.view.View
import android.widget.ImageView
import androidx.core.view.updateLayoutParams
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.target.ImageViewTarget
import com.bumptech.glide.request.target.Target
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView.ORIENTATION_USE_EXIF
import com.github.piasy.biv.view.BigImageView
import im.vector.app.R
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.files.LocalFilesHelper
import im.vector.app.core.glide.BlurHashData
import im.vector.app.core.glide.GlideApp
import im.vector.app.core.glide.GlideRequest
import im.vector.app.core.glide.GlideRequests
import im.vector.app.core.ui.model.Size
import im.vector.app.core.utils.DimensionConverter
import im.vector.app.features.themes.ThemeUtils
import kotlinx.parcelize.Parcelize
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.session.content.ContentUrlResolver
import org.matrix.android.sdk.api.util.MimeTypes
import org.matrix.android.sdk.internal.crypto.attachments.ElementToDecrypt
import timber.log.Timber
import xyz.belvi.blurhash.BlurHash
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
                                               private val dimensionConverter: DimensionConverter,
        // private val contentDownloadStateTracker: ContentDownloadStateTracker,
                                               private val blurHash: BlurHash) {

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
            val blurHash: String?,
            // If true will load non mxc url, be careful to set it only for images sent by you
            override val allowNonMxcUrls: Boolean = false,
            val autoDownload: Boolean = false
    ) : AttachmentData

    enum class Mode {
        FULL_SIZE,
        THUMBNAIL,
        STICKER
    }

    /**
     * For url preview
     */
    fun render(mxcUrl: String, imageView: ImageView): Boolean {
        val contentUrlResolver = activeSessionHolder.getActiveSession().contentUrlResolver()
        val imageUrl = contentUrlResolver.resolveFullSize(mxcUrl) ?: return false

        GlideApp.with(imageView)
                .load(imageUrl)
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

    interface ContentRendererCallbacks {
        fun onThumbnailModeFinish(success: Boolean)

        fun onLoadModeFinish(success: Boolean)
    }
    /**
     * In timeline
     * All encrypted media will be downloaded by the SDK's FileService, so caller could follow progress using download tracker,
     * but for clear media a server thumbnail will be requested and in this case it will be invisible to download tracker that's why there is the
     * `mxcThumbnailCallback` callback. Caller can use it to know when media is loaded.
     */
    fun render(data: Data, mode: Mode, imageView: ImageView, animate: Boolean = false, rendererCallbacks: ContentRendererCallbacks? = null) {
        val size = processSize(data, mode)
        // This size will be used by glide for bitmap size
        imageView.updateLayoutParams {
            width = size.width
            height = size.height
        }
        // a11y
        imageView.contentDescription = data.filename

        createGlideRequest(data, mode, imageView, size, animate, rendererCallbacks)
                .apply {
                    if (!animate) {
                        dontAnimate()
                    }
                }
                // .dontAnimate()
                .transform(RoundedCorners(dimensionConverter.dpToPx(8)))
                .placeholder(ColorDrawable(ThemeUtils.getColor(imageView.context, R.attr.riotx_reaction_background_off)))
                .apply {
                    if (data.blurHash != null) {
                        thumbnail(
                                GlideApp.with(imageView)
                                        .load(BlurHashData(data.blurHash))
                                        .transform(RoundedCorners(dimensionConverter.dpToPx(8)))
                                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        )
                    }
                }

                .apply {
                    // In case of permanent error, the thumbnail might not be loaded and images goes directly to blank state instead of
                    // loading the blurhash thumbnail.. so ensure that error will use the blur hash
                    if (data.blurHash != null) {
                        error(
                                GlideApp.with(imageView)
                                        .load(BlurHashData(data.blurHash))
                                        .transform(RoundedCorners(dimensionConverter.dpToPx(8)))
                                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        ).error(ColorDrawable(ThemeUtils.getColor(imageView.context, R.attr.riotx_reaction_background_off)))
                    } else {
                        error(ColorDrawable(ThemeUtils.getColor(imageView.context, R.attr.riotx_reaction_background_off)))
                    }
                }
                .into(object : ImageViewTarget<Drawable>(imageView) {
                    override fun setResource(resource: Drawable?) {
                        resource?.let {
                            imageView.post { imageView.setImageDrawable(it) }
                        }
                    }
                })
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
                    .thumbnail()
        }

        req.override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                .thumbnail(
                        GlideApp.with(contextView)
                                .load(BlurHashData(data.blurHash))
                )
                .fitCenter()
                .into(target)
    }

//    fun renderFitTarget(data: Data, mode: Mode, imageView: ImageView, callback: ((Boolean) -> Unit)? = null) {
//        val size = processSize(data, mode)
//
//        // a11y
//        imageView.contentDescription = data.filename
//
//        createGlideRequest(data, mode, imageView, size)
//                .listener(object : RequestListener<Drawable> {
//                    override fun onLoadFailed(e: GlideException?,
//                                              model: Any?,
//                                              target: Target<Drawable>?,
//                                              isFirstResource: Boolean): Boolean {
//                        callback?.invoke(false)
//                        return false
//                    }
//
//                    override fun onResourceReady(resource: Drawable?,
//                                                 model: Any?,
//                                                 target: Target<Drawable>?,
//                                                 dataSource: DataSource?,
//                                                 isFirstResource: Boolean): Boolean {
//                        callback?.invoke(true)
//                        return false
//                    }
//                })
//                .fitCenter()
//                .into(imageView)
//    }

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
                    .asDrawable()
                    .load(data)
        } else {
            // Clear image
            val resolvedUrl = resolveUrl(data)
            GlideApp
                    .with(imageView)
                    .asDrawable()
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

    private fun createGlideRequest(data: Data, mode: Mode, imageView: ImageView, size: Size, autoplay: Boolean = false, rendererCallbacks: ContentRendererCallbacks? = null): GlideRequest<Drawable> {
        return createGlideRequest(data, mode, GlideApp.with(imageView), size, autoplay, rendererCallbacks)
    }

    fun createGlideRequest(data: Data, mode: Mode, glideRequests: GlideRequests, size: Size = processSize(data, mode), autoplay: Boolean = false, rendererCallbacks: ContentRendererCallbacks? = null): GlideRequest<Drawable> {
        return if (data.elementToDecrypt != null) {
            // Encrypted image
            glideRequests
                    .apply {
                        if (!autoplay && data.mimeType == MimeTypes.Gif) {
                            // if it's a gif and that we don't auto play,
                            // there is no point of loading all frames, just use this to take first one
                            asBitmap()
                        }
                    }
                    .load(data)
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                            rendererCallbacks?.onLoadModeFinish(false)
                            return false
                        }

                        override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                            rendererCallbacks?.onLoadModeFinish(true)
                            return false
                        }
                    })
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
        } else {
            // Clear image
            // Check if it's worth it asking the server for a thumbnail
            val shouldQueryThumb = if (mode == Mode.THUMBNAIL) {
                (data.width == null || data.height == null
                        || size.width * size.height < (data.width * data.height) * 2)
            } else false

            if (shouldQueryThumb) {
                val contentUrlResolver = activeSessionHolder.getActiveSession().contentUrlResolver()
                glideRequests
                        .apply {
                            if (!autoplay && data.mimeType == MimeTypes.Gif) {
                                // if it's a gif and that we don't auto play,
                                // there is no point of loading all frames, just use this to take first one
                                asBitmap()
                            }
                        }.load(
                                contentUrlResolver.resolveThumbnail(data.url, size.width, size.height, ContentUrlResolver.ThumbnailMethod.SCALE)
                                        ?: data.url.takeIf { it?.startsWith("content://") == true }
                        ).listener(object : RequestListener<Drawable> {
                            override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                                rendererCallbacks?.onThumbnailModeFinish(false)
                                return false
                            }

                            override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                                rendererCallbacks?.onThumbnailModeFinish(true)
                                return false
                            }
                        })
            } else {
                glideRequests
                        .apply {
                            if (!autoplay && data.mimeType == MimeTypes.Gif) {
                                // if it's a gif and that we don't auto play,
                                // there is no point of loading all frames, just use this to take first one
                                asBitmap()
                            }
                        }
                        .load(data)
                        .listener(object : RequestListener<Drawable> {
                            override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                                rendererCallbacks?.onLoadModeFinish(false)
                                return false
                            }

                            override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                                rendererCallbacks?.onLoadModeFinish(true)
                                return false
                            }
                        })
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
            }

//            glideRequests
//                    .apply {
//                        if (!autoplay && data.mimeType == MimeTypes.Gif) {
//                            // if it's a gif and that we don't auto play,
//                            // there is no point of loading all frames, just use this to take first one
//                            asBitmap()
//                        }
//                    }
//                    .apply {
//                        if (shouldQueryThumb) {
//                            load(
//                                    contentUrlResolver.resolveThumbnail(data.url, size.width, size.height, ContentUrlResolver.ThumbnailMethod.SCALE)
//                                            ?: data.url.takeIf { it?.startsWith("content://") == true }
//                            )
//                        } else {
//                            load(data).diskCacheStrategy(DiskCacheStrategy.NONE)
//                        }
//                    }
//                    .load(
//                            if (shouldQueryThumb) {
//                                contentUrlResolver.resolveThumbnail(data.url, size.width, size.height, ContentUrlResolver.ThumbnailMethod.SCALE)
//                                        ?: data.url.takeIf { it?.startsWith("content://") == true }
//                            } else {
//                                data
//                            }
//                    )
            // cache is handled by the VectorGlideModelLoader
//                    .diskCacheStrategy(DiskCacheStrategy.NONE)
//                    .apply {
//                        if (mode == Mode.THUMBNAIL) {
//                            error(
//                                    glideRequests.load(resolveUrl(data))
//                            )
//                        }
//                    }
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
        val maxImageWidth = data.maxWidth
        val maxImageHeight = data.maxHeight
        val width = data.width ?: maxImageWidth
        val height = data.height ?: maxImageHeight
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
                Mode.THUMBNAIL -> {
                    finalHeight = min(maxImageWidth * height / width, maxImageHeight)
                    finalWidth = finalHeight * width / height
                }
                Mode.STICKER   -> {
                    // limit on width
                    val maxWidthDp = min(dimensionConverter.dpToPx(120), maxImageWidth / 2)
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
}
