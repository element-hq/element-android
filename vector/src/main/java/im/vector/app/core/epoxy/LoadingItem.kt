/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.epoxy

import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.extensions.setTextOrHide

@EpoxyModelClass
abstract class LoadingItem : VectorEpoxyModel<LoadingItem.Holder>(R.layout.item_loading) {

    @EpoxyAttribute var loadingText: String? = null
    @EpoxyAttribute var showLoader: Boolean = true

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.progressBar.isVisible = showLoader
        holder.textView.setTextOrHide(loadingText)
    }

    class Holder : VectorEpoxyHolder() {
        val textView by bind<TextView>(R.id.loadingText)
        val progressBar by bind<ProgressBar>(R.id.loadingProgress)
    }
}
