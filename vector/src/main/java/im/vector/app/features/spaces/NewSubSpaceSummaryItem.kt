/*
 * Copyright (c) 2021 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.spaces

import android.widget.ImageView
import android.widget.Space
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.platform.CheckableConstraintLayout
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.list.UnreadCounterBadgeView
import org.matrix.android.sdk.api.util.MatrixItem

@EpoxyModelClass
abstract class NewSubSpaceSummaryItem : VectorEpoxyModel<NewSubSpaceSummaryItem.Holder>(R.layout.item_new_sub_space) {

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) lateinit var avatarRenderer: AvatarRenderer
    @EpoxyAttribute var countState: UnreadCounterBadgeView.State = UnreadCounterBadgeView.State.Count(0, false)
    @EpoxyAttribute var expanded: Boolean = false
    @EpoxyAttribute var hasChildren: Boolean = false
    @EpoxyAttribute var indent: Int = 0
    @EpoxyAttribute lateinit var matrixItem: MatrixItem
    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) var onLongClickListener: ClickListener? = null
    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) var onSubSpaceSelectedListener: ClickListener? = null
    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) var onToggleExpandListener: ClickListener? = null
    @EpoxyAttribute var selected: Boolean = false

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.root.onClick(onSubSpaceSelectedListener)
        holder.name.text = matrixItem.displayName
        holder.root.isChecked = selected
        holder.root.setOnLongClickListener { onLongClickListener?.invoke(holder.root).let { true } }

        holder.chevron.setImageDrawable(
                ContextCompat.getDrawable(
                        holder.view.context,
                        if (expanded) R.drawable.ic_expand_more else R.drawable.ic_arrow_right
                )
        )
        holder.chevron.onClick(onToggleExpandListener)
        holder.chevron.isVisible = hasChildren

        holder.indent.isVisible = indent > 0
        holder.indent.updateLayoutParams {
            width = indent * 30
        }

        avatarRenderer.render(matrixItem, holder.avatar)
        holder.notificationBadge.render(countState)
    }

    override fun unbind(holder: Holder) {
        avatarRenderer.clear(holder.avatar)
        super.unbind(holder)
    }

    class Holder : VectorEpoxyHolder() {
        val avatar by bind<ImageView>(R.id.avatar)
        val name by bind<TextView>(R.id.name)
        val root by bind<CheckableConstraintLayout>(R.id.root)
        val chevron by bind<ImageView>(R.id.chevron)
        val indent by bind<Space>(R.id.indent)
        val notificationBadge by bind<UnreadCounterBadgeView>(R.id.notification_badge)
    }
}
