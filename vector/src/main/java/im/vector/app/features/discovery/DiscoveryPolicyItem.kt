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

package im.vector.app.features.discovery

import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.extensions.setTextOrHide

@EpoxyModelClass
abstract class DiscoveryPolicyItem : VectorEpoxyModel<DiscoveryPolicyItem.Holder>(R.layout.item_discovery_policy) {

    @EpoxyAttribute
    var name: String? = null

    @EpoxyAttribute
    var url: String? = null

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var clickListener: ClickListener? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.title.text = name
        holder.url.setTextOrHide(url)
        holder.view.onClick(clickListener)
    }

    class Holder : VectorEpoxyHolder() {
        val title by bind<TextView>(R.id.discovery_policy_name)
        val url by bind<TextView>(R.id.discovery_policy_url)
    }
}
