/*
 * Copyright 2021 New Vector Ltd
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

package im.vector.app.features.home.room.threads.list.model

import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.extensions.clearDrawables
import im.vector.app.core.extensions.setLeftDrawable
import im.vector.app.core.utils.DimensionConverter
import im.vector.app.features.displayname.getBestName
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.themes.ThemeUtils
import org.matrix.android.sdk.api.session.threads.ThreadNotificationState
import org.matrix.android.sdk.api.util.MatrixItem

@EpoxyModelClass(layout = R.layout.item_thread)
abstract class ThreadListItem : VectorEpoxyModel<ThreadListItem.Holder>() {

    @EpoxyAttribute lateinit var avatarRenderer: AvatarRenderer
    @EpoxyAttribute lateinit var matrixItem: MatrixItem
    @EpoxyAttribute lateinit var title: String
    @EpoxyAttribute lateinit var date: String
    @EpoxyAttribute lateinit var rootMessage: String
    @EpoxyAttribute lateinit var lastMessage: String
    @EpoxyAttribute var threadNotificationState: ThreadNotificationState = ThreadNotificationState.NO_NEW_MESSAGE
    @EpoxyAttribute lateinit var lastMessageCounter: String
    @EpoxyAttribute var rootMessageDeleted: Boolean = false
    @EpoxyAttribute var lastMessageMatrixItem: MatrixItem? = null
    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash) var itemClickListener: ClickListener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.rootView.onClick(itemClickListener)
        avatarRenderer.render(matrixItem, holder.avatarImageView)
        holder.avatarImageView.contentDescription = matrixItem.getBestName()
        holder.titleTextView.text = title
        holder.dateTextView.text = date
        if (rootMessageDeleted) {
            holder.rootMessageTextView.text = holder.view.context.getString(R.string.event_redacted)
            holder.rootMessageTextView.setTextColor(ThemeUtils.getColor(holder.view.context, R.attr.vctr_content_secondary))
            holder.rootMessageTextView.setLeftDrawable(R.drawable.ic_trash_16, R.attr.vctr_content_tertiary)
            holder.rootMessageTextView.compoundDrawablePadding = DimensionConverter(holder.view.context.resources).dpToPx(10)
        } else {
            holder.rootMessageTextView.setTextColor(ThemeUtils.getColor(holder.view.context, R.attr.vctr_content_primary))
            holder.rootMessageTextView.text = rootMessage
            holder.rootMessageTextView.clearDrawables()
        }
        // Last message summary
        lastMessageMatrixItem?.let {
            avatarRenderer.render(it, holder.lastMessageAvatarImageView)
        }
        holder.lastMessageAvatarImageView.contentDescription = lastMessageMatrixItem?.getBestName()
        holder.lastMessageTextView.text = lastMessage
        holder.lastMessageCounterTextView.text = lastMessageCounter
        renderNotificationState(holder)
    }

    private fun renderNotificationState(holder: Holder) {
        when (threadNotificationState) {
            ThreadNotificationState.NEW_MESSAGE             -> {
                holder.unreadImageView.isVisible = true
                holder.unreadImageView.setColorFilter(ContextCompat.getColor(holder.view.context, R.color.palette_gray_200))
            }
            ThreadNotificationState.NEW_HIGHLIGHTED_MESSAGE -> {
                holder.unreadImageView.isVisible = true
                holder.unreadImageView.setColorFilter(ContextCompat.getColor(holder.view.context, R.color.palette_vermilion))
            }
            else                                            -> {
                holder.unreadImageView.isVisible = false
            }
        }
    }

    class Holder : VectorEpoxyHolder() {
        val avatarImageView by bind<ImageView>(R.id.threadSummaryAvatarImageView)
        val titleTextView by bind<TextView>(R.id.threadSummaryTitleTextView)
        val dateTextView by bind<TextView>(R.id.threadSummaryDateTextView)
        val rootMessageTextView by bind<TextView>(R.id.threadSummaryRootMessageTextView)
        val lastMessageAvatarImageView by bind<ImageView>(R.id.messageThreadSummaryAvatarImageView)
        val lastMessageCounterTextView by bind<TextView>(R.id.messageThreadSummaryCounterTextView)
        val lastMessageTextView by bind<TextView>(R.id.messageThreadSummaryInfoTextView)
        val unreadImageView by bind<ImageView>(R.id.threadSummaryUnreadImageView)
        val rootView by bind<ConstraintLayout>(R.id.threadSummaryRootConstraintLayout)
    }
}
