package im.vector.riotredesign.features.media

import android.content.Context
import android.graphics.Point
import android.media.ExifInterface
import android.view.WindowManager
import android.widget.ImageView
import im.vector.matrix.android.api.session.content.ContentUrlResolver
import im.vector.matrix.android.api.session.room.model.message.MessageImageContent
import im.vector.riotredesign.core.glide.GlideApp

object MessageImageRenderer {

    fun render(messageContent: MessageImageContent, imageView: ImageView) {
        val (maxImageWidth, maxImageHeight) = computeMaxSize(imageView.context)
        val imageInfo = messageContent.info
        val rotationAngle = imageInfo.rotation ?: 0
        val orientation = imageInfo.orientation ?: ExifInterface.ORIENTATION_NORMAL
        var width = imageInfo.width
        var height = imageInfo.height

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
            finalHeight = Math.min(maxImageWidth * height / width, maxImageHeight)
            finalWidth = finalHeight * width / height
        }
        // ensure that some values are properly initialized
        if (finalHeight < 0) {
            finalHeight = maxImageHeight
        }
        if (finalWidth < 0) {
            finalWidth = maxImageWidth
        }
        imageView.layoutParams.height = finalHeight
        imageView.layoutParams.width = finalWidth

        val resolvedUrl = ContentUrlResolver.resolve(messageContent.url) ?: return
        GlideApp
                .with(imageView)
                .load(resolvedUrl)
                .override(finalWidth, finalHeight)
                .thumbnail(0.3f)
                .into(imageView)
    }

    private fun computeMaxSize(context: Context): Pair<Int, Int> {
        val size = Point(0, 0)
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm.defaultDisplay.getSize(size)
        val screenWidth = size.x
        val screenHeight = size.y
        val maxImageWidth: Int
        val maxImageHeight: Int
        // landscape / portrait
        if (screenWidth < screenHeight) {
            maxImageWidth = Math.round(screenWidth * 0.6f)
            maxImageHeight = Math.round(screenHeight * 0.4f)
        } else {
            maxImageWidth = Math.round(screenWidth * 0.4f)
            maxImageHeight = Math.round(screenHeight * 0.6f)
        }
        return Pair(maxImageWidth, maxImageHeight)
    }

}