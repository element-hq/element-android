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
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.riotredesign.R
import im.vector.riotredesign.features.home.AvatarRenderer

@EpoxyModelClass(layout = R.layout.item_timeline_event_base_noinfo)
abstract class NoticeItem : BaseEventItem<NoticeItem.Holder>() {

    @EpoxyAttribute
    var noticeText: CharSequence? = null
    @EpoxyAttribute
    var avatarUrl: String? = null
    @EpoxyAttribute
    var userId: String = ""
    @EpoxyAttribute
    var memberName: CharSequence? = null


    @EpoxyAttribute
    var longClickListener: View.OnLongClickListener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.noticeTextView.text = noticeText
        AvatarRenderer.render(avatarUrl, userId, memberName?.toString(), holder.avatarImageView)
        holder.view.setOnLongClickListener(longClickListener)
    }

    override fun getStubType(): Int = STUB_ID

    class Holder : BaseHolder() {
        override fun getStubId(): Int = STUB_ID

        val avatarImageView by bind<ImageView>(R.id.itemNoticeAvatarView)
        val noticeTextView by bind<TextView>(R.id.itemNoticeTextView)
    }

    companion object {
        private const val STUB_ID = R.id.messageContentNoticeStub
    }
}