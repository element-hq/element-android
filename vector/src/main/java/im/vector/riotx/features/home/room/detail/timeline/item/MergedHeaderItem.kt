/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.riotx.features.home.room.detail.timeline.item

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.children
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.riotx.R
import im.vector.riotx.features.home.AvatarRenderer
import im.vector.riotx.features.home.room.detail.timeline.TimelineEventController

@EpoxyModelClass(layout = R.layout.item_timeline_event_base_noinfo)
abstract class MergedHeaderItem : BaseEventItem<MergedHeaderItem.Holder>() {

    @EpoxyAttribute
    lateinit var attributes: Attributes

    private val distinctMergeData by lazy {
        attributes.mergeData.distinctBy { it.userId }
    }

    override fun getViewType() = STUB_ID

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.expandView.setOnClickListener {
            attributes.onCollapsedStateChanged(!attributes.isCollapsed)
        }
        if (attributes.isCollapsed) {
            val summary = holder.expandView.resources.getQuantityString(R.plurals.membership_changes, attributes.mergeData.size, attributes.mergeData.size)
            holder.summaryView.text = summary
            holder.summaryView.visibility = View.VISIBLE
            holder.avatarListView.visibility = View.VISIBLE
            holder.avatarListView.children.forEachIndexed { index, view ->
                val data = distinctMergeData.getOrNull(index)
                if (data != null && view is ImageView) {
                    view.visibility = View.VISIBLE
                    attributes.avatarRenderer.render(data.avatarUrl, data.userId, data.memberName, view)
                } else {
                    view.visibility = View.GONE
                }
            }
            holder.separatorView.visibility = View.GONE
            holder.expandView.setText(R.string.merged_events_expand)
        } else {
            holder.avatarListView.visibility = View.INVISIBLE
            holder.summaryView.visibility = View.GONE
            holder.separatorView.visibility = View.VISIBLE
            holder.expandView.setText(R.string.merged_events_collapse)
        }
        // No read receipt for this item
        holder.readReceiptsView.isVisible = false
    }

    override fun getEventIds(): List<String> {
        return attributes.mergeData.map { it.eventId }
    }

    data class Data(
            val localId: Long,
            val eventId: String,
            val userId: String,
            val memberName: String,
            val avatarUrl: String?
    )

    data class Attributes(
            val isCollapsed: Boolean,
            val mergeData: List<Data>,
            val avatarRenderer: AvatarRenderer,
            val readReceiptsCallback: TimelineEventController.ReadReceiptsCallback? = null,
            val onCollapsedStateChanged: (Boolean) -> Unit
    )

    class Holder : BaseHolder(STUB_ID) {
        val expandView by bind<TextView>(R.id.itemMergedExpandTextView)
        val summaryView by bind<TextView>(R.id.itemMergedSummaryTextView)
        val separatorView by bind<View>(R.id.itemMergedSeparatorView)
        val avatarListView by bind<ViewGroup>(R.id.itemMergedAvatarListView)
    }

    companion object {
        private const val STUB_ID = R.id.messageContentMergedheaderStub
    }
}
