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
package im.vector.app.core.epoxy

import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R

/**
 * Default background color is for the bottom sheets (R.attr.vctr_list_bottom_sheet_divider_color).
 * To use in fragment, set color using R.attr.riotx_list_divider_color
 */
@EpoxyModelClass(layout = R.layout.item_divider)
abstract class DividerItem : VectorEpoxyModel<DividerItem.Holder>() {

    @EpoxyAttribute var color: Int = -1

    override fun bind(holder: Holder) {
        super.bind(holder)
        if (color != -1) {
            holder.view.setBackgroundColor(color)
        }
    }

    class Holder : VectorEpoxyHolder()
}
