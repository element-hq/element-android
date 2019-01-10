package im.vector.riotredesign.features.home.room.detail.timeline

import android.widget.TextView
import im.vector.riotredesign.R
import im.vector.riotredesign.core.epoxy.KotlinModel

class TextItem(
        val text: CharSequence? = null
) : KotlinModel(R.layout.item_event_text) {

    private val messageView by bind<TextView>(R.id.stateMessageView)

    override fun bind() {
        messageView.text = text
    }
}