/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.epoxy

import android.widget.CompoundButton
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.google.android.material.checkbox.MaterialCheckBox
import im.vector.app.R

@EpoxyModelClass
abstract class CheckBoxItem : VectorEpoxyModel<CheckBoxItem.Holder>(R.layout.item_checkbox) {

    @EpoxyAttribute
    var checked: Boolean = false

    @EpoxyAttribute lateinit var title: String

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var checkChangeListener: CompoundButton.OnCheckedChangeListener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.checkbox.isChecked = checked
        holder.checkbox.text = title
        holder.checkbox.setOnCheckedChangeListener(checkChangeListener)
    }

    class Holder : VectorEpoxyHolder() {
        val checkbox by bind<MaterialCheckBox>(R.id.checkbox)
    }
}
