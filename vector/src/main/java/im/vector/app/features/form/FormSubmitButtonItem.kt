/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.form

import android.widget.Button
import androidx.annotation.StringRes
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.extensions.setTextOrHide

@EpoxyModelClass
abstract class FormSubmitButtonItem : VectorEpoxyModel<FormSubmitButtonItem.Holder>(R.layout.item_form_submit_button) {

    @EpoxyAttribute
    var enabled: Boolean = true

    @EpoxyAttribute
    var buttonTitle: String? = null

    @EpoxyAttribute
    @StringRes
    var buttonTitleId: Int? = null

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var buttonClickListener: ClickListener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        if (buttonTitleId != null) {
            holder.button.setText(buttonTitleId!!)
        } else {
            holder.button.setTextOrHide(buttonTitle)
        }

        holder.button.isEnabled = enabled
        holder.button.onClick(buttonClickListener)
    }

    class Holder : VectorEpoxyHolder() {
        val button by bind<Button>(R.id.form_submit_button)
    }
}
