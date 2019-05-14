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

package im.vector.riotredesign.features.home.room.detail.timeline.item

import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.helper.widget.Flow
import androidx.core.view.children
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import im.vector.riotredesign.R
import im.vector.riotredesign.core.utils.DimensionUtils.dpToPx
import im.vector.riotredesign.features.home.AvatarRenderer
import im.vector.riotredesign.features.reactions.widget.ReactionButton


abstract class AbsMessageItem<H : AbsMessageItem.Holder> : BaseEventItem<H>() {

    abstract val informationData: MessageInformationData

    @EpoxyAttribute
    var longClickListener: View.OnLongClickListener? = null

    @EpoxyAttribute
    var cellClickListener: View.OnClickListener? = null

    @EpoxyAttribute
    var avatarClickListener: View.OnClickListener? = null

    @EpoxyAttribute
    var memberClickListener: View.OnClickListener? = null

    override fun bind(holder: H) {
        super.bind(holder)
        if (informationData.showInformation) {

            holder.avatarImageView.layoutParams = holder.avatarImageView.layoutParams?.apply {
                val size = dpToPx(avatarStyle.avatarSizeDP, holder.view.context)
                height = size
                width = size
            }
            holder.avatarImageView.visibility = View.VISIBLE
            holder.avatarImageView.setOnClickListener(avatarClickListener)
            holder.memberNameView.visibility = View.VISIBLE
            holder.memberNameView.setOnClickListener(memberClickListener)
            holder.timeView.visibility = View.VISIBLE
            holder.timeView.text = informationData.time
            holder.memberNameView.text = informationData.memberName
            AvatarRenderer.render(informationData.avatarUrl, informationData.senderId, informationData.memberName?.toString(), holder.avatarImageView)
        } else {
            holder.avatarImageView.setOnClickListener(null)
            holder.memberNameView.setOnClickListener(null)
            holder.avatarImageView.visibility = View.GONE
            holder.memberNameView.visibility = View.GONE
            holder.timeView.visibility = View.GONE
        }
        holder.view.setOnClickListener(cellClickListener)
        holder.view.setOnLongClickListener(longClickListener)

        if (informationData.orderedReactionList.isNullOrEmpty()) {
            holder.reactionWrapper.isVisible = false
        } else {
            holder.reactionWrapper.isVisible = true
            //clear all reaction buttons (but not the Flow helper!)
            holder.reactionWrapper.children.forEach { (it as? ReactionButton)?.isGone = true }
            val idToRefInFlow = ArrayList<Int>()
            informationData.orderedReactionList?.forEachIndexed { index, reaction ->
                (holder.reactionWrapper.children.elementAt(index) as? ReactionButton)?.let { reactionButton ->
                    reactionButton.isVisible = true
                    idToRefInFlow.add(reactionButton.id)
                    reactionButton.reactionString = reaction.first
                    reactionButton.reactionCount = reaction.second
                    reactionButton.setChecked(reaction.third)
                }
            }
            // Just setting the view as gone will break the FlowHelper (and invisible will take too much space),
            // so have to update ref ids
            holder.reactionFlowHelper.referencedIds = idToRefInFlow.toIntArray()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && !holder.view.isInLayout) {
                holder.reactionFlowHelper.requestLayout()
            }

        }
    }

    protected fun View.renderSendState() {
        isClickable = informationData.sendState.isSent()
        alpha = if (informationData.sendState.isSent()) 1f else 0.5f
    }

    abstract class Holder : BaseHolder() {

        val avatarImageView by bind<ImageView>(R.id.messageAvatarImageView)
        val memberNameView by bind<TextView>(R.id.messageMemberNameView)
        val timeView by bind<TextView>(R.id.messageTimeView)

        val reactionWrapper: ViewGroup by bind(R.id.messageBottomInfo)
        val reactionFlowHelper: Flow by bind(R.id.reactionsFlowHelper)
    }

}