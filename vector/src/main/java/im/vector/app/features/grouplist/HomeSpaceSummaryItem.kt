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
 *
 */

package im.vector.app.features.grouplist

import android.content.res.Resources
import android.util.TypedValue
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.platform.CheckableConstraintLayout

@EpoxyModelClass(layout = R.layout.item_space)
abstract class HomeSpaceSummaryItem : VectorEpoxyModel<HomeSpaceSummaryItem.Holder>() {

    @EpoxyAttribute var selected: Boolean = false
    @EpoxyAttribute var listener: (() -> Unit)? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.rootView.setOnClickListener { listener?.invoke() }
        holder.groupNameView.text = holder.view.context.getString(R.string.group_details_home)
        holder.rootView.isChecked = selected
        holder.rootView.context.resources
        holder.avatarImageView.background = ContextCompat.getDrawable(holder.view.context, R.drawable.space_home_background)
        holder.avatarImageView.setImageResource(R.drawable.ic_space_home)
        holder.avatarImageView.scaleType = ImageView.ScaleType.CENTER_INSIDE
    }

    class Holder : VectorEpoxyHolder() {
        val avatarImageView by bind<ImageView>(R.id.groupAvatarImageView)
        val groupNameView by bind<TextView>(R.id.groupNameView)
        val rootView by bind<CheckableConstraintLayout>(R.id.itemGroupLayout)
    }

    fun dpToPx(resources: Resources, dp: Int): Int {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp.toFloat(),
                resources.displayMetrics
        ).toInt()
    }
}
