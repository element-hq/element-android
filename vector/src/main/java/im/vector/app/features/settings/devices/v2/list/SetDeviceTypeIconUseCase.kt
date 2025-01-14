/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
