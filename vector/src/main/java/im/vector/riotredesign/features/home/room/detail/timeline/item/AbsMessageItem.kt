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

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import im.vector.riotredesign.R
import com.airbnb.epoxy.EpoxyAttribute
import com.jakewharton.rxbinding2.view.RxView
import im.vector.riotredesign.core.epoxy.VectorEpoxyHolder
import im.vector.riotredesign.core.epoxy.VectorEpoxyModel
import im.vector.riotredesign.features.home.AvatarRenderer


abstract class AbsMessageItem<H : AbsMessageItem.Holder> : AEventItemBase<H>() {

    abstract val informationData: MessageInformationData

    @EpoxyAttribute
    var longClickListener: View.OnLongClickListener? = null

    @EpoxyAttribute
    var cellClickListener: View.OnClickListener? = null

    @EpoxyAttribute
    var avatarClickListener: View.OnClickListener? = null

    override fun bind(holder: H) {
        super.bind(holder)
        if (informationData.showInformation) {

            val lp = holder.avatarImageView.layoutParams?.apply {
                val size = dpToPx(avatarStyle.avatarSizeDP, holder.view.context)
                height = size
                width = size
            }
            holder.avatarImageView.layoutParams = lp

            holder.avatarImageView.visibility = View.VISIBLE
            holder.avatarImageView.setOnClickListener(avatarClickListener)
            holder.memberNameView.visibility = View.VISIBLE
            holder.timeView.visibility = View.VISIBLE
            holder.timeView.text = informationData.time
            holder.memberNameView.text = informationData.memberName
            AvatarRenderer.render(informationData.avatarUrl, informationData.senderId, informationData.memberName?.toString(), holder.avatarImageView)
        } else {
            holder.avatarImageView.visibility = View.GONE
            holder.memberNameView.visibility = View.GONE
            holder.timeView.visibility = View.GONE
        }
        holder.view.setOnClickListener(cellClickListener)
        holder.view.setOnLongClickListener(longClickListener)

    }

    protected fun View.renderSendState() {
        isClickable = informationData.sendState.isSent()
        alpha = if (informationData.sendState.isSent()) 1f else 0.5f
    }

    abstract class Holder : BaseHolder() {

        val avatarImageView by bind<ImageView>(R.id.messageAvatarImageView)
        val memberNameView by bind<TextView>(R.id.messageMemberNameView)
        val timeView by bind<TextView>(R.id.messageTimeView)
    }

}