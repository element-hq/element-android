package im.vector.riotredesign.features.home.room.detail

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import im.vector.riotredesign.R
import im.vector.riotredesign.core.epoxy.KotlinModel

data class TimelineMessageItem(
        val message: CharSequence? = null,
        val time: CharSequence? = null,
        val avatarDrawable: Drawable? = null,
        val memberName: CharSequence? = null,
        val showInformation: Boolean = true
) : KotlinModel(R.layout.item_event_message) {

    private val avatarImageView by bind<ImageView>(R.id.messageAvatarImageView)
    private val memberNameView by bind<TextView>(R.id.messageMemberNameView)
    private val timeView by bind<TextView>(R.id.messageTimeView)
    private val messageView by bind<TextView>(R.id.messageTextView)

    override fun bind() {
        messageView.text = message
        if (showInformation) {
            avatarImageView.visibility = View.VISIBLE
            memberNameView.visibility = View.VISIBLE
            timeView.visibility = View.VISIBLE

            avatarImageView.setImageDrawable(avatarDrawable)
            timeView.text = time
            memberNameView.text = memberName
        } else {
            avatarImageView.visibility = View.GONE
            memberNameView.visibility = View.GONE
            timeView.visibility = View.GONE
        }
    }
}