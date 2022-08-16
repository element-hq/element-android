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

package im.vector.app.features.spaces

import android.content.Context
import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel

@EpoxyModelClass
abstract class NewSpaceListHeaderItem : VectorEpoxyModel<NewSpaceListHeaderItem.Holder>(R.layout.item_new_space_list_header) {

    @EpoxyAttribute var currentSpace: String? = null
    @EpoxyAttribute var spaceHistory: List<Pair<String?, String>> = emptyList()

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.spaceHeader.text = buildSpaceHeaderText(holder.spaceHeader.context)
    }

    private fun buildSpaceHeaderText(context: Context): String {
        val allChats = context.getString(R.string.all_chats)
        var spaceHeaderText = allChats

        val nonRootSpaceHistory = spaceHistory.filter { it.second.isNotEmpty() }

        if (nonRootSpaceHistory.isNotEmpty()) {
            spaceHeaderText += " > ${nonRootSpaceHistory.joinToString(" > ") { it.second }}"
        }
        if (currentSpace != null) {
            spaceHeaderText += " > $currentSpace"
        }
        return spaceHeaderText
    }

    class Holder : VectorEpoxyHolder() {
        val spaceHeader by bind<TextView>(R.id.space_header)
    }
}
