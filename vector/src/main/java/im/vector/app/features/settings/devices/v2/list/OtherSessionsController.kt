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

import com.airbnb.epoxy.TypedEpoxyController
import im.vector.app.R
import im.vector.app.core.date.DateFormatKind
import im.vector.app.core.date.VectorDateFormatter
import im.vector.app.core.epoxy.noResultItem
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.settings.devices.DeviceFullInfo
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel
import javax.inject.Inject

class OtherSessionsController @Inject constructor(
        private val stringProvider: StringProvider,
        private val dateFormatter: VectorDateFormatter,
) : TypedEpoxyController<List<DeviceFullInfo>>() {

    override fun buildModels(data: List<DeviceFullInfo>?) {
        val host = this

        if (data.isNullOrEmpty()) {
            noResultItem {
                id("empty")
                text(host.stringProvider.getString(R.string.no_result_placeholder))
            }
        } else {
            data.take(NUMBER_OF_OTHER_DEVICES_TO_RENDER).forEach { device ->
                val formattedLastActivityDate = host.dateFormatter.format(device.deviceInfo.lastSeenTs, DateFormatKind.DEFAULT_DATE_AND_TIME)
                val description = if (device.trustLevelForShield == RoomEncryptionTrustLevel.Trusted) {
                    stringProvider.getString(R.string.device_manager_other_sessions_description_verified, formattedLastActivityDate)
                } else {
                    stringProvider.getString(R.string.device_manager_other_sessions_description_unverified, formattedLastActivityDate)
                }

                otherSessionItem {
                    id(device.deviceInfo.deviceId)
                    deviceType(DeviceType.UNKNOWN) // TODO. We don't have this info yet. Update accordingly.
                    roomEncryptionTrustLevel(device.trustLevelForShield)
                    sessionName(device.deviceInfo.displayName)
                    sessionDescription(description)
                    stringProvider(this@OtherSessionsController.stringProvider)
                }
            }
        }
    }
}
