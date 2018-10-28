package im.vector.riotredesign.features.home.list

import android.support.v4.content.ContextCompat
import android.widget.TextView
import im.vector.riotredesign.R
import im.vector.riotredesign.core.epoxy.KotlinModel
import im.vector.riotredesign.core.platform.CheckableConstraintLayout

data class RoomSummaryItem(
        val title: CharSequence,
        val isSelected: Boolean,
        val listener: (() -> Unit)? = null
) : KotlinModel(R.layout.item_room) {

    val titleView by bind<TextView>(R.id.titleView)
    val rootView by bind<CheckableConstraintLayout>(R.id.itemRoomLayout)

    override fun bind() {
        rootView.isChecked = isSelected
        titleView.setOnClickListener { listener?.invoke() }
        titleView.text = title
    }
}