/*
 *
 *  * Copyright 2019 New Vector Ltd
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package im.vector.riotredesign.features.home.room.detail.timeline

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import im.vector.matrix.android.api.permalinks.MatrixLinkify
import im.vector.riotredesign.R
import im.vector.riotredesign.core.epoxy.KotlinModel
import im.vector.riotredesign.features.home.AvatarRenderer

class MessageItem(
        val message: CharSequence? = null,
        val time: CharSequence? = null,
        val avatarUrl: String?,
        val memberName: CharSequence? = null,
        val showInformation: Boolean = true
) : KotlinModel(R.layout.item_timeline_event_message) {

    private val avatarImageView by bind<ImageView>(R.id.messageAvatarImageView)
    private val memberNameView by bind<TextView>(R.id.messageMemberNameView)
    private val timeView by bind<TextView>(R.id.messageTimeView)
    private val messageView by bind<TextView>(R.id.messageTextView)

    override fun bind() {
        messageView.text = message
        MatrixLinkify.addLinkMovementMethod(messageView)
        if (showInformation) {
            avatarImageView.visibility = View.VISIBLE
            memberNameView.visibility = View.VISIBLE
            timeView.visibility = View.VISIBLE
            timeView.text = time
            memberNameView.text = memberName
            AvatarRenderer.render(avatarUrl, memberName?.toString(), avatarImageView)
        } else {
            avatarImageView.visibility = View.GONE
            memberNameView.visibility = View.GONE
            timeView.visibility = View.GONE
        }
    }
}