/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.popup

import android.app.Activity
import android.view.View
import im.vector.app.R
import im.vector.app.core.extensions.setLeftDrawable
import im.vector.app.core.glide.GlideApp
import im.vector.app.databinding.AlerterIncomingCallLayoutBinding
import im.vector.app.features.displayname.getBestName
import im.vector.app.features.home.AvatarRenderer
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.util.MatrixItem

class IncomingCallAlert(
        uid: String,
        override val shouldBeDisplayedIn: ((Activity) -> Boolean) = { true }
) : DefaultVectorAlert(uid, "", "", 0, shouldBeDisplayedIn) {

    override val priority = PopupAlertManager.INCOMING_CALL_PRIORITY
    override val layoutRes = R.layout.alerter_incoming_call_layout
    override var colorAttribute: Int? = com.google.android.material.R.attr.colorSurface
    override val dismissOnClick: Boolean = false
    override val isLight: Boolean = true

    class ViewBinder(
            private val matrixItem: MatrixItem?,
            private val avatarRenderer: AvatarRenderer,
            private val isVideoCall: Boolean,
            private val onAccept: () -> Unit,
            private val onReject: () -> Unit
    ) : VectorAlert.ViewBinder {

        override fun bind(view: View) {
            val views = AlerterIncomingCallLayoutBinding.bind(view)
            val (callKindText, callKindIcon, callKindActionIcon) = if (isVideoCall) {
                Triple(CommonStrings.action_video_call, R.drawable.ic_call_video_small, R.drawable.ic_call_answer_video)
            } else {
                Triple(CommonStrings.action_voice_call, R.drawable.ic_call_audio_small, R.drawable.ic_call_answer)
            }
            views.incomingCallKindView.setText(callKindText)
            views.incomingCallKindView.setLeftDrawable(callKindIcon)
            views.incomingCallNameView.text = matrixItem?.getBestName()
            matrixItem?.let { avatarRenderer.render(it, views.incomingCallAvatar, GlideApp.with(view.context.applicationContext)) }
            views.incomingCallAcceptView.setOnClickListener {
                onAccept()
            }
            views.incomingCallAcceptView.setImageResource(callKindActionIcon)
            views.incomingCallRejectView.setOnClickListener {
                onReject()
            }
        }
    }
}
