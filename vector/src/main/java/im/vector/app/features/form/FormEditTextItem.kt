/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.form

import android.text.Editable
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import im.vector.app.R
import im.vector.app.core.epoxy.TextListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.epoxy.addTextChangedListenerOnce
import im.vector.app.core.epoxy.setValueOnce
import im.vector.app.core.platform.SimpleTextWatcher

@EpoxyModelClass(layout = R.layout.item_form_text_input)
abstract class FormEditTextItem : VectorEpoxyModel<FormEditTextItem.Holder>() {

    @EpoxyAttribute
    var hint: String? = null

    @EpoxyAttribute
    var value: String? = null

    @EpoxyAttribute
    var forceUpdateValue: Boolean = false

    @EpoxyAttribute
    var errorMessage: String? = null

    @EpoxyAttribute
    var enabled: Boolean = true

    @EpoxyAttribute
    var inputType: Int? = null

    @EpoxyAttribute
    var singleLine: Boolean = true

    @EpoxyAttribute
    var imeOptions: Int? = null

    @EpoxyAttribute
    var endIconMode: Int? = null

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var onTextChange: TextListener? = null

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var editorActionListener: TextView.OnEditorActionListener? = null

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var onFocusChange: ((Boolean) -> Unit)? = null

    @EpoxyAttribute
    var prefixText: String? = null

    @EpoxyAttribute
    var suffixText: String? = null

    private val onTextChangeListener = object : SimpleTextWatcher() {
        override fun afterTextChanged(s: Editable) {
            onTextChange?.invoke(s.toString())
        }
    }

    private val onFocusChangedListener = View.OnFocusChangeListener { _, hasFocus -> onFocusChange?.invoke(hasFocus) }

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.textInputLayout.isEnabled = enabled
        holder.textInputLayout.hint = hint
        holder.textInputLayout.error = errorMessage
        holder.textInputLayout.endIconMode = endIconMode ?: TextInputLayout.END_ICON_NONE

        holder.textInputLayout.prefixText = prefixText
        holder.textInputLayout.suffixText = suffixText

        if (forceUpdateValue) {
            holder.textInputEditText.setText(value)
        } else {
            holder.setValueOnce(holder.textInputEditText, value)
        }

        holder.textInputEditText.isEnabled = enabled
        inputType?.let { holder.textInputEditText.inputType = it }
        holder.textInputEditText.isSingleLine = singleLine
        holder.textInputEditText.imeOptions = imeOptions ?: EditorInfo.IME_ACTION_NONE

        holder.textInputEditText.addTextChangedListenerOnce(onTextChangeListener)
        holder.textInputEditText.setOnEditorActionListener(editorActionListener)
        holder.textInputEditText.onFocusChangeListener = onFocusChangedListener
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
    }
}
