package im.vector.riotredesign.features.home.room.detail.timeline

import android.widget.ImageView
import android.widget.TextView
import im.vector.riotredesign.R
import im.vector.riotredesign.core.epoxy.KotlinModel
import im.vector.riotredesign.features.home.AvatarRenderer

class NoticeItem(private val noticeText: CharSequence? = null,
                 private val avatarUrl: String?,
                 private val memberName: CharSequence? = null)
    : KotlinModel(R.layout.item_timeline_event_notice) {

    private val avatarImageView by bind<ImageView>(R.id.itemNoticeAvatarView)
    private val noticeTextView by bind<TextView>(R.id.itemNoticeTextView)

    override fun bind() {
        noticeTextView.text = noticeText
        AvatarRenderer.render(avatarUrl, memberName?.toString(), avatarImageView)
    }
}