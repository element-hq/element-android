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
 *
 */
package im.vector.riotx.core.epoxy.bottomsheet

import android.widget.ImageView
import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.riotx.R
import im.vector.riotx.core.epoxy.VectorEpoxyHolder
import im.vector.riotx.core.epoxy.VectorEpoxyModel
import im.vector.riotx.core.extensions.setTextOrHide
import im.vector.riotx.features.home.AvatarRenderer
import im.vector.riotx.features.home.room.detail.timeline.TimelineEventController
import im.vector.riotx.features.home.room.detail.timeline.tools.createLinkMovementMethod
import im.vector.riotx.features.home.room.detail.timeline.tools.findPillsAndProcess

/**
 * A message preview for bottom sheet.
 */
@EpoxyModelClass(layout = R.layout.item_bottom_sheet_message_preview)
abstract class BottomSheetItemMessagePreview : VectorEpoxyModel<BottomSheetItemMessagePreview.Holder>() {

    @EpoxyAttribute
    lateinit var avatarRenderer: AvatarRenderer
    @EpoxyAttribute
    lateinit var avatarUrl: String
    @EpoxyAttribute
    lateinit var senderId: String
    @EpoxyAttribute
    var senderName: String? = null
    @EpoxyAttribute
    lateinit var body: CharSequence
    @EpoxyAttribute
    var time: CharSequence? = null
    @EpoxyAttribute
    var urlClickCallback: TimelineEventController.UrlClickCallback? = null

    override fun bind(holder: Holder) {
        avatarRenderer.render(avatarUrl, senderId, senderName, holder.avatar)
        holder.sender.setTextOrHide(senderName)
        holder.body.movementMethod = createLinkMovementMethod(urlClickCallback)
        holder.body.text = body
        body.findPillsAndProcess { it.bind(holder.body) }
        holder.timestamp.setTextOrHide(time)
    }

    class Holder : VectorEpoxyHolder() {
        val avatar by bind<ImageView>(R.id.bottom_sheet_message_preview_avatar)
        val sender by bind<TextView>(R.id.bottom_sheet_message_preview_sender)
        val body by bind<TextView>(R.id.bottom_sheet_message_preview_body)
        val timestamp by bind<TextView>(R.id.bottom_sheet_message_preview_timestamp)
    }
}
