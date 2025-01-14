/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.signout.soft.epoxy

import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.extensions.setTextOrHide

@EpoxyModelClass
abstract class LoginTitleSmallItem : VectorEpoxyModel<LoginTitleSmallItem.Holder>(R.layout.item_login_title_small) {

    @EpoxyAttribute var text: String? = null

    override fun bind(holder: Holder) {
        super.bind(holder)

        holder.textView.setTextOrHide(text)
    }

    class Holder : VectorEpoxyHolder() {
        val textView by bind<TextView>(R.id.itemLoginTitleSmallText)
    }
}
