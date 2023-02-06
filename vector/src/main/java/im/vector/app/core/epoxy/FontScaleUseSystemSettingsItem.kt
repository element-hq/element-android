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

import android.widget.CheckBox
import android.widget.CompoundButton
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R

@EpoxyModelClass
abstract class FontScaleUseSystemSettingsItem : VectorEpoxyModel<FontScaleUseSystemSettingsItem.Holder>(R.layout.item_font_scale_system) {

    @EpoxyAttribute var useSystemSettings: Boolean = true

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var checkChangeListener: CompoundButton.OnCheckedChangeListener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.checkBox.isChecked = useSystemSettings
        holder.checkBox.setOnCheckedChangeListener(checkChangeListener)
        holder.view.onClick {
            holder.checkBox.performClick()
        }
    }

    class Holder : VectorEpoxyHolder() {
        val checkBox by bind<CheckBox>(R.id.font_scale_use_system_checkbox)
    }
}
