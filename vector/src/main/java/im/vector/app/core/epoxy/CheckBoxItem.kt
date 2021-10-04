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

package im.vector.app.core.epoxy

import android.widget.CompoundButton
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.google.android.material.checkbox.MaterialCheckBox
import im.vector.app.R

@EpoxyModelClass(layout = R.layout.item_checkbox)
abstract class CheckBoxItem : VectorEpoxyModel<CheckBoxItem.Holder>() {

    @EpoxyAttribute
    var checked: Boolean = false

    @EpoxyAttribute lateinit var title: String

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var checkChangeListener: CompoundButton.OnCheckedChangeListener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.checkbox.isChecked = checked
        holder.checkbox.text = title
        holder.checkbox.setOnCheckedChangeListener(checkChangeListener)
    }

    class Holder : VectorEpoxyHolder() {
        val checkbox by bind<MaterialCheckBox>(R.id.checkbox)
    }
}
