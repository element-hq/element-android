/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.preference

import android.content.Context
import android.text.Editable
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import androidx.core.view.children
import androidx.preference.PreferenceViewHolder
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputLayout
import im.vector.app.R
import im.vector.app.core.epoxy.addTextChangedListenerOnce
import im.vector.app.core.platform.SimpleTextWatcher
import im.vector.lib.strings.CommonStrings

class KeywordPreference : VectorPreference {

    interface Listener {
        fun onFocusDidChange(hasFocus: Boolean)
        fun didAddKeyword(keyword: String)
        fun didRemoveKeyword(keyword: String)
    }

    private var keywordsEnabled = true
    private var isCurrentKeywordValid = true

    private var _keywords: LinkedHashSet<String> = linkedSetOf()

    var keywords: Set<String>
        get() {
            return _keywords
        }
        set(value) {
            // Updates existing `LinkedHashSet` vs assign a new set.
            // This preserves the order added while on the screen (avoids keywords jumping around).
            _keywords.removeAll(_keywords.filter { !value.contains(it) })
            _keywords.addAll(value.sorted())
            notifyChanged()
        }

    var listener: Listener? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    init {
        layoutResource = R.layout.vector_preference_chip_group
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        keywordsEnabled = enabled
        notifyChanged()
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        holder.itemView.setOnClickListener(null)
        holder.itemView.setOnLongClickListener(null)

        val chipEditText = holder.findViewById(R.id.chipEditText) as? EditText ?: return
        val chipGroup = holder.findViewById(R.id.chipGroup) as? ChipGroup ?: return
        val addKeywordButton = holder.findViewById(R.id.addKeywordButton) as? Button ?: return
        val chipTextInputLayout = holder.findViewById(R.id.chipTextInputLayout) as? TextInputLayout ?: return

        chipEditText.text = null
        chipGroup.removeAllViews()

        keywords.forEach {
            addChipToGroup(it, chipGroup)
        }

        chipEditText.isEnabled = keywordsEnabled
        chipGroup.isEnabled = keywordsEnabled
        chipGroup.children.forEach { it.isEnabled = keywordsEnabled }

        chipEditText.addTextChangedListenerOnce(onTextChangeListener(chipTextInputLayout, addKeywordButton))
        chipEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId != EditorInfo.IME_ACTION_DONE) {
                return@setOnEditorActionListener false
            }
            return@setOnEditorActionListener addKeyword(chipEditText)
        }
        chipEditText.setOnFocusChangeListener { _, hasFocus ->
            listener?.onFocusDidChange(hasFocus)
        }

        addKeywordButton.setOnClickListener {
            addKeyword(chipEditText)
        }
    }

    private fun addKeyword(chipEditText: EditText): Boolean {
        val keyword = chipEditText.text.toString().trim()

        if (!isCurrentKeywordValid || keyword.isEmpty()) {
            return false
        }

        listener?.didAddKeyword(keyword)
        onPreferenceChangeListener?.onPreferenceChange(this, _keywords)
        notifyChanged()
        chipEditText.text = null
        return true
    }

    private fun onTextChangeListener(chipTextInputLayout: TextInputLayout, addKeywordButton: Button) = object : SimpleTextWatcher() {
        override fun afterTextChanged(s: Editable) {
            val keyword = s.toString().trim()
            val errorMessage = when {
                keyword.startsWith(".") -> {
                    context.getString(CommonStrings.settings_notification_keyword_contains_dot)
                }
                keyword.contains("\\") -> {
                    context.getString(CommonStrings.settings_notification_keyword_contains_invalid_character, "\\")
                }
                keyword.contains("/") -> {
                    context.getString(CommonStrings.settings_notification_keyword_contains_invalid_character, "/")
                }
                else -> null
            }

            chipTextInputLayout.isErrorEnabled = errorMessage != null
            chipTextInputLayout.error = errorMessage
            val keywordValid = errorMessage == null
            addKeywordButton.isEnabled = keywordsEnabled && keywordValid
            this@KeywordPreference.isCurrentKeywordValid = keywordValid
        }
    }

    private fun addChipToGroup(keyword: String, chipGroup: ChipGroup) {
        val chip = Chip(context, null, im.vector.lib.ui.styles.R.attr.vctr_keyword_style)
        chip.text = keyword
        chipGroup.addView(chip)

        chip.setOnCloseIconClickListener {
            if (!keywordsEnabled) {
                return@setOnCloseIconClickListener
            }
            listener?.didRemoveKeyword(keyword)
            onPreferenceChangeListener?.onPreferenceChange(this, _keywords)
            notifyChanged()
        }
    }
}
