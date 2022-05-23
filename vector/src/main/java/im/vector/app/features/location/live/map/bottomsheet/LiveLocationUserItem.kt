/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.location.live.map.bottomsheet

import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.epoxy.onClick
import im.vector.app.features.home.AvatarRenderer
import org.matrix.android.sdk.api.util.MatrixItem

@EpoxyModelClass(layout = R.layout.item_live_location_users_bottom_sheet)
abstract class LiveLocationUserItem : VectorEpoxyModel<LiveLocationUserItem.Holder>() {

    interface Callback {
        fun onStopSharingClicked()
    }

    @EpoxyAttribute
    var callback: Callback? = null

    @EpoxyAttribute
    lateinit var matrixItem: MatrixItem

    @EpoxyAttribute
    lateinit var avatarRenderer: AvatarRenderer

    @EpoxyAttribute
    var remainingTime: String? = null

    @EpoxyAttribute
    var lastUpdatedAt: String? = null

    @EpoxyAttribute
    var showStopSharingButton: Boolean = false

    override fun bind(holder: Holder) {
        super.bind(holder)
        avatarRenderer.render(matrixItem, holder.itemUserAvatarImageView)
        holder.itemUserDisplayNameTextView.text = matrixItem.displayName
        holder.itemRemainingTimeTextView.text = remainingTime
        holder.itemLastUpdatedAtTextView.text = lastUpdatedAt

        holder.itemStopSharingButton.isVisible = showStopSharingButton
        if (showStopSharingButton) {
            holder.itemStopSharingButton.onClick {
                callback?.onStopSharingClicked()
            }
        }
    }

    class Holder : VectorEpoxyHolder() {
        val itemUserAvatarImageView by bind<ImageView>(R.id.itemUserAvatarImageView)
        val itemUserDisplayNameTextView by bind<TextView>(R.id.itemUserDisplayNameTextView)
        val itemRemainingTimeTextView by bind<TextView>(R.id.itemRemainingTimeTextView)
        val itemLastUpdatedAtTextView by bind<TextView>(R.id.itemLastUpdatedAtTextView)
        val itemStopSharingButton by bind<Button>(R.id.itemStopSharingButton)
    }
}
