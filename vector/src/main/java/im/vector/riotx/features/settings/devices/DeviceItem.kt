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

package im.vector.riotx.features.settings.devices

import android.graphics.Typeface
import android.view.View
import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.matrix.android.internal.crypto.model.rest.DeviceInfo
import im.vector.riotx.R
import im.vector.riotx.core.epoxy.VectorEpoxyHolder
import im.vector.riotx.core.epoxy.VectorEpoxyModel

/**
 * A list item for Device.
 */
@EpoxyModelClass(layout = R.layout.item_device)
abstract class DeviceItem : VectorEpoxyModel<DeviceItem.Holder>() {

    @EpoxyAttribute
    lateinit var deviceInfo: DeviceInfo

    @EpoxyAttribute
    var bold = false

    @EpoxyAttribute
    var itemClickAction: (() -> Unit)? = null

    override fun bind(holder: Holder) {
        holder.root.setOnClickListener { itemClickAction?.invoke() }

        holder.displayNameText.text = deviceInfo.displayName ?: ""
        holder.deviceIdText.text = deviceInfo.deviceId ?: ""

        if (bold) {
            holder.displayNameText.setTypeface(null, Typeface.BOLD)
            holder.deviceIdText.setTypeface(null, Typeface.BOLD)
        } else {
            holder.displayNameText.setTypeface(null, Typeface.NORMAL)
            holder.deviceIdText.setTypeface(null, Typeface.NORMAL)
        }
    }

    class Holder : VectorEpoxyHolder() {
        val root by bind<View>(R.id.itemDeviceRoot)
        val displayNameText by bind<TextView>(R.id.itemDeviceDisplayName)
        val deviceIdText by bind<TextView>(R.id.itemDeviceId)
    }
}
