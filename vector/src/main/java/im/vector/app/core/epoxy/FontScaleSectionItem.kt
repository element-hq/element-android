/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.epoxy

import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R

@EpoxyModelClass
abstract class FontScaleSectionItem : VectorEpoxyModel<FontScaleSectionItem.Holder>(R.layout.item_font_scale_section) {

    @EpoxyAttribute var sectionName: String = ""

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.textView.text = sectionName
    }

    class Holder : VectorEpoxyHolder() {
        val textView by bind<TextView>(R.id.font_scale_section_name)
    }
}
