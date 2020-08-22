/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.home.room.detail.timeline.item

import android.util.TypedValue
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.timeline.TimelineEventController

@EpoxyModelClass(layout = R.layout.item_timeline_event_base_noinfo)
abstract class MergedUTDItem : BasedMergedItem<MergedUTDItem.Holder>() {

    @EpoxyAttribute
    override lateinit var attributes: Attributes

    @EpoxyAttribute
    var big: Boolean? = false

    override fun getViewType() = STUB_ID

    override fun bind(holder: Holder) {
        super.bind(holder)

        holder.mergedTile.updateLayoutParams<RelativeLayout.LayoutParams> {
            this.marginEnd = leftGuideline
            if (big == true) {
                this.height = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        800f,
                        holder.view.context.resources.displayMetrics
                ).toInt()
            } else {
                this.height = LinearLayout.LayoutParams.WRAP_CONTENT
            }
        }

//        if (attributes.isCollapsed) {
//            // Take the oldest data
//            val data = distinctMergeData.lastOrNull()
//
//            val summary = holder.expandView.resources.getString(R.string.room_created_summary_item,
//                    data?.memberName ?: data?.userId ?: "")
//            holder.summaryView.text = summary
//            holder.summaryView.visibility = View.VISIBLE
//            holder.avatarView.visibility = View.VISIBLE
//            if (data != null) {
//                holder.avatarView.visibility = View.VISIBLE
//                attributes.avatarRenderer.render(data.toMatrixItem(), holder.avatarView)
//            } else {
//                holder.avatarView.visibility = View.GONE
//            }
//
//            if (attributes.hasEncryptionEvent) {
//                holder.encryptionTile.isVisible = true
//                holder.encryptionTile.updateLayoutParams<RelativeLayout.LayoutParams> {
//                    this.marginEnd = leftGuideline
//                }
//                if (attributes.isEncryptionAlgorithmSecure) {
//                    holder.e2eTitleTextView.text = holder.expandView.resources.getString(R.string.encryption_enabled)
//                    holder.e2eTitleDescriptionView.text = holder.expandView.resources.getString(R.string.encryption_enabled_tile_description)
//                    holder.e2eTitleDescriptionView.textAlignment = View.TEXT_ALIGNMENT_CENTER
//                    holder.e2eTitleTextView.setCompoundDrawablesWithIntrinsicBounds(
//                            ContextCompat.getDrawable(holder.view.context, R.drawable.ic_shield_black),
//                            null, null, null
//                    )
//                } else {
//                    holder.e2eTitleTextView.text = holder.expandView.resources.getString(R.string.encryption_not_enabled)
//                    holder.e2eTitleDescriptionView.text = holder.expandView.resources.getString(R.string.encryption_unknown_algorithm_tile_description)
//                    holder.e2eTitleTextView.setCompoundDrawablesWithIntrinsicBounds(
//                            ContextCompat.getDrawable(holder.view.context, R.drawable.ic_shield_warning),
//                            null, null, null
//                    )
//                }
//            } else {
//                holder.encryptionTile.isVisible = false
//            }
//        } else {
//            holder.avatarView.visibility = View.INVISIBLE
//            holder.summaryView.visibility = View.GONE
//            holder.encryptionTile.isGone = true
//        }
        // No read receipt for this item
        holder.readReceiptsView.isVisible = false
    }

    class Holder : BasedMergedItem.Holder(STUB_ID) {
        //        val summaryView by bind<TextView>(R.id.itemNoticeTextView)
//        val avatarView by bind<ImageView>(R.id.itemNoticeAvatarView)
        val mergedTile by bind<ViewGroup>(R.id.mergedUTDTile)
//
//        val e2eTitleTextView by bind<TextView>(R.id.itemVerificationDoneTitleTextView)
//        val e2eTitleDescriptionView by bind<TextView>(R.id.itemVerificationDoneDetailTextView)
    }

    companion object {
        private const val STUB_ID = R.id.messageContentMergedUTDStub
    }

    data class Attributes(
            override val isCollapsed: Boolean,
            override val mergeData: List<Data>,
            override val avatarRenderer: AvatarRenderer,
            override val readReceiptsCallback: TimelineEventController.ReadReceiptsCallback? = null,
            override val onCollapsedStateChanged: (Boolean) -> Unit
    ) : BasedMergedItem.Attributes
}
