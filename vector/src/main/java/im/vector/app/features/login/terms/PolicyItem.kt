/*
 * Copyright 2018 New Vector Ltd
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

package im.vector.app.features.login.terms

import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.airbnb.epoxy.EpoxyModelWithHolder
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.onClick

@EpoxyModelClass(layout = R.layout.item_policy)
abstract class PolicyItem : EpoxyModelWithHolder<PolicyItem.Holder>() {
    @EpoxyAttribute
    var checked: Boolean = false

    @EpoxyAttribute
    var title: String? = null

    @EpoxyAttribute
    var subtitle: String? = null

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var checkChangeListener: CompoundButton.OnCheckedChangeListener? = null

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var clickListener: ClickListener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.let {
            it.checkbox.isChecked = checked
            it.checkbox.setOnCheckedChangeListener(checkChangeListener)
            it.title.text = title
            it.subtitle.text = subtitle
            it.view.onClick(clickListener)
        }
    }

    // Ensure checkbox behaves as expected (remove the listener)
    override fun unbind(holder: Holder) {
        super.unbind(holder)
        holder.checkbox.setOnCheckedChangeListener(null)
    }

    class Holder : VectorEpoxyHolder() {
        val checkbox by bind<CheckBox>(R.id.adapter_item_policy_checkbox)
        val title by bind<TextView>(R.id.adapter_item_policy_title)
        val subtitle by bind<TextView>(R.id.adapter_item_policy_subtitle)
    }
}
