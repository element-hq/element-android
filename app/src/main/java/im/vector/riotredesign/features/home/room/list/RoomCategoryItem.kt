/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.riotredesign.features.home.room.list

import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
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
        val expandedArrowDrawable = ContextCompat.getDrawable(rootView.context, expandedArrowDrawableRes)?.also {
            DrawableCompat.setTint(it, tintColor)
        }
        titleView.setCompoundDrawablesWithIntrinsicBounds(expandedArrowDrawable, null, null, null)
        titleView.text = title
        rootView.setOnClickListener { listener?.invoke() }
    }
}
