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

package im.vector.app.features.settings.devices.v2.list

import android.widget.ImageView
import im.vector.app.R
import im.vector.app.core.resources.StringProvider
import im.vector.lib.strings.CommonStrings

class SetDeviceTypeIconUseCase {

    fun execute(deviceType: DeviceType, imageView: ImageView, stringProvider: StringProvider) {
        when (deviceType) {
            DeviceType.MOBILE -> {
                imageView.setImageResource(R.drawable.ic_device_type_mobile)
                imageView.contentDescription = stringProvider.getString(CommonStrings.a11y_device_manager_device_type_mobile)
            }
            DeviceType.WEB -> {
                imageView.setImageResource(R.drawable.ic_device_type_web)
                imageView.contentDescription = stringProvider.getString(CommonStrings.a11y_device_manager_device_type_web)
            }
            DeviceType.DESKTOP -> {
                imageView.setImageResource(R.drawable.ic_device_type_desktop)
                imageView.contentDescription = stringProvider.getString(CommonStrings.a11y_device_manager_device_type_desktop)
            }
            DeviceType.UNKNOWN -> {
                imageView.setImageResource(R.drawable.ic_device_type_unknown)
                imageView.contentDescription = stringProvider.getString(CommonStrings.a11y_device_manager_device_type_unknown)
            }
        }
    }
}
