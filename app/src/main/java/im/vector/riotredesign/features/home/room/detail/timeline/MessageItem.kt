package im.vector.riotredesign.features.home.room.detail.timeline

import android.text.util.Linkify
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import im.vector.matrix.android.api.permalinks.MatrixLinkify
import im.vector.matrix.android.api.permalinks.MatrixPermalinkSpan
import im.vector.riotredesign.R
import im.vector.riotredesign.core.epoxy.KotlinModel
import im.vector.riotredesign.features.home.AvatarRenderer

data class MessageItem(
        val message: CharSequence? = null,
        val time: CharSequence? = null,
        val avatarUrl: String?,
        val memberName: CharSequence? = null,
        val showInformation: Boolean = true,
        val onUrlClickedListener: ((url: String) -> Unit)? = null
) : KotlinModel(R.layout.item_event_message) {

    private val avatarImageView by bind<ImageView>(R.id.messageAvatarImageView)
    private val memberNameView by bind<TextView>(R.id.messageMemberNameView)
    private val timeView by bind<TextView>(R.id.messageTimeView)
    private val messageView by bind<TextView>(R.id.messageTextView)

    override fun bind() {
        messageView.text = message
        MatrixLinkify.addLinks(messageView, object : MatrixPermalinkSpan.Callback {
            override fun onUrlClicked(url: String) {
                onUrlClickedListener?.invoke(url)
            }
        })
        Linkify.addLinks(messageView, Linkify.ALL)
        if (showInformation) {
            avatarImageView.visibility = View.VISIBLE
            memberNameView.visibility = View.VISIBLE
            timeView.visibility = View.VISIBLE
            timeView.text = time
            memberNameView.text = memberName
            AvatarRenderer.render(avatarUrl, memberName?.toString(), avatarImageView)
        } else {
            avatarImageView.visibility = View.GONE
            memberNameView.visibility = View.GONE
            timeView.visibility = View.GONE
        }
    }
}