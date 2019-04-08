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

package im.vector.riotredesign.features.autocomplete.command

import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.riotredesign.R
import im.vector.riotredesign.core.epoxy.VectorEpoxyHolder
import im.vector.riotredesign.core.epoxy.VectorEpoxyModel

@EpoxyModelClass(layout = R.layout.item_command_autocomplete)
abstract class AutocompleteCommandItem : VectorEpoxyModel<AutocompleteCommandItem.Holder>() {

    @EpoxyAttribute
    var name: CharSequence? = null
    @EpoxyAttribute
    var parameters: CharSequence? = null
    @EpoxyAttribute
    var description: CharSequence? = null

    override fun bind(holder: Holder) {
        holder.nameView.text = name
        holder.parametersView.text = parameters
        holder.descriptionView.text = description
    }

    class Holder : VectorEpoxyHolder() {
        val nameView by bind<TextView>(R.id.commandName)
        val parametersView by bind<TextView>(R.id.commandParameter)
        val descriptionView by bind<TextView>(R.id.commandDescription)
    }

}