package im.vector.riotredesign.features.home.room.list

import android.graphics.drawable.Drawable
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.request.RequestOptions
import im.vector.riotredesign.R
import im.vector.riotredesign.core.epoxy.KotlinModel
import im.vector.riotredesign.core.glide.GlideApp
import im.vector.riotredesign.core.platform.CheckableFrameLayout


data class RoomSummaryItem(
        val title: CharSequence,
        val avatarUrl: String,
        val fallbackAvatarDrawable: Drawable,
        val isSelected: Boolean,
        val listener: (() -> Unit)? = null
) : KotlinModel(R.layout.item_room) {

    private val titleView by bind<TextView>(R.id.titleView)
    private val avatarImageView by bind<ImageView>(R.id.messageAvatarImageView)
    private val rootView by bind<CheckableFrameLayout>(R.id.itemRoomLayout)

    override fun bind() {
        rootView.isChecked = isSelected
        rootView.setOnClickListener { listener?.invoke() }
        titleView.text = title
        GlideApp
                .with(avatarImageView)
                .load(avatarUrl)
                .placeholder(fallbackAvatarDrawable)
                .apply(RequestOptions.circleCropTransform())
                .into(avatarImageView)
    }
}