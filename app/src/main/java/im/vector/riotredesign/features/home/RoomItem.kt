package im.vector.riotredesign.features.home

import android.widget.TextView
import im.vector.riotredesign.R
import im.vector.riotredesign.core.helpers.KotlinModel

data class RoomItem(
        val title: String,
        val listener: (() -> Unit)? = null
) : KotlinModel(R.layout.item_room) {

    val titleView by bind<TextView>(R.id.titleView)

    override fun bind() {
        titleView.setOnClickListener { listener?.invoke() }
        titleView.text = title
    }
}