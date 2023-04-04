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

import com.airbnb.epoxy.EpoxyController
import im.vector.app.R
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.StringProvider
import javax.inject.Inject

class BluetoothLowEnergyDevicesBottomSheetController @Inject constructor(
        private val stringProvider: StringProvider,
        private val colorProvider: ColorProvider,
) : EpoxyController() {

    interface Callback {
        fun onItemSelected(deviceAddress: String)
    }

    private var deviceList: List<BluetoothLowEnergyDevice>? = null
    var callback: Callback? = null

    fun setData(deviceList: List<BluetoothLowEnergyDevice>) {
        this.deviceList = deviceList
        requestModelBuild()
    }

    override fun buildModels() {
        val currentDeviceList = deviceList ?: return
        val host = this

        currentDeviceList.forEach { device ->
            val deviceConnectionStatus = host.stringProvider.getString(
                    if (device.isConnected) R.string.push_to_talk_device_connected else R.string.push_to_talk_device_disconnected
            )
            val deviceConnectionStatusColor = host.colorProvider.getColorFromAttribute(
                    if (device.isConnected) R.attr.colorPrimary else R.attr.colorError
            )

            val deviceItemCallback = object : BluetoothLowEnergyDeviceItem.Callback {
                override fun onItemSelected(deviceAddress: String) {
                    host.callback?.onItemSelected(deviceAddress)
                }
            }

            bluetoothLowEnergyDeviceItem {
                id(device.hashCode())
                deviceName(device.name)
                deviceMacAddress(device.macAddress)
                deviceConnectionStatusText(deviceConnectionStatus)
                deviceConnectionStatusTextColor(deviceConnectionStatusColor)
                callback(deviceItemCallback)
            }
        }
    }
}
