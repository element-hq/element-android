package im.vector.riotredesign.features.home.room.list

import android.support.v4.content.ContextCompat
import android.view.ViewGroup
import android.widget.TextView
import im.vector.riotredesign.R
import im.vector.riotredesign.core.epoxy.KotlinModel

data class RoomCategoryItem(
        val title: CharSequence,
        val isExpanded: Boolean,
        val listener: (() -> Unit)? = null
) : KotlinModel(R.layout.item_room_category) {

    private val titleView by bind<TextView>(R.id.roomCategoryTitleView)
    private val rootView by bind<ViewGroup>(R.id.roomCategoryRootView)

    private val tintColor by lazy {
        ContextCompat.getColor(rootView.context, R.color.bluey_grey_two)
    }

    override fun bind() {
        val expandedArrowDrawableRes = if (isExpanded) R.drawable.ic_expand_more_white else R.drawable.ic_expand_less_white
        val expandedArrowDrawable = ContextCompat.getDrawable(rootView.context, expandedArrowDrawableRes)
        expandedArrowDrawable?.setTint(tintColor)
        titleView.setCompoundDrawablesWithIntrinsicBounds(expandedArrowDrawable, null, null, null)
        titleView.text = title
        rootView.setOnClickListener { listener?.invoke() }
    }
}
