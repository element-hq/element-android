package im.vector.riotredesign.features.home.room.detail.timeline

import android.widget.TextView
import im.vector.riotredesign.R
import im.vector.riotredesign.core.epoxy.KotlinModel

data class DaySeparatorItem(
        val formattedDay: CharSequence
) : KotlinModel(R.layout.item_event_day_separator) {

    private val dayTextView by bind<TextView>(R.id.itemDayTextView)

    override fun bind() {
        dayTextView.text = formattedDay
    }
}