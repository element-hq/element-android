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
import com.airbnb.epoxy.EpoxyModelGroup
import com.airbnb.epoxy.ModelGroupHolder
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.riotredesign.R
import im.vector.riotredesign.core.epoxy.VectorEpoxyModel
import im.vector.riotredesign.features.home.AvatarRenderer

class RoomMemberMergedItem(val events: List<TimelineEvent>,
                           private val roomMemberItems: List<NoticeItem>,
                           private val visibilityStateChangedListener: VectorEpoxyModel.OnVisibilityStateChangedListener
) : EpoxyModelGroup(R.layout.item_timeline_event_room_member_merged, roomMemberItems) {

    private val distinctRoomMemberItems = roomMemberItems.distinctBy { it.userId }
    var isCollapsed = true
        set(value) {
            field = value
            updateModelVisibility()
        }

    init {
        updateModelVisibility()
    }

    override fun onVisibilityStateChanged(visibilityState: Int, view: ModelGroupHolder) {
        super.onVisibilityStateChanged(visibilityState, view)
        visibilityStateChangedListener.onVisibilityStateChanged(visibilityState)
    }

    override fun bind(holder: ModelGroupHolder) {
        super.bind(holder)
        val expandView = holder.rootView.findViewById<TextView>(R.id.itemMergedExpandTextView)
        val summaryView = holder.rootView.findViewById<TextView>(R.id.itemMergedSummaryTextView)
        val separatorView = holder.rootView.findViewById<View>(R.id.itemMergedSeparatorView)
        val avatarListView = holder.rootView.findViewById<ViewGroup>(R.id.itemMergedAvatarListView)
        if (isCollapsed) {
            val summary = holder.rootView.resources.getQuantityString(R.plurals.membership_changes, roomMemberItems.size, roomMemberItems.size)
            summaryView.text = summary
            summaryView.visibility = View.VISIBLE
            avatarListView.visibility = View.VISIBLE
            avatarListView.children.forEachIndexed { index, view ->
                val roomMemberItem = distinctRoomMemberItems.getOrNull(index)
                if (roomMemberItem != null && view is ImageView) {
                    view.visibility = View.VISIBLE
                    AvatarRenderer.render(roomMemberItem.avatarUrl, roomMemberItem.userId, roomMemberItem.memberName?.toString(), view)
                } else {
                    view.visibility = View.GONE
                }
            }
            separatorView.visibility = View.GONE
            expandView.setText(R.string.merged_events_expand)
        } else {
            avatarListView.visibility = View.INVISIBLE
            summaryView.visibility = View.GONE
            separatorView.visibility = View.VISIBLE
            expandView.setText(R.string.merged_events_collapse)
        }
        expandView.setOnClickListener { _ ->
            isCollapsed = !isCollapsed
            updateModelVisibility()
            bind(holder)
        }
    }

    private fun updateModelVisibility() {
        roomMemberItems.forEach {
            if (isCollapsed) {
                it.hide()
            } else {
                it.show()
            }
        }
    }

}