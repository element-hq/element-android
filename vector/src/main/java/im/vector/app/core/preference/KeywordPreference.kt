/*
 * Copyright (c) 2021 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.core.preference

import android.content.Context
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.core.view.children
import androidx.preference.PreferenceViewHolder
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import im.vector.app.R

class KeywordPreference : VectorPreference {

    interface Listener {
        fun didAddKeyword(keyword: String)
        fun didRemoveKeyword(keyword: String)
    }

    private var keywordsEnabled = true

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

        chipEditText.text = null
        chipGroup.removeAllViews()

        keywords.forEach {
            addChipToGroup(it, chipGroup)
        }

        chipEditText.isEnabled = keywordsEnabled
        chipGroup.isEnabled = keywordsEnabled
        chipGroup.children.forEach { it.isEnabled = keywordsEnabled }

        chipEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId != EditorInfo.IME_ACTION_DONE) {
                return@setOnEditorActionListener false
            }
            val keyword = chipEditText.text.toString().trim()
            if (keyword.isEmpty()){
                return@setOnEditorActionListener false
            }
            _keywords.add(keyword)
            listener?.didAddKeyword(keyword)
            onPreferenceChangeListener?.onPreferenceChange(this, _keywords)
            notifyChanged()
            chipEditText.text = null
            return@setOnEditorActionListener true
        }

    }

    private fun addChipToGroup(keyword: String, chipGroup: ChipGroup) {
        val chip = Chip(context, null, R.attr.vctr_keyword_style)
        chip.text = keyword
        chip.isClickable = true
        chip.isCheckable = false
        chip.isCloseIconVisible = true
        chipGroup.addView(chip)

        chip.setOnCloseIconClickListener {
            if (!keywordsEnabled)
                return@setOnCloseIconClickListener
            _keywords.remove(keyword)
            listener?.didRemoveKeyword(keyword)
            onPreferenceChangeListener?.onPreferenceChange(this, _keywords)
            notifyChanged()
        }
    }
}
