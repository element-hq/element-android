/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.epoxy.profiles

import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.features.displayname.getBestName
import im.vector.app.features.home.AvatarRenderer
import org.matrix.android.sdk.api.session.crypto.model.UserVerificationLevel
import org.matrix.android.sdk.api.util.MatrixItem

abstract class BaseProfileMatrixItem<T : ProfileMatrixItem.Holder>(@LayoutRes layoutId: Int) : VectorEpoxyModel<T>(layoutId) {
    @EpoxyAttribute lateinit var avatarRenderer: AvatarRenderer
    @EpoxyAttribute lateinit var matrixItem: MatrixItem
    @EpoxyAttribute var editable: Boolean = true

    @EpoxyAttribute
    var userVerificationLevel: UserVerificationLevel? = null

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var clickListener: ClickListener? = null

    @CallSuper
    override fun bind(holder: T) {
        super.bind(holder)
        val bestName = matrixItem.getBestName()
        val matrixId = matrixItem.id
                .takeIf { it != bestName }
                // Special case for ThreePid fake matrix item
                .takeIf { it != "@" }
        holder.view.onClick(clickListener?.takeIf { editable })
        holder.titleView.text = bestName
        holder.subtitleView.setTextOrHide(matrixId)
        holder.editableView.isVisible = editable
        avatarRenderer.render(matrixItem, holder.avatarImageView)
        holder.avatarDecorationImageView.renderUser(userVerificationLevel)
    }
}
