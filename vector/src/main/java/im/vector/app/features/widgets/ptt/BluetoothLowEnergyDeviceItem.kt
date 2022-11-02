/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.features.widgets.ptt

import android.widget.TextView
import androidx.annotation.ColorInt
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.features.themes.ThemeUtils

@EpoxyModelClass
abstract class BluetoothLowEnergyDeviceItem : VectorEpoxyModel<BluetoothLowEnergyDeviceItem.Holder>(R.layout.item_bluetooth_device) {

    interface Callback {
        fun onItemSelected(deviceAddress: String)
    }

    @EpoxyAttribute
    var deviceName: String? = null

    @EpoxyAttribute
    var deviceMacAddress: String? = null

    @EpoxyAttribute
    var deviceConnectionStatusText: String? = null

    @EpoxyAttribute
    @ColorInt
    var deviceConnectionStatusTextColor: Int? = null

    @EpoxyAttribute
    var callback: Callback? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.bluetoothDeviceNameTextView.setTextOrHide(deviceName)
        holder.bluetoothDeviceMacAddressTextView.setTextOrHide(deviceMacAddress)
        holder.bluetoothDeviceConnectionStatusTextView.setTextOrHide(deviceConnectionStatusText)

        deviceConnectionStatusTextColor?.let {
            holder.bluetoothDeviceConnectionStatusTextView.setTextColor(it)
        } ?: run {
            holder.bluetoothDeviceConnectionStatusTextView.setTextColor(ThemeUtils.getColor(holder.view.context, R.attr.vctr_content_primary))
        }

        holder.view.setOnClickListener {
            deviceMacAddress?.let {
                callback?.onItemSelected(it)
            }
        }
    }

    class Holder : VectorEpoxyHolder() {
        val bluetoothDeviceNameTextView by bind<TextView>(R.id.bluetoothDeviceNameTextView)
        val bluetoothDeviceMacAddressTextView by bind<TextView>(R.id.bluetoothDeviceMacAddressTextView)
        val bluetoothDeviceConnectionStatusTextView by bind<TextView>(R.id.bluetoothDeviceConnectionStatusTextView)
    }
}
