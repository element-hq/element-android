/*
 * Copyright (c) 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.popup

import android.app.Activity
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import im.vector.app.R
import im.vector.app.core.extensions.setLeftDrawable
import im.vector.app.core.glide.GlideApp
import im.vector.app.features.home.AvatarRenderer
import org.matrix.android.sdk.api.util.MatrixItem

class IncomingCallAlert(uid: String,
                        override val shouldBeDisplayedIn: ((Activity) -> Boolean) = { true }
) : DefaultVectorAlert(uid, "", "", 0, shouldBeDisplayedIn) {

    override val priority = PopupAlertManager.INCOMING_CALL_PRIORITY
    override val layoutRes = R.layout.alerter_incoming_call_layout
    override var colorAttribute: Int? = R.attr.riotx_alerter_background
    override val dismissOnClick: Boolean = false
    override val isLight: Boolean = true

    class ViewBinder(private val matrixItem: MatrixItem?,
                     private val avatarRenderer: AvatarRenderer,
                     private val isVideoCall: Boolean,
                     private val onAccept: () -> Unit,
                     private val onReject: () -> Unit)
        : VectorAlert.ViewBinder {

        override fun bind(view: View) {
            val (callKindText, callKindIcon) = if (isVideoCall) {
                Pair(R.string.action_video_call, R.drawable.ic_call_video_small)
            } else {
                Pair(R.string.action_voice_call, R.drawable.ic_call_audio_small)
            }
            view.findViewById<TextView>(R.id.incomingCallKindView).apply {
                setText(callKindText)
                setLeftDrawable(callKindIcon)
            }
            view.findViewById<TextView>(R.id.incomingCallNameView).text = matrixItem?.getBestName()
            view.findViewById<ImageView>(R.id.incomingCallAvatar)?.let { imageView ->
                matrixItem?.let { avatarRenderer.render(it, imageView, GlideApp.with(view.context.applicationContext)) }
            }
            view.findViewById<ImageView>(R.id.incomingCallAcceptView).apply {
                setOnClickListener {
                    onAccept()
                }
                setImageResource(callKindIcon)
            }
            view.findViewById<ImageView>(R.id.incomingCallRejectView).setOnClickListener {
                onReject()
            }
        }
    }
}
