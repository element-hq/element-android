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
import im.vector.app.R
import im.vector.app.core.di.HasScreenInjector
import im.vector.app.core.platform.ButtonStateView
import im.vector.app.features.home.AvatarRenderer
import org.matrix.android.sdk.api.session.room.members.ChangeMembershipState
import org.matrix.android.sdk.api.session.user.model.User
import org.matrix.android.sdk.api.util.toMatrixItem
import kotlinx.android.synthetic.main.vector_invite_view.view.*
import javax.inject.Inject

class VectorInviteView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0)
    : ConstraintLayout(context, attrs, defStyle) {

    interface Callback {
        fun onAcceptInvite()
        fun onRejectInvite()
    }

    enum class Mode {
        LARGE,
        SMALL
    }

    @Inject lateinit var avatarRenderer: AvatarRenderer
    var callback: Callback? = null

    init {
        if (context is HasScreenInjector) {
            context.injector().inject(this)
        }
        View.inflate(context, R.layout.vector_invite_view, this)
        inviteAcceptView.callback = object : ButtonStateView.Callback {
            override fun onButtonClicked() {
                callback?.onAcceptInvite()
            }

            override fun onRetryClicked() {
                callback?.onAcceptInvite()
            }
        }

        inviteRejectView.callback = object : ButtonStateView.Callback {
            override fun onButtonClicked() {
                callback?.onRejectInvite()
            }

            override fun onRetryClicked() {
                callback?.onRejectInvite()
            }
        }
    }

    fun render(sender: User, mode: Mode = Mode.LARGE, changeMembershipState: ChangeMembershipState) {
        if (mode == Mode.LARGE) {
            updateLayoutParams { height = LayoutParams.MATCH_CONSTRAINT }
            avatarRenderer.render(sender.toMatrixItem(), inviteAvatarView)
            inviteIdentifierView.text = sender.userId
            inviteNameView.text = sender.displayName
            inviteLabelView.text = context.getString(R.string.send_you_invite)
        } else {
            updateLayoutParams { height = LayoutParams.WRAP_CONTENT }
            inviteAvatarView.visibility = View.GONE
            inviteIdentifierView.visibility = View.GONE
            inviteNameView.visibility = View.GONE
            inviteLabelView.text = context.getString(R.string.invited_by, sender.userId)
        }
        InviteButtonStateBinder.bind(inviteAcceptView, inviteRejectView, changeMembershipState)
    }
}
