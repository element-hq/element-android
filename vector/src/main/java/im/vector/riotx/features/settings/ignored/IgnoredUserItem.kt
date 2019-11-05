/*
 * Copyright 2019 New Vector Ltd
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
package im.vector.riotx.features.settings.ignored

import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.riotx.R
import im.vector.riotx.core.epoxy.VectorEpoxyHolder
import im.vector.riotx.core.epoxy.VectorEpoxyModel
import im.vector.riotx.core.extensions.setTextOrHide

/**
 * A list item for ignored user.
 */
@EpoxyModelClass(layout = R.layout.item_ignored_user)
abstract class IgnoredUserItem : VectorEpoxyModel<IgnoredUserItem.Holder>() {

    @EpoxyAttribute
    var userId: String? = null

    @EpoxyAttribute
    var itemClickAction: (() -> Unit)? = null

    override fun bind(holder: Holder) {
        holder.userIdText.setTextOrHide(userId)

        holder.userIdText.setOnClickListener { itemClickAction?.invoke() }
    }

    class Holder : VectorEpoxyHolder() {
        val userIdText by bind<TextView>(R.id.itemIgnoredUserId)
    }
}
