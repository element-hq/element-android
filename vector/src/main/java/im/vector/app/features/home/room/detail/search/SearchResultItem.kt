/*
 * Copyright 2020 New Vector Ltd
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

package im.vector.app.features.home.room.detail.search

import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.features.displayname.getBestName
import im.vector.app.features.home.AvatarRenderer
import im.vector.lib.core.utils.epoxy.charsequence.EpoxyCharSequence
import org.matrix.android.sdk.api.session.threads.ThreadDetails
import org.matrix.android.sdk.api.util.MatrixItem

@EpoxyModelClass(layout = R.layout.item_search_result)
abstract class SearchResultItem : VectorEpoxyModel<SearchResultItem.Holder>() {

    @EpoxyAttribute lateinit var avatarRenderer: AvatarRenderer
    @EpoxyAttribute var formattedDate: String? = null
    @EpoxyAttribute lateinit var spannable: EpoxyCharSequence
    @EpoxyAttribute var sender: MatrixItem? = null
    @EpoxyAttribute var threadDetails: ThreadDetails? = null
    @EpoxyAttribute var threadSummaryFormatted: String? = null
    @EpoxyAttribute var areThreadMessagesEnabled: Boolean = false

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) var listener: ClickListener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)

        holder.view.onClick(listener)
        sender?.let { avatarRenderer.render(it, holder.avatarImageView) }
        holder.memberNameView.setTextOrHide(sender?.getBestName())
        holder.timeView.text = formattedDate
        holder.contentView.text = spannable.charSequence

        if (areThreadMessagesEnabled) {
            threadDetails?.let {
                if (it.isRootThread) {
                    showThreadSummary(holder)
                    holder.threadSummaryCounterTextView.text = it.numberOfThreads.toString()
                    holder.threadSummaryInfoTextView.text = threadSummaryFormatted.orEmpty()
                    val userId = it.threadSummarySenderInfo?.userId ?: return@let
                    val displayName = it.threadSummarySenderInfo?.displayName
                    val avatarUrl = it.threadSummarySenderInfo?.avatarUrl
                    avatarRenderer.render(MatrixItem.UserItem(userId, displayName, avatarUrl), holder.threadSummaryAvatarImageView)
                } else {
                    showFromThread(holder)
                }
            } ?: run {
                holder.threadSummaryConstraintLayout.isVisible = false
                holder.fromThreadConstraintLayout.isVisible = false
            }
        }
    }

    private fun showThreadSummary(holder: Holder, show: Boolean = true) {
        holder.threadSummaryConstraintLayout.isVisible = show
        holder.fromThreadConstraintLayout.isVisible = !show
    }

    private fun showFromThread(holder: Holder, show: Boolean = true) {
        holder.threadSummaryConstraintLayout.isVisible = !show
        holder.fromThreadConstraintLayout.isVisible = show
    }

    class Holder : VectorEpoxyHolder() {
        val avatarImageView by bind<ImageView>(R.id.messageAvatarImageView)
        val memberNameView by bind<TextView>(R.id.messageMemberNameView)
        val timeView by bind<TextView>(R.id.messageTimeView)
        val contentView by bind<TextView>(R.id.messageContentView)
        val threadSummaryConstraintLayout by bind<ConstraintLayout>(R.id.searchThreadSummaryConstraintLayout)
        val threadSummaryCounterTextView by bind<TextView>(R.id.messageThreadSummaryCounterTextView)
        val threadSummaryAvatarImageView by bind<ImageView>(R.id.messageThreadSummaryAvatarImageView)
        val threadSummaryInfoTextView by bind<TextView>(R.id.messageThreadSummaryInfoTextView)
        val fromThreadConstraintLayout by bind<ConstraintLayout>(R.id.searchFromThreadConstraintLayout)
    }
}
