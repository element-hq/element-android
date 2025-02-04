/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.autocomplete

import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel

@EpoxyModelClass
abstract class AutocompleteHeaderItem : VectorEpoxyModel<AutocompleteHeaderItem.Holder>(R.layout.item_autocomplete_header_item) {

    @EpoxyAttribute var title: String? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.titleView.text = title
    }

    class Holder : VectorEpoxyHolder() {
        val titleView by bind<TextView>(R.id.headerItemAutocompleteTitle)
    }
}
