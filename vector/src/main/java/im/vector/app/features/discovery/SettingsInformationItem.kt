/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.discovery

import android.widget.TextView
import androidx.annotation.ColorInt
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel

@EpoxyModelClass
abstract class SettingsInformationItem : VectorEpoxyModel<SettingsInformationItem.Holder>(R.layout.item_settings_information) {

    @EpoxyAttribute
    lateinit var message: String

    @EpoxyAttribute
    @ColorInt
    var textColor: Int = 0

    override fun bind(holder: Holder) {
        super.bind(holder)

        holder.textView.text = message
        holder.textView.setTextColor(textColor)
    }

    class Holder : VectorEpoxyHolder() {
        val textView by bind<TextView>(R.id.settings_item_information)
    }
}
