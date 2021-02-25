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
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.platform.CheckableConstraintLayout
import im.vector.app.core.utils.DebouncedClickListener
import im.vector.app.features.home.AvatarRenderer
import org.matrix.android.sdk.api.util.MatrixItem

@EpoxyModelClass(layout = R.layout.item_space)
abstract class SpaceSummaryItem : VectorEpoxyModel<SpaceSummaryItem.Holder>() {

    @EpoxyAttribute lateinit var avatarRenderer: AvatarRenderer
    @EpoxyAttribute lateinit var matrixItem: MatrixItem
    @EpoxyAttribute var selected: Boolean = false
    @EpoxyAttribute var listener: (() -> Unit)? = null
    @EpoxyAttribute var onLeave: (() -> Unit)? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.rootView.setOnClickListener { listener?.invoke() }
        holder.groupNameView.text = matrixItem.displayName
        holder.rootView.isChecked = selected
        if (onLeave != null) {
            holder.leaveView.setOnClickListener(
                    DebouncedClickListener({ _ ->
                        onLeave?.invoke()
                    })
            )
        } else {
            holder.leaveView.isVisible = false
        }
        avatarRenderer.renderSpace(matrixItem, holder.avatarImageView)
    }

    override fun unbind(holder: Holder) {
        avatarRenderer.clear(holder.avatarImageView)
        super.unbind(holder)
    }

    class Holder : VectorEpoxyHolder() {
        val avatarImageView by bind<ImageView>(R.id.groupAvatarImageView)
        val groupNameView by bind<TextView>(R.id.groupNameView)
        val rootView by bind<CheckableConstraintLayout>(R.id.itemGroupLayout)
        val leaveView by bind<ImageView>(R.id.groupTmpLeave)
    }
}
