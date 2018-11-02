package im.vector.riotredesign.features.home

import android.support.v4.content.ContextCompat
import android.widget.ImageView
import com.amulyakhare.textdrawable.TextDrawable
import com.bumptech.glide.request.RequestOptions
import im.vector.matrix.android.api.session.room.model.RoomMember
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.riotredesign.R
import im.vector.riotredesign.core.extensions.firstCharAsString
import im.vector.riotredesign.core.glide.GlideApp

private const val MEDIA_URL = "https://matrix.org/_matrix/media/v1/download/"
private const val MXC_PREFIX = "mxc://"

object AvatarRenderer {

    fun render(roomMember: RoomMember, imageView: ImageView) {
        render(roomMember.avatarUrl, roomMember.displayName, imageView)
    }

    fun render(roomSummary: RoomSummary, imageView: ImageView) {
        render(roomSummary.avatarUrl, roomSummary.displayName, imageView)
    }

    fun render(avatarUrl: String?, name: String?, imageView: ImageView) {
        if (name.isNullOrEmpty()) {
            return
        }
        val resolvedUrl = avatarUrl?.replace(MXC_PREFIX, MEDIA_URL)
        val avatarColor = ContextCompat.getColor(imageView.context, R.color.pale_teal)
        val fallbackDrawable = TextDrawable.builder().buildRound(name.firstCharAsString().toUpperCase(), avatarColor)

        GlideApp
                .with(imageView)
                .load(resolvedUrl)
                .placeholder(fallbackDrawable)
                .apply(RequestOptions.circleCropTransform())
                .into(imageView)
    }


}