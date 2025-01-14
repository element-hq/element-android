/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
