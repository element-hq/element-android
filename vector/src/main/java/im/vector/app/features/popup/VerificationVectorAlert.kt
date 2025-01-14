/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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

class VerificationVectorAlert(
        uid: String,
        title: String,
        override val description: String,
        @DrawableRes override val iconId: Int?,
        override val priority: Int = PopupAlertManager.DEFAULT_PRIORITY,
        /**
         * Alert are displayed by default, but let this lambda return false to prevent displaying.
         */
        override val shouldBeDisplayedIn: ((Activity) -> Boolean) = { true }
) : DefaultVectorAlert(uid, title, description, iconId, shouldBeDisplayedIn) {
    override val layoutRes = R.layout.alerter_verification_layout

    class ViewBinder(
            private val matrixItem: MatrixItem,
            private val avatarRenderer: AvatarRenderer
    ) : VectorAlert.ViewBinder {

        override fun bind(view: View) {
            val views = AlerterVerificationLayoutBinding.bind(view)
            avatarRenderer.render(matrixItem, views.ivUserAvatar, GlideApp.with(view.context.applicationContext))
        }
    }
}
