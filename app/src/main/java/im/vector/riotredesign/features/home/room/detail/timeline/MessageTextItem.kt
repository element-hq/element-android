package im.vector.riotredesign.features.home.room.detail.timeline

import android.widget.ImageView
import android.widget.TextView
import im.vector.matrix.android.api.permalinks.MatrixLinkify
import im.vector.riotredesign.R

class MessageTextItem(
        val message: CharSequence? = null,
        informationData: MessageInformationData
) : AbsMessageItem(informationData, R.layout.item_timeline_event_text_message) {

    override val avatarImageView by bind<ImageView>(R.id.messageAvatarImageView)
    override val memberNameView by bind<TextView>(R.id.messageMemberNameView)
    override val timeView by bind<TextView>(R.id.messageTimeView)
    private val messageView by bind<TextView>(R.id.messageTextView)

    override fun bind() {
        super.bind()
        messageView.text = message
        MatrixLinkify.addLinkMovementMethod(messageView)
    }
}