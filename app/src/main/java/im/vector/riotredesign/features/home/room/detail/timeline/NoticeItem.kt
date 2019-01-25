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
import im.vector.riotredesign.core.epoxy.KotlinModel
import im.vector.riotredesign.features.home.AvatarRenderer

class NoticeItem(private val noticeText: CharSequence? = null,
                 private val avatarUrl: String?,
                 private val memberName: CharSequence? = null)
    : KotlinModel(R.layout.item_timeline_event_notice) {

    private val avatarImageView by bind<ImageView>(R.id.itemNoticeAvatarView)
    private val noticeTextView by bind<TextView>(R.id.itemNoticeTextView)

    override fun bind() {
        noticeTextView.text = noticeText
        AvatarRenderer.render(avatarUrl, memberName?.toString(), avatarImageView)
    }
}