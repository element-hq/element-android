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

import android.view.View
import android.widget.TextView
import androidx.annotation.IdRes
import im.vector.app.R
import im.vector.app.features.home.AvatarRenderer
import org.matrix.android.sdk.api.util.MatrixItem

abstract class BasedMergedItem<H : BasedMergedItem.Holder> : BaseEventItem<H>() {

    abstract val attributes: Attributes

    override fun bind(holder: H) {
        super.bind(holder)
        holder.expandView.setOnClickListener {
            attributes.onCollapsedStateChanged(!attributes.isCollapsed)
        }
        if (attributes.isCollapsed) {
            holder.separatorView.visibility = View.GONE
            holder.expandView.setText(R.string.merged_events_expand)
        } else {
            holder.separatorView.visibility = View.VISIBLE
            holder.expandView.setText(R.string.merged_events_collapse)
        }
    }

    protected val distinctMergeData by lazy {
        attributes.mergeData.distinctBy { it.userId }
    }

    override fun getEventIds(): List<String> {
        return if (attributes.isCollapsed) {
            attributes.mergeData.map { it.eventId }
        } else {
            emptyList()
        }
    }

    data class Data(
            val localId: Long,
            val eventId: String,
            val userId: String,
            val memberName: String,
            val avatarUrl: String?,
            val isDirectRoom: Boolean
    )

    fun Data.toMatrixItem() = MatrixItem.UserItem(userId, memberName, avatarUrl)

    interface Attributes {
        val isCollapsed: Boolean
        val mergeData: List<Data>
        val avatarRenderer: AvatarRenderer
        val onCollapsedStateChanged: (Boolean) -> Unit
    }

    abstract class Holder(@IdRes stubId: Int) : BaseEventItem.BaseHolder(stubId) {
        val expandView by bind<TextView>(R.id.itemMergedExpandTextView)
        val separatorView by bind<View>(R.id.itemMergedSeparatorView)
    }
}
