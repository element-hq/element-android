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

package im.vector.riotredesign.features.home.room.detail.timeline

import android.widget.ImageView
import android.widget.TextView
import im.vector.riotredesign.R
import im.vector.riotredesign.features.media.MediaContentRenderer

class MessageImageItem(
        private val mediaData: MediaContentRenderer.Data,
        informationData: MessageInformationData
) : AbsMessageItem(informationData, R.layout.item_timeline_event_image_message) {

    override val avatarImageView by bind<ImageView>(R.id.messageAvatarImageView)
    override val memberNameView by bind<TextView>(R.id.messageMemberNameView)
    override val timeView by bind<TextView>(R.id.messageTimeView)
    private val imageView by bind<ImageView>(R.id.messageImageView)

    override fun bind() {
        super.bind()
        MediaContentRenderer.render(mediaData, MediaContentRenderer.Mode.THUMBNAIL, imageView)
    }


}