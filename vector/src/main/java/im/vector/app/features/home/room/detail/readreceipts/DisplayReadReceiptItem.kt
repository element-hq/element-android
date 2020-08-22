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

package im.vector.app.features.home.room.detail.readreceipts

import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.airbnb.epoxy.EpoxyModelWithHolder
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.features.home.AvatarRenderer
import org.matrix.android.sdk.api.util.MatrixItem

@EpoxyModelClass(layout = R.layout.item_display_read_receipt)
abstract class DisplayReadReceiptItem : EpoxyModelWithHolder<DisplayReadReceiptItem.Holder>() {

    @EpoxyAttribute lateinit var matrixItem: MatrixItem
    @EpoxyAttribute var timestamp: CharSequence? = null
    @EpoxyAttribute lateinit var avatarRenderer: AvatarRenderer
    @EpoxyAttribute var userClicked: (() -> Unit)? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        avatarRenderer.render(matrixItem, holder.avatarView)
        holder.displayNameView.text = matrixItem.getBestName()
        timestamp?.let {
            holder.timestampView.text = it
            holder.timestampView.isVisible = true
        } ?: run {
            holder.timestampView.isVisible = false
        }
        holder.view.setOnClickListener { userClicked?.invoke() }
    }

    class Holder : VectorEpoxyHolder() {
        val avatarView by bind<ImageView>(R.id.readReceiptAvatar)
        val displayNameView by bind<TextView>(R.id.readReceiptName)
        val timestampView by bind<TextView>(R.id.readReceiptDate)
    }
}
