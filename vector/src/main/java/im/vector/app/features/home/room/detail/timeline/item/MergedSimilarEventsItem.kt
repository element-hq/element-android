/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.item

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.PluralsRes
import androidx.core.view.children
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.features.home.AvatarRenderer

@EpoxyModelClass
abstract class MergedSimilarEventsItem : BasedMergedItem<MergedSimilarEventsItem.Holder>(R.layout.item_timeline_event_base_noinfo) {

    override fun getViewStubId() = STUB_ID

    @EpoxyAttribute
    override lateinit var attributes: Attributes

    override fun bind(holder: Holder) {
        super.bind(holder)
        if (attributes.isCollapsed) {
            val summary = holder.expandView.resources.getQuantityString(attributes.summaryTitleResId, attributes.mergeData.size, attributes.mergeData.size)
            holder.summaryView.text = summary
            holder.summaryView.visibility = View.VISIBLE
            holder.avatarListView.visibility = View.VISIBLE
            holder.avatarListView.children.forEachIndexed { index, view ->
                val data = distinctMergeData.getOrNull(index)
                if (data != null && view is ImageView) {
                    view.visibility = View.VISIBLE
                    attributes.avatarRenderer.render(data.toMatrixItem(), view)
                } else {
                    view.visibility = View.GONE
                }
            }
        } else {
            holder.avatarListView.visibility = View.INVISIBLE
            holder.summaryView.visibility = View.GONE
        }
    }

    class Holder : BasedMergedItem.Holder(STUB_ID) {
        val summaryView by bind<TextView>(R.id.itemMergedSummaryTextView)
        val avatarListView by bind<ViewGroup>(R.id.itemMergedAvatarListView)
    }

    companion object {
        private val STUB_ID = R.id.messageContentMergedHeaderStub
    }

    data class Attributes(
            @PluralsRes val summaryTitleResId: Int,
            override val isCollapsed: Boolean,
            override val mergeData: List<Data>,
            override val avatarRenderer: AvatarRenderer,
            override val onCollapsedStateChanged: (Boolean) -> Unit
    ) : BasedMergedItem.Attributes
}
