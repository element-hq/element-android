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
package im.vector.app.core.ui.list

import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.google.android.material.button.MaterialButton
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.epoxy.onClick
import im.vector.lib.core.utils.epoxy.charsequence.EpoxyCharSequence

/**
 * A generic button list item.
 */
@EpoxyModelClass
abstract class ButtonPositiveDestructiveButtonBarItem : VectorEpoxyModel<ButtonPositiveDestructiveButtonBarItem.Holder>(
        R.layout.item_positive_destrutive_buttons
) {

    @EpoxyAttribute
    var positiveText: EpoxyCharSequence? = null

    @EpoxyAttribute
    var destructiveText: EpoxyCharSequence? = null

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var positiveButtonClickAction: ClickListener? = null

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var destructiveButtonClickAction: ClickListener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        positiveText?.charSequence?.let { holder.positiveButton.text = it }
        destructiveText?.charSequence?.let { holder.destructiveButton.text = it }

        holder.positiveButton.onClick(positiveButtonClickAction)
        holder.destructiveButton.onClick(destructiveButtonClickAction)
    }

    class Holder : VectorEpoxyHolder() {
        val destructiveButton by bind<MaterialButton>(R.id.destructive_button)
        val positiveButton by bind<MaterialButton>(R.id.positive_button)
    }
}
