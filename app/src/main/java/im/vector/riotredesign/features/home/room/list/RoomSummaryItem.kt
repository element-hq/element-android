package im.vector.riotredesign.features.home.room.list

import android.widget.ImageView
import android.widget.TextView
import im.vector.riotredesign.R
import im.vector.riotredesign.core.epoxy.KotlinModel
import im.vector.riotredesign.core.platform.CheckableFrameLayout
import im.vector.riotredesign.features.home.AvatarRenderer


data class RoomSummaryItem(
        val roomName: CharSequence,
        val avatarUrl: String?,
        val isSelected: Boolean,
        val listener: (() -> Unit)? = null
) : KotlinModel(R.layout.item_room) {

    private val titleView by bind<TextView>(R.id.titleView)
    private val avatarImageView by bind<ImageView>(R.id.messageAvatarImageView)
    private val rootView by bind<CheckableFrameLayout>(R.id.itemRoomLayout)

    override fun bind() {
        rootView.isChecked = isSelected
        rootView.setOnClickListener { listener?.invoke() }
        titleView.text = roomName
        AvatarRenderer.render(avatarUrl, roomName.toString(), avatarImageView)
    }
}