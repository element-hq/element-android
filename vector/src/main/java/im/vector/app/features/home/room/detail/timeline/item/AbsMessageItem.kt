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

package im.vector.app.features.home.room.detail.timeline.item

import android.graphics.Typeface
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.IdRes
import com.airbnb.epoxy.EpoxyAttribute
import im.vector.app.R
import im.vector.app.core.utils.DebouncedClickListener
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.timeline.MessageColorProvider
import im.vector.app.features.home.room.detail.timeline.TimelineEventController

/**
 * Base timeline item that adds an optional information bar with the sender avatar, name and time
 * Adds associated click listeners (on avatar, displayname)
 */
abstract class AbsMessageItem<H : AbsMessageItem.Holder> : AbsBaseMessageItem<H>() {

    override val baseAttributes: AbsBaseMessageItem.Attributes
        get() = attributes

    @EpoxyAttribute
    lateinit var attributes: Attributes

    private val _avatarClickListener = DebouncedClickListener(View.OnClickListener {
        attributes.avatarCallback?.onAvatarClicked(attributes.informationData)
    })
    private val _memberNameClickListener = DebouncedClickListener(View.OnClickListener {
        attributes.avatarCallback?.onMemberNameClicked(attributes.informationData)
    })

    override fun bind(holder: H) {
        super.bind(holder)
        if (attributes.informationData.showInformation) {
            holder.avatarImageView.layoutParams = holder.avatarImageView.layoutParams?.apply {
                height = attributes.avatarSize
                width = attributes.avatarSize
            }
            holder.avatarImageView.visibility = View.VISIBLE
            holder.avatarImageView.setOnClickListener(_avatarClickListener)
            holder.memberNameView.visibility = View.VISIBLE
            holder.memberNameView.setOnClickListener(_memberNameClickListener)
            holder.timeView.visibility = View.VISIBLE
            holder.timeView.text = attributes.informationData.time
            holder.memberNameView.text = attributes.informationData.memberName
            holder.memberNameView.setTextColor(attributes.getMemberNameColor())
            attributes.avatarRenderer.render(attributes.informationData.matrixItem, holder.avatarImageView)
            holder.avatarImageView.setOnLongClickListener(attributes.itemLongClickListener)
            holder.memberNameView.setOnLongClickListener(attributes.itemLongClickListener)
        } else {
            holder.avatarImageView.setOnClickListener(null)
            holder.memberNameView.setOnClickListener(null)
            holder.avatarImageView.visibility = View.GONE
            holder.memberNameView.visibility = View.GONE
            holder.timeView.visibility = View.GONE
            holder.avatarImageView.setOnLongClickListener(null)
            holder.memberNameView.setOnLongClickListener(null)
        }
    }

    override fun unbind(holder: H) {
        holder.avatarImageView.setOnClickListener(null)
        holder.avatarImageView.setOnLongClickListener(null)
        holder.memberNameView.setOnClickListener(null)
        holder.memberNameView.setOnLongClickListener(null)
        super.unbind(holder)
    }

    private fun Attributes.getMemberNameColor() = messageColorProvider.getMemberNameTextColor(informationData.senderId)

    abstract class Holder(@IdRes stubId: Int) : AbsBaseMessageItem.Holder(stubId) {
        val avatarImageView by bind<ImageView>(R.id.messageAvatarImageView)
        val memberNameView by bind<TextView>(R.id.messageMemberNameView)
        val timeView by bind<TextView>(R.id.messageTimeView)
    }

    /**
     * This class holds all the common attributes for timeline items.
     */
    data class Attributes(
            val avatarSize: Int,
            override val informationData: MessageInformationData,
            override val avatarRenderer: AvatarRenderer,
            override val messageColorProvider: MessageColorProvider,
            override val itemLongClickListener: View.OnLongClickListener? = null,
            override val itemClickListener: View.OnClickListener? = null,
            val memberClickListener: View.OnClickListener? = null,
            override val reactionPillCallback: TimelineEventController.ReactionPillCallback? = null,
            val avatarCallback: TimelineEventController.AvatarCallback? = null,
            override val readReceiptsCallback: TimelineEventController.ReadReceiptsCallback? = null,
            val emojiTypeFace: Typeface? = null
    ) : AbsBaseMessageItem.Attributes {

        // Have to override as it's used to diff epoxy items
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Attributes

            if (avatarSize != other.avatarSize) return false
            if (informationData != other.informationData) return false

            return true
        }

        override fun hashCode(): Int {
            var result = avatarSize
            result = 31 * result + informationData.hashCode()
            return result
        }
    }
}
