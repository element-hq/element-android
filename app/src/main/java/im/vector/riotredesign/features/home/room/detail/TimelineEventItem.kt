package im.vector.riotredesign.features.home.room.detail

import android.widget.TextView
import im.vector.riotredesign.R
import im.vector.riotredesign.core.epoxy.KotlinModel

data class TimelineEventItem(
        val title: String
) : KotlinModel(R.layout.item_event) {

    val titleView by bind<TextView>(R.id.titleView)

    override fun bind() {
        titleView.text = title
    }
}