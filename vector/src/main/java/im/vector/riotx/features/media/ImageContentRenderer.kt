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

package im.vector.riotx.features.media

import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Parcelable
import android.widget.ImageView
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.github.piasy.biv.view.BigImageView
import im.vector.matrix.android.api.session.content.ContentUrlResolver
import im.vector.matrix.android.internal.crypto.attachments.ElementToDecrypt
import im.vector.riotx.core.di.ActiveSessionHolder
import im.vector.riotx.core.glide.GlideApp
import im.vector.riotx.core.glide.GlideRequest
import im.vector.riotx.core.utils.DimensionConverter
import im.vector.riotx.core.utils.isLocalFile
import kotlinx.android.parcel.Parcelize
import timber.log.Timber
import javax.inject.Inject

class ImageContentRenderer @Inject constructor(private val activeSessionHolder: ActiveSessionHolder,
                                               private val dimensionConverter: DimensionConverter) {

    @Parcelize
    data class Data(
            val filename: String,
            val url: String?,
            val elementToDecrypt: ElementToDecrypt?,
            val height: Int?,
            val maxHeight: Int,
            val width: Int?,
            val maxWidth: Int
    ) : Parcelable {

        fun isLocalFile() = url.isLocalFile()
    }

    enum class Mode {
        FULL_SIZE,
        THUMBNAIL
    }

    fun render(data: Data, mode: Mode, imageView: ImageView) {
        val (width, height) = processSize(data, mode)
        imageView.layoutParams.height = height
        imageView.layoutParams.width = width
        // a11y
        imageView.contentDescription = data.filename

        createGlideRequest(data, mode, imageView, width, height)
                .dontAnimate()
                .transform(RoundedCorners(dimensionConverter.dpToPx(8)))
                .thumbnail(0.3f)
                .into(imageView)

    }

    fun renderFitTarget(data: Data, mode: Mode, imageView: ImageView, callback: ((Boolean) -> Unit)? = null) {
        val (width, height) = processSize(data, mode)

        // a11y
        imageView.contentDescription = data.filename

        createGlideRequest(data, mode, imageView, width, height)
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

    private fun createGlideRequest(data: Data, mode: Mode, imageView: ImageView, width: Int, height: Int): GlideRequest<Drawable> {
        return if (data.elementToDecrypt != null) {
            // Encrypted image
            GlideApp
                    .with(imageView)
                    .load(data)
        } else {
            // Clear image
            val contentUrlResolver = activeSessionHolder.getActiveSession().contentUrlResolver()
            val resolvedUrl = when (mode) {
                Mode.FULL_SIZE -> contentUrlResolver.resolveFullSize(data.url)
                Mode.THUMBNAIL -> contentUrlResolver.resolveThumbnail(data.url, width, height, ContentUrlResolver.ThumbnailMethod.SCALE)
            }
            //Fallback to base url
                    ?: data.url

            GlideApp
                    .with(imageView)
                    .load(resolvedUrl)
        }
    }

    fun render(data: Data, imageView: BigImageView) {
        // a11y
        imageView.contentDescription = data.filename

        val (width, height) = processSize(data, Mode.THUMBNAIL)
        val contentUrlResolver = activeSessionHolder.getActiveSession().contentUrlResolver()
        val fullSize = contentUrlResolver.resolveFullSize(data.url)
        val thumbnail = contentUrlResolver.resolveThumbnail(data.url, width, height, ContentUrlResolver.ThumbnailMethod.SCALE)

        if (fullSize.isNullOrBlank() || thumbnail.isNullOrBlank()) {
            Timber.w("Invalid urls")
            return
        }

        imageView.showImage(
                Uri.parse(thumbnail),
                Uri.parse(fullSize)
        )
    }

    private fun processSize(data: Data, mode: Mode): Pair<Int, Int> {
        val maxImageWidth = data.maxWidth
        val maxImageHeight = data.maxHeight
        val width = data.width ?: maxImageWidth
        val height = data.height ?: maxImageHeight
        var finalHeight = -1
        var finalWidth = -1

        // if the image size is known
        // compute the expected height
        if (width > 0 && height > 0) {
            if (mode == Mode.FULL_SIZE) {
                finalHeight = height
                finalWidth = width
            } else {
                finalHeight = Math.min(maxImageWidth * height / width, maxImageHeight)
                finalWidth = finalHeight * width / height
            }
        }
        // ensure that some values are properly initialized
        if (finalHeight < 0) {
            finalHeight = maxImageHeight
        }
        if (finalWidth < 0) {
            finalWidth = maxImageWidth
        }
        return Pair(finalWidth, finalHeight)
    }
}