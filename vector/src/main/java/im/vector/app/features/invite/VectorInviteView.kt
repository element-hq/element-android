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

package im.vector.app.features.invite

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.databinding.VectorInviteViewBinding
import im.vector.app.features.home.AvatarRenderer
import org.matrix.android.sdk.api.session.room.members.ChangeMembershipState
import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

@AndroidEntryPoint
class VectorInviteView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) :
    ConstraintLayout(context, attrs, defStyle) {

    interface Callback {
        fun onAcceptInvite()
        fun onRejectInvite()
    }

    enum class Mode {
        LARGE,
        SMALL
    }

    private val views: VectorInviteViewBinding

    @Inject lateinit var avatarRenderer: AvatarRenderer
    var callback: Callback? = null

    init {
        inflate(context, R.layout.vector_invite_view, this)
        views = VectorInviteViewBinding.bind(this)
        views.inviteAcceptView.commonClicked = { callback?.onAcceptInvite() }
        views.inviteRejectView.commonClicked = { callback?.onRejectInvite() }
    }

    fun render(sender: RoomMemberSummary, mode: Mode = Mode.LARGE, changeMembershipState: ChangeMembershipState) {
        if (mode == Mode.LARGE) {
            updateLayoutParams { height = LayoutParams.MATCH_CONSTRAINT }
            avatarRenderer.render(sender.toMatrixItem(), views.inviteAvatarView)
            views.inviteIdentifierView.text = sender.userId
            views.inviteNameView.text = sender.displayName
            views.inviteLabelView.text = context.getString(R.string.send_you_invite)
        } else {
            updateLayoutParams { height = LayoutParams.WRAP_CONTENT }
            views.inviteAvatarView.visibility = View.GONE
            views.inviteIdentifierView.visibility = View.GONE
            views.inviteNameView.visibility = View.GONE
            views.inviteLabelView.text = context.getString(R.string.invited_by, sender.userId)
        }
        InviteButtonStateBinder.bind(views.inviteAcceptView, views.inviteRejectView, changeMembershipState)
    }
}
