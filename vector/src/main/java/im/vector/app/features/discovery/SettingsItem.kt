/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.discovery

import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.google.android.material.switchmaterial.SwitchMaterial
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.extensions.setTextOrHide

@EpoxyModelClass
abstract class SettingsItem : VectorEpoxyModel<SettingsItem.Holder>(R.layout.item_settings_simple_item) {

    @EpoxyAttribute
    var title: String? = null

    @EpoxyAttribute
    @StringRes
    var titleResId: Int? = null

    @EpoxyAttribute
    @StringRes
    var descriptionResId: Int? = null

    @EpoxyAttribute
    var description: String? = null

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var itemClickListener: ClickListener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        if (titleResId != null) {
            holder.titleText.setText(titleResId!!)
        } else {
            holder.titleText.setTextOrHide(title)
        }

        if (descriptionResId != null) {
            holder.descriptionText.setText(descriptionResId!!)
        } else {
            holder.descriptionText.setTextOrHide(description)
        }

        holder.switchButton.isVisible = false

        holder.view.onClick(itemClickListener)
    }

    class Holder : VectorEpoxyHolder() {
        val titleText by bind<TextView>(R.id.settings_item_title)
        val descriptionText by bind<TextView>(R.id.settings_item_description)
        val switchButton by bind<SwitchMaterial>(R.id.settings_item_switch)
    }
}
