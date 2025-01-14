/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.epoxy.profiles

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.ui.views.PresenceStateImageView
import im.vector.app.core.ui.views.ShieldImageView

@EpoxyModelClass
abstract class ProfileMatrixItem : BaseProfileMatrixItem<ProfileMatrixItem.Holder>(R.layout.item_profile_matrix_item) {

    open class Holder : VectorEpoxyHolder() {
        val titleView by bind<TextView>(R.id.matrixItemTitle)
        val subtitleView by bind<TextView>(R.id.matrixItemSubtitle)
        val ignoredUserView by bind<ImageView>(R.id.matrixItemIgnored)
        val powerLabel by bind<TextView>(R.id.matrixItemPowerLevelLabel)
        val presenceImageView by bind<PresenceStateImageView>(R.id.matrixItemPresenceImageView)
        val avatarImageView by bind<ImageView>(R.id.matrixItemAvatar)
        val avatarDecorationImageView by bind<ShieldImageView>(R.id.matrixItemAvatarDecoration)
        val editableView by bind<View>(R.id.matrixItemEditable)
    }
}
