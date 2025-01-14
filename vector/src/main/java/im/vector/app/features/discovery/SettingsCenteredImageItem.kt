/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.discovery

import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel

@EpoxyModelClass
abstract class SettingsCenteredImageItem : VectorEpoxyModel<SettingsCenteredImageItem.Holder>(R.layout.item_settings_centered_image) {

    @EpoxyAttribute
    @DrawableRes
    var drawableRes: Int = 0

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.image.setImageResource(drawableRes)
    }

    class Holder : VectorEpoxyHolder() {
        val image by bind<ImageView>(R.id.itemSettingsImage)
    }
}
