package im.vector.riotredesign.features.media

import android.media.ExifInterface
import android.widget.ImageView
import im.vector.matrix.android.api.Matrix
import im.vector.matrix.android.api.session.content.ContentUrlResolver
import im.vector.riotredesign.core.glide.GlideApp

object MediaContentRenderer {

    data class Data(
            val url: String?,
            val height: Int,
            val maxHeight: Int,
            val width: Int,
            val maxWidth: Int = width,
            val orientation: Int,
            val rotation: Int
    )

    enum class Mode {
        FULL_SIZE,
        THUMBNAIL
    }

    fun render(data: Data, mode: Mode, imageView: ImageView) {
        val (width, height) = processSize(data, mode)
        imageView.layoutParams.height = height
        imageView.layoutParams.width = width

        val contentUrlResolver = Matrix.getInstance().currentSession.contentUrlResolver()
        val resolvedUrl = when (mode) {
            Mode.FULL_SIZE -> contentUrlResolver.resolveFullSize(data.url)
            Mode.THUMBNAIL -> contentUrlResolver.resolveThumbnail(data.url, width, height, ContentUrlResolver.ThumbnailMethod.SCALE)
        }
                ?: return

        GlideApp
                .with(imageView)
                .load(resolvedUrl)
                .thumbnail(0.3f)
                .into(imageView)
    }

    private fun processSize(data: Data, mode: Mode): Pair<Int, Int> {
        val maxImageWidth = data.maxWidth
        val maxImageHeight = data.maxHeight
        val rotationAngle = data.rotation
        val orientation = data.orientation
        var width = data.width
        var height = data.height
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