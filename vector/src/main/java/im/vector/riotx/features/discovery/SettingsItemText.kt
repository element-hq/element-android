/*
 * Copyright (c) 2020 New Vector Ltd
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
package im.vector.riotx.features.discovery

import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.airbnb.epoxy.EpoxyModelWithHolder
import com.google.android.material.textfield.TextInputLayout
import im.vector.riotx.R
import im.vector.riotx.core.epoxy.VectorEpoxyHolder
import im.vector.riotx.core.extensions.setTextOrHide

@EpoxyModelClass(layout = R.layout.item_settings_edit_text)
abstract class SettingsItemText : EpoxyModelWithHolder<SettingsItemText.Holder>() {

    @EpoxyAttribute var descriptionText: String? = null
    @EpoxyAttribute var errorText: String? = null

    @EpoxyAttribute
    var interactionListener: Listener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.textView.setTextOrHide(descriptionText)

        holder.validateButton.setOnClickListener {
            val code = holder.editText.text.toString()
            holder.editText.text.clear()
            interactionListener?.onValidate(code)
        }

        if (errorText.isNullOrBlank()) {
            holder.textInputLayout.error = null
        } else {
            holder.textInputLayout.error = errorText
        }

        holder.editText.setOnEditorActionListener { tv, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val code = tv.text.toString()
                interactionListener?.onValidate(code)
                holder.editText.text.clear()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
    }

    class Holder : VectorEpoxyHolder() {
        val textView by bind<TextView>(R.id.settings_item_description)
        val editText by bind<EditText>(R.id.settings_item_edittext)
        val textInputLayout by bind<TextInputLayout>(R.id.settings_item_enter_til)
        val validateButton by bind<Button>(R.id.settings_item_enter_button)
    }

    interface Listener {
        fun onValidate(code: String)
    }
}
