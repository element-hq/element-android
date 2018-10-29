package im.vector.riotredesign.features.home.room.list

import android.graphics.drawable.Drawable
import android.widget.ImageView
import android.widget.TextView
import im.vector.riotredesign.R
import im.vector.riotredesign.core.epoxy.KotlinModel
import im.vector.riotredesign.core.platform.CheckableConstraintLayout


data class RoomSummaryItem(
        val title: CharSequence,
        val avatarDrawable: Drawable,
        val isSelected: Boolean,
        val listener: (() -> Unit)? = null
) : KotlinModel(R.layout.item_room) {

    private val titleView by bind<TextView>(R.id.titleView)
    private val avatarImageView by bind<ImageView>(R.id.toolbarAvatarImageView)
    private val rootView by bind<CheckableConstraintLayout>(R.id.itemRoomLayout)

    override fun bind() {
        rootView.isChecked = isSelected
        titleView.setOnClickListener { listener?.invoke() }
        titleView.text = title
        avatarImageView.setImageDrawable(avatarDrawable)
    }
}