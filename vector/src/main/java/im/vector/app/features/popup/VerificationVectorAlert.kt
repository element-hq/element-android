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
import androidx.annotation.DrawableRes
import im.vector.app.R
import im.vector.app.core.glide.GlideApp
import im.vector.app.databinding.AlerterVerificationLayoutBinding
import im.vector.app.features.home.AvatarRenderer
import org.matrix.android.sdk.api.util.MatrixItem

class VerificationVectorAlert(uid: String,
                              title: String,
                              override val description: String,
                              @DrawableRes override val iconId: Int?,
                              /**
                               * Alert are displayed by default, but let this lambda return false to prevent displaying
                               */
                              override val shouldBeDisplayedIn: ((Activity) -> Boolean) = { true }
) : DefaultVectorAlert(uid, title, description, iconId, shouldBeDisplayedIn) {
    override val layoutRes = R.layout.alerter_verification_layout

    class ViewBinder(private val matrixItem: MatrixItem?,
                     private val avatarRenderer: AvatarRenderer) : VectorAlert.ViewBinder {

        override fun bind(view: View) {
            val views = AlerterVerificationLayoutBinding.bind(view)
            matrixItem?.let { avatarRenderer.render(it, views.ivUserAvatar, GlideApp.with(view.context.applicationContext)) }
        }
    }
}
