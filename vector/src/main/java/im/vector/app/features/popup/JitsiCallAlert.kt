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
import im.vector.app.R
import im.vector.app.core.glide.GlideApp
import im.vector.app.databinding.AlerterJitsiCallLayoutBinding
import im.vector.app.features.home.AvatarRenderer
import org.matrix.android.sdk.api.util.MatrixItem

class JitsiCallAlert(uid: String,
                     override val shouldBeDisplayedIn: ((Activity) -> Boolean) = { true }
) : DefaultVectorAlert(uid, "", "", 0, shouldBeDisplayedIn) {

    override val priority = PopupAlertManager.JITSI_CALL_PRIORITY
    override val layoutRes = R.layout.alerter_jitsi_call_layout
    override var colorAttribute: Int? = R.attr.colorSurface
    override val dismissOnClick: Boolean = false
    override val isLight: Boolean = true

    class ViewBinder(private val matrixItem: MatrixItem?,
                     private val avatarRenderer: AvatarRenderer,
                     private val onJoin: () -> Unit) : VectorAlert.ViewBinder {

        override fun bind(view: View) {
            val views = AlerterJitsiCallLayoutBinding.bind(view)
            views.jitsiCallNameView.text = matrixItem?.getBestName()
            matrixItem?.let { avatarRenderer.render(it, views.jitsiCallAvatar, GlideApp.with(view.context.applicationContext)) }
            views.jitsiCallJoinView.setOnClickListener {
                onJoin()
            }
        }
    }
}
