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
import im.vector.riotredesign.core.epoxy.RiotEpoxyHolder
import im.vector.riotredesign.core.epoxy.RiotEpoxyModel
import im.vector.riotredesign.features.home.AvatarRenderer

abstract class AbsMessageItem<H : AbsMessageItem.Holder> : RiotEpoxyModel<H>() {

    abstract val informationData: MessageInformationData

    override fun bind(holder: H) {
        super.bind(holder)
        if (informationData.showInformation) {
            holder.avatarImageView.visibility = View.VISIBLE
            holder.memberNameView.visibility = View.VISIBLE
            holder.timeView.visibility = View.VISIBLE
            holder.timeView.text = informationData.time
            holder.memberNameView.text = informationData.memberName
            AvatarRenderer.render(informationData.avatarUrl, informationData.memberName?.toString(), holder.avatarImageView)
        } else {
            holder.avatarImageView.visibility = View.GONE
            holder.memberNameView.visibility = View.GONE
            holder.timeView.visibility = View.GONE
        }
    }

    abstract class Holder : RiotEpoxyHolder() {
        abstract val avatarImageView: ImageView
        abstract val memberNameView: TextView
        abstract val timeView: TextView
    }

}