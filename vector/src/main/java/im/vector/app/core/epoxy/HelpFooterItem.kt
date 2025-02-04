/*
 * Copyright 2019-2024 New Vector Ltd.
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
abstract class HelpFooterItem : VectorEpoxyModel<HelpFooterItem.Holder>(R.layout.item_help_footer) {

    @EpoxyAttribute
    var text: String? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.textView.text = text
    }

    class Holder : VectorEpoxyHolder() {
        val textView by bind<TextView>(R.id.itemHelpText)
    }
}
