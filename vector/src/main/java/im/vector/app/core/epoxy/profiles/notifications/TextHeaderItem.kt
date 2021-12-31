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

package im.vector.app.core.epoxy.profiles.notifications

import android.widget.TextView
import androidx.annotation.StringRes
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel

@EpoxyModelClass(layout = R.layout.item_text_header)
abstract class TextHeaderItem : VectorEpoxyModel<TextHeaderItem.Holder>() {

    @EpoxyAttribute
    var text: String? = null

    @StringRes
    @EpoxyAttribute
    var textRes: Int? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        val textResource = textRes
        if (textResource != null) {
            holder.textView.setText(textResource)
        } else {
            holder.textView.text = text
        }
    }

    class Holder : VectorEpoxyHolder() {
        val textView by bind<TextView>(R.id.headerText)
    }
}
