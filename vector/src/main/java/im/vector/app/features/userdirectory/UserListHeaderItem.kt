/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.userdirectory

import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel

@EpoxyModelClass
abstract class UserListHeaderItem : VectorEpoxyModel<UserListHeaderItem.Holder>(R.layout.item_user_list_header) {

    @EpoxyAttribute var header: String = ""

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.headerTextView.text = header
    }

    class Holder : VectorEpoxyHolder() {
        val headerTextView by bind<TextView>(R.id.userListHeaderView)
    }
}
