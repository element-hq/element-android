/*
 * Copyright 2019 New Vector Ltd
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
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.features.displayname.getBestName
import im.vector.app.features.home.AvatarRenderer
import org.matrix.android.sdk.api.util.MatrixItem

@EpoxyModelClass(layout = R.layout.item_thread_summary)
abstract class ThreadSummaryModel : VectorEpoxyModel<ThreadSummaryModel.Holder>() {

    @EpoxyAttribute lateinit var avatarRenderer: AvatarRenderer
    @EpoxyAttribute lateinit var matrixItem: MatrixItem
    @EpoxyAttribute lateinit var title: String
    @EpoxyAttribute lateinit var date: String
    @EpoxyAttribute lateinit var rootMessage: String
    @EpoxyAttribute lateinit var lastMessage: String
    @EpoxyAttribute lateinit var lastMessageCounter: String
    @EpoxyAttribute lateinit var lastMessageMatrixItem: MatrixItem

    override fun bind(holder: Holder) {
        super.bind(holder)
        avatarRenderer.render(matrixItem, holder.avatarImageView)
        holder.avatarImageView.contentDescription = matrixItem.getBestName()
        holder.titleTextView.text = title
        holder.dateTextView.text = date
        holder.rootMessageTextView.text = rootMessage

        // Last message summary
        avatarRenderer.render(lastMessageMatrixItem, holder.lastMessageAvatarImageView)
        holder.lastMessageAvatarImageView.contentDescription = lastMessageMatrixItem.getBestName()
        holder.lastMessageTextView.text = lastMessage
        holder.lastMessageCounterTextView.text = lastMessageCounter
    }

    class Holder : VectorEpoxyHolder() {
        val avatarImageView by bind<ImageView>(R.id.threadSummaryAvatarImageView)
        val titleTextView by bind<TextView>(R.id.threadSummaryTitleTextView)
        val dateTextView by bind<TextView>(R.id.threadSummaryDateTextView)
        val rootMessageTextView by bind<TextView>(R.id.threadSummaryRootMessageTextView)
        val lastMessageAvatarImageView by bind<ImageView>(R.id.messageThreadSummaryAvatarImageView)
        val lastMessageCounterTextView by bind<TextView>(R.id.messageThreadSummaryCounterTextView)
        val lastMessageTextView by bind<TextView>(R.id.messageThreadSummaryInfoTextView)
    }
}
