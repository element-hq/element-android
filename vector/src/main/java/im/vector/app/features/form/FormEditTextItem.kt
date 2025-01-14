/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.form

import android.text.Editable
import android.text.InputFilter
import android.text.InputType
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

@EpoxyModelClass
abstract class FormEditTextItem : VectorEpoxyModel<FormEditTextItem.Holder>(R.layout.item_form_text_input) {

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
    var autoCapitalize: Boolean = false

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

    @EpoxyAttribute
    var maxLength: Int? = null

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

        configureInputType(holder)
        configureImeOptions(holder)

        holder.textInputEditText.addTextChangedListenerOnce(onTextChangeListener)
        holder.textInputEditText.setOnEditorActionListener(editorActionListener)
        holder.textInputEditText.onFocusChangeListener = onFocusChangedListener

        if (maxLength != null) {
            holder.textInputEditText.filters = arrayOf(InputFilter.LengthFilter(maxLength!!))
            holder.textInputLayout.isCounterEnabled = true
            holder.textInputLayout.counterMaxLength = maxLength!!
        } else {
            holder.textInputEditText.filters = arrayOf()
            holder.textInputLayout.isCounterEnabled = false
        }
    }

    /**
     * Configure the inputType of the EditText, input type should be always defined
     * especially when we want to use a single line, we set the InputType to InputType.TYPE_CLASS_TEXT
     * while the default for the EditText is InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE.
     */
    private fun configureInputType(holder: Holder) {
        val newInputType =
                inputType ?: InputType.TYPE_CLASS_TEXT
                        .let { if (autoCapitalize) it or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES else it }
                        .let { if (!singleLine) it or InputType.TYPE_TEXT_FLAG_MULTI_LINE else it }

        // This is a must in order to avoid extreme lag in some devices, on fast typing
        if (holder.textInputEditText.inputType != newInputType) {
            holder.textInputEditText.inputType = newInputType
        }
    }

    /**
     * Configure the imeOptions of the EditText, when imeOptions are not defined by the developer
     * EditorInfo.IME_ACTION_NEXT will be used for singleLine EditTexts to disable "new line"
     * while EditorInfo.IME_ACTION_NONE will be used for all the other cases.
     */
    private fun configureImeOptions(holder: Holder) {
        holder.textInputEditText.imeOptions =
                imeOptions ?: when (singleLine) {
                    true -> EditorInfo.IME_ACTION_NEXT
                    false -> EditorInfo.IME_ACTION_NONE
                }
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
