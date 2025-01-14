/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.epoxy

import android.widget.Button
import android.widget.TextView
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R

@EpoxyModelClass
abstract class ErrorWithRetryItem : VectorEpoxyModel<ErrorWithRetryItem.Holder>(R.layout.item_error_retry) {

    @EpoxyAttribute
    var text: String? = null

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var listener: ClickListener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.textView.text = text
        holder.buttonView.isVisible = listener != null
        holder.buttonView.onClick(listener)
    }

    class Holder : VectorEpoxyHolder() {
        val textView by bind<TextView>(R.id.itemErrorRetryText)
        val buttonView by bind<Button>(R.id.itemErrorRetryButton)
    }
}
