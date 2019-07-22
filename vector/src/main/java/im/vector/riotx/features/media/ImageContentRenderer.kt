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
import androidx.exifinterface.media.ExifInterface
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
import im.vector.riotx.core.utils.DimensionUtils.dpToPx
import kotlinx.android.parcel.Parcelize
import timber.log.Timber
import java.io.File
import javax.inject.Inject

class ImageContentRenderer @Inject constructor(private val activeSessionHolder: ActiveSessionHolder) {

    @Parcelize
    data class Data(
            val filename: String,
            val url: String?,
            val elementToDecrypt: ElementToDecrypt?,
            val height: Int?,
            val maxHeight: Int,
            val width: Int?,
            val maxWidth: Int,
            val orientation: Int? = null,
            val rotation: Int? = null
    ) : Parcelable {

        fun isLocalFile(): Boolean {
            return url != null && File(url).exists()
        }
    }

    enum class Mode {
        FULL_SIZE,
        THUMBNAIL
    }

    fun render(data: Data, mode: Mode, imageView: ImageView) {
        val (width, height) = processSize(data, mode)
        imageView.layoutParams.height = height
        imageView.layoutParams.width = width

        val glideRequest = if (data.elementToDecrypt != null) {
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

        glideRequest
                .dontAnimate()
                .transform(RoundedCorners(dpToPx(8, imageView.context)))
                .thumbnail(0.3f)
                .into(imageView)

    }

    fun renderFitTarget(data: Data, mode: Mode, imageView: ImageView, callback :((Boolean) -> Unit)? = null) {
        val (width, height) = processSize(data, mode)

        val glideRequest = if (data.elementToDecrypt != null) {
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

        glideRequest
                .listener(object: RequestListener<Drawable> {
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

    fun render(data: Data, imageView: BigImageView) {
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
        val rotationAngle = data.rotation ?: 0
        val orientation = data.orientation ?: ExifInterface.ORIENTATION_NORMAL
        var width = data.width ?: maxImageWidth
        var height = data.height ?: maxImageHeight
        var finalHeight = -1
        var finalWidth = -1

        // if the image size is known
        // compute the expected height
        if (width > 0 && height > 0) {
            // swap width and height if the image is side oriented
            if (rotationAngle == 90 || rotationAngle == 270) {
                val tmp = width
                width = height
                height = tmp
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_90 || orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                val tmp = width
                width = height
                height = tmp
            }
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