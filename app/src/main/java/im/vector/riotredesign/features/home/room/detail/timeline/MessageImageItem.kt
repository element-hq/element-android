package im.vector.riotredesign.features.home.room.detail.timeline

import android.widget.ImageView
import android.widget.TextView
import im.vector.riotredesign.R
import im.vector.riotredesign.features.media.MediaContentRenderer

class MessageImageItem(
        private val mediaData: MediaContentRenderer.Data,
        informationData: MessageInformationData
) : AbsMessageItem(informationData, R.layout.item_timeline_event_image_message) {

    override val avatarImageView by bind<ImageView>(R.id.messageAvatarImageView)
    override val memberNameView by bind<TextView>(R.id.messageMemberNameView)
    override val timeView by bind<TextView>(R.id.messageTimeView)
    private val imageView by bind<ImageView>(R.id.messageImageView)

    override fun bind() {
        super.bind()
        MediaContentRenderer.render(mediaData, MediaContentRenderer.Mode.THUMBNAIL, imageView)
    }


}