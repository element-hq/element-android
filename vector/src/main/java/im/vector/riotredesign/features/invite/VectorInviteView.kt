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

package im.vector.riotredesign.features.invite

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import im.vector.matrix.android.api.session.room.model.RoomSummary
import im.vector.riotredesign.R
import im.vector.riotredesign.features.home.AvatarRenderer
import kotlinx.android.synthetic.main.vector_invite_view.view.*

class VectorInviteView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0)
    : ConstraintLayout(context, attrs, defStyle) {

    interface Callback {
        fun onAcceptInvite()
        fun onRejectInvite()
    }

    var callback: Callback? = null

    init {
        View.inflate(context, R.layout.vector_invite_view, this)
        layoutParams = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT)
        setBackgroundColor(Color.WHITE)
        inviteRejectView.setOnClickListener { callback?.onRejectInvite() }
        inviteAcceptView.setOnClickListener { callback?.onAcceptInvite() }
    }

    fun render(roomSummary: RoomSummary) {
        AvatarRenderer.render(roomSummary.avatarUrl, roomSummary.roomId, roomSummary.displayName, inviteAvatarView)
        inviteIdentifierView.text = roomSummary.lastMessage?.sender
        inviteNameView.text = roomSummary.displayName
    }
}