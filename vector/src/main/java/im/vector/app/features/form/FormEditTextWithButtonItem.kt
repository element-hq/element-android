/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.form

import android.text.Editable
import android.widget.Button
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.TextListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.epoxy.addTextChangedListenerOnce
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.epoxy.setValueOnce
import im.vector.app.core.platform.SimpleTextWatcher

@EpoxyModelClass
abstract class FormEditTextWithButtonItem : VectorEpoxyModel<FormEditTextWithButtonItem.Holder>(R.layout.item_form_text_input_with_button) {

    @EpoxyAttribute
    var hint: String? = null

    @EpoxyAttribute
    var value: String? = null

    @EpoxyAttribute
    var enabled: Boolean = true

    @EpoxyAttribute
    var buttonText: String? = null

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var onTextChange: TextListener? = null

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var onButtonClicked: ClickListener? = null

    private val onTextChangeListener = object : SimpleTextWatcher() {
        override fun afterTextChanged(s: Editable) {
            onTextChange?.invoke(s.toString())
        }
    }

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.textInputLayout.isEnabled = enabled
        holder.textInputLayout.hint = hint

        holder.setValueOnce(holder.textInputEditText, value)

        holder.textInputEditText.isEnabled = enabled

        holder.textInputEditText.addTextChangedListenerOnce(onTextChangeListener)

        holder.textInputButton.text = buttonText
        holder.textInputButton.onClick(onButtonClicked)
    }

    override fun shouldSaveViewState(): Boolean {
        return false
    }

    override fun unbind(holder: Holder) {
        super.unbind(holder)
        holder.textInputEditText.removeTextChangedListener(onTextChangeListener)
    }

    class Holder : VectorEpoxyHolder() {
        val textInputLayout by bind<TextInputLayout>(R.id.formTextInputTextInputLayout)
        val textInputEditText by bind<TextInputEditText>(R.id.formTextInputTextInputEditText)
        val textInputButton by bind<Button>(R.id.formTextInputButton)
    }
}
