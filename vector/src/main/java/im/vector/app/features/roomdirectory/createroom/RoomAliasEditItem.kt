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

package im.vector.app.features.roomdirectory.createroom

import android.text.Editable
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.epoxy.setValueOnce
import im.vector.app.core.platform.SimpleTextWatcher

@EpoxyModelClass(layout = R.layout.item_room_alias_text_input)
abstract class RoomAliasEditItem : VectorEpoxyModel<RoomAliasEditItem.Holder>() {

    @EpoxyAttribute
    var value: String? = null

    @EpoxyAttribute
    var showBottomSeparator: Boolean = true

    @EpoxyAttribute
    var errorMessage: String? = null

    @EpoxyAttribute
    var homeServer: String? = null

    @EpoxyAttribute
    var enabled: Boolean = true

    @EpoxyAttribute
    var onTextChange: ((String) -> Unit)? = null

    private val onTextChangeListener = object : SimpleTextWatcher() {
        override fun afterTextChanged(s: Editable) {
            onTextChange?.invoke(s.toString())
        }
    }

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.textInputLayout.isEnabled = enabled
        holder.textInputLayout.error = errorMessage

        holder.setValueOnce(holder.textInputEditText, value)
        holder.textInputEditText.isEnabled = enabled
        holder.textInputEditText.addTextChangedListener(onTextChangeListener)
        holder.homeServerText.text = homeServer
        holder.bottomSeparator.isVisible = showBottomSeparator
    }

    override fun shouldSaveViewState(): Boolean {
        return false
    }

    override fun unbind(holder: Holder) {
        super.unbind(holder)
        holder.textInputEditText.removeTextChangedListener(onTextChangeListener)
    }

    class Holder : VectorEpoxyHolder() {
        val textInputLayout by bind<TextInputLayout>(R.id.itemRoomAliasTextInputLayout)
        val textInputEditText by bind<TextInputEditText>(R.id.itemRoomAliasTextInputEditText)
        val homeServerText by bind<TextView>(R.id.itemRoomAliasHomeServer)
        val bottomSeparator by bind<View>(R.id.itemRoomAliasDivider)
    }
}
