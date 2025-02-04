/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.epoxy.profiles.notifications

import android.widget.TextView
import androidx.annotation.StringRes
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel

@EpoxyModelClass
abstract class TextHeaderItem : VectorEpoxyModel<TextHeaderItem.Holder>(R.layout.item_text_header) {

    @EpoxyAttribute
    var text: String? = null

    @StringRes
    @EpoxyAttribute
    var textRes: Int? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        val textResource = textRes
        if (textResource != null) {
            holder.textView.setText(textResource)
        } else {
            holder.textView.text = text
        }
    }

    class Holder : VectorEpoxyHolder() {
        val textView by bind<TextView>(R.id.headerText)
    }
}
