package im.vector.riotredesign.features.home.room.list

import android.support.annotation.DrawableRes
import im.vector.riotredesign.R
import im.vector.riotredesign.core.epoxy.KotlinModel

data class RoomCategoryItem(
        val title: CharSequence,
        @DrawableRes val expandDrawable: Int,
        val listener: (() -> Unit)? = null
) : KotlinModel(R.layout.item_room_category) {

    override fun bind() {

    }
}
