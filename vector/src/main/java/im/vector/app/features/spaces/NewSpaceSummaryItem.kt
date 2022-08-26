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
import android.widget.TextView
import androidx.core.view.isVisible
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
abstract class NewSpaceSummaryItem : VectorEpoxyModel<NewSpaceSummaryItem.Holder>(R.layout.item_new_space) {

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) lateinit var avatarRenderer: AvatarRenderer
    @EpoxyAttribute var countState: UnreadCounterBadgeView.State = UnreadCounterBadgeView.State.Count(0, false)
    @EpoxyAttribute var expanded: Boolean = false
    @EpoxyAttribute var hasChildren: Boolean = false
    @EpoxyAttribute lateinit var matrixItem: MatrixItem
    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) var onLongClickListener: ClickListener? = null
    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) var onSpaceSelectedListener: ClickListener? = null
    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) var onToggleExpandListener: ClickListener? = null
    @EpoxyAttribute var selected: Boolean = false

    override fun bind(holder: Holder) {
        super.bind(holder)
        val context = holder.root.context
        holder.root.onClick(onSpaceSelectedListener)
        holder.root.setOnLongClickListener {
            onLongClickListener?.invoke(holder.root)
            true
        }
        holder.name.text = matrixItem.displayName
        holder.root.isChecked = selected

        holder.chevron.setOnClickListener(onToggleExpandListener)
        holder.chevron.isVisible = hasChildren
        holder.chevron.setImageResource(if (expanded) R.drawable.ic_expand_more else R.drawable.ic_arrow_right)
        holder.chevron.contentDescription = context.getString(if (expanded) R.string.a11y_collapse_space_children else R.string.a11y_expand_space_children)

        avatarRenderer.render(matrixItem, holder.avatar)
        holder.unreadCounter.render(countState)
    }

    override fun unbind(holder: Holder) {
        avatarRenderer.clear(holder.avatar)
        super.unbind(holder)
    }

    class Holder : VectorEpoxyHolder() {
        val root by bind<CheckableConstraintLayout>(R.id.root)
        val avatar by bind<ImageView>(R.id.avatar)
        val name by bind<TextView>(R.id.name)
        val unreadCounter by bind<UnreadCounterBadgeView>(R.id.unread_counter)
        val chevron by bind<ImageView>(R.id.chevron)
    }
}
