/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.core.epoxy

import android.util.TypedValue
import android.widget.CompoundButton
import android.widget.RadioButton
import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.features.settings.FontScaleValue

@EpoxyModelClass
abstract class FontScaleItem : VectorEpoxyModel<FontScaleItem.Holder>(R.layout.item_font_scale) {

    companion object {
        const val MINIMAL_TEXT_SIZE_DP = 10f
    }

    @EpoxyAttribute var fontScale: FontScaleValue? = null
    @EpoxyAttribute var selected: Boolean = true
    @EpoxyAttribute var enabled: Boolean = true

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var checkChangeListener: CompoundButton.OnCheckedChangeListener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        val context = holder.view.context
        holder.textView.text = fontScale?.let {
            context.resources.getString(it.nameResId)
        }
        val index = fontScale?.index ?: 0
        holder.textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, MINIMAL_TEXT_SIZE_DP + index * 2)
        holder.textView.isEnabled = enabled
        holder.button.isChecked = selected
        holder.button.isEnabled = enabled
        holder.button.isClickable = enabled
        holder.view.onClick {
            holder.button.performClick()
        }
        holder.button.setOnCheckedChangeListener(checkChangeListener)
    }

    class Holder : VectorEpoxyHolder() {
        val button by bind<RadioButton>(R.id.font_scale_radio_button)
        val textView by bind<TextView>(R.id.font_scale_text)
    }
}
