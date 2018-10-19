package im.vector.riotredesign.features.home

import android.widget.TextView
import im.vector.riotredesign.R
import im.vector.riotredesign.core.epoxy.KotlinModel

data class TimelineEventItem(
        val title: String,
        val listener: (() -> Unit)? = null
) : KotlinModel(R.layout.item_event) {

    val titleView by bind<TextView>(R.id.titleView)

    override fun bind() {
        titleView.setOnClickListener { listener?.invoke() }
        titleView.text = title
    }
}