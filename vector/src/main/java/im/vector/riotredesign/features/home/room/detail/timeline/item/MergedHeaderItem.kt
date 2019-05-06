/*
 *
 *  * Copyright 2019 New Vector Ltd
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package im.vector.riotredesign.features.home.room.detail.timeline.item

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.children
import im.vector.riotredesign.R
import im.vector.riotredesign.core.epoxy.VectorEpoxyHolder
import im.vector.riotredesign.core.epoxy.VectorEpoxyModel
import im.vector.riotredesign.features.home.AvatarRenderer

data class MergedHeaderItem(private val isCollapsed: Boolean,
                       private val mergeId: String,
                       private val mergeData: List<Data>,
                       private val onCollapsedStateChanged: (Boolean) -> Unit
) : VectorEpoxyModel<MergedHeaderItem.Holder>() {

    private val distinctMergeData = mergeData.distinctBy { it.userId }

    init {
        id(mergeId)
    }

    override fun getDefaultLayout(): Int {
        return R.layout.item_timeline_event_merged_header
    }

    override fun createNewHolder(): Holder {
        return Holder()
    }

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.expandView.setOnClickListener {
            onCollapsedStateChanged(!isCollapsed)
        }
        if (isCollapsed) {
            val summary = holder.expandView.resources.getQuantityString(R.plurals.membership_changes, mergeData.size, mergeData.size)
            holder.summaryView.text = summary
            holder.summaryView.visibility = View.VISIBLE
            holder.avatarListView.visibility = View.VISIBLE
            holder.avatarListView.children.forEachIndexed { index, view ->
                val data = distinctMergeData.getOrNull(index)
                if (data != null && view is ImageView) {
                    view.visibility = View.VISIBLE
                    AvatarRenderer.render(data.avatarUrl, data.userId, data.memberName, view)
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
    }

    data class Data(
            val eventId: String,
            val userId: String,
            val memberName: String,
            val avatarUrl: String?
    )

    class Holder : VectorEpoxyHolder() {
        val expandView by bind<TextView>(R.id.itemMergedExpandTextView)
        val summaryView by bind<TextView>(R.id.itemMergedSummaryTextView)
        val separatorView by bind<View>(R.id.itemMergedSeparatorView)
        val avatarListView by bind<ViewGroup>(R.id.itemMergedAvatarListView)

    }
}