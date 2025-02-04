/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.item

import android.view.View
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import im.vector.app.R
import im.vector.app.features.home.AvatarRenderer
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.util.MatrixItem

abstract class BasedMergedItem<H : BasedMergedItem.Holder>(@LayoutRes layoutId: Int) : BaseEventItem<H>(layoutId) {

    abstract val attributes: Attributes

    override fun bind(holder: H) {
        super.bind(holder)
        holder.expandView.setOnClickListener {
            attributes.onCollapsedStateChanged(!attributes.isCollapsed)
        }
        if (attributes.isCollapsed) {
            holder.separatorView.visibility = View.GONE
            holder.expandView.setText(CommonStrings.merged_events_expand)
        } else {
            holder.separatorView.visibility = View.VISIBLE
            holder.expandView.setText(CommonStrings.merged_events_collapse)
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
            val roomId: String?,
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
