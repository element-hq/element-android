/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.core.ui.bottomsheet

import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.extensions.setTextOrHide

/**
 * A title for bottom sheet, with an optional subtitle. It does not include the bottom separator.
 */
@EpoxyModelClass
abstract class BottomSheetTitleItem : VectorEpoxyModel<BottomSheetTitleItem.Holder>(R.layout.item_bottom_sheet_title) {

    @EpoxyAttribute
    lateinit var title: String

    @EpoxyAttribute
    var subTitle: String? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.title.text = title
        holder.subtitle.setTextOrHide(subTitle)
    }

    class Holder : VectorEpoxyHolder() {
        val title by bind<TextView>(R.id.itemBottomSheetTitleTitle)
        val subtitle by bind<TextView>(R.id.itemBottomSheetTitleSubtitle)
    }
}
