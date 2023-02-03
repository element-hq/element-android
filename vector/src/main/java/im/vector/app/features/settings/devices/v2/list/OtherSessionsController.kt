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

import android.view.View
import com.airbnb.epoxy.TypedEpoxyController
import im.vector.app.R
import im.vector.app.core.date.DateFormatKind
import im.vector.app.core.date.VectorDateFormatter
import im.vector.app.core.epoxy.noResultItem
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.DrawableProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.settings.devices.v2.DeviceFullInfo
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel
import javax.inject.Inject

class OtherSessionsController @Inject constructor(
        private val stringProvider: StringProvider,
        private val dateFormatter: VectorDateFormatter,
        private val drawableProvider: DrawableProvider,
        private val colorProvider: ColorProvider,
) : TypedEpoxyController<List<DeviceFullInfo>>() {

    var callback: Callback? = null

    interface Callback {
        fun onItemLongClicked(deviceId: String)
        fun onItemClicked(deviceId: String)
    }

    override fun buildModels(data: List<DeviceFullInfo>?) {
        val host = this

        if (data.isNullOrEmpty()) {
            noResultItem {
                id("empty")
                text(host.stringProvider.getString(R.string.no_result_placeholder))
            }
        } else {
            data.forEach { device ->
                val dateFormatKind = if (device.isInactive) DateFormatKind.TIMELINE_DAY_DIVIDER else DateFormatKind.DEFAULT_DATE_AND_TIME
                val formattedLastActivityDate = host.dateFormatter.format(device.deviceInfo.lastSeenTs, dateFormatKind)
                val description = buildDescription(device, formattedLastActivityDate)
                val descriptionColor = if (device.isCurrentDevice) {
                    host.colorProvider.getColorFromAttribute(R.attr.colorError)
                } else {
                    host.colorProvider.getColorFromAttribute(R.attr.vctr_content_secondary)
                }
                val drawableColor = host.colorProvider.getColorFromAttribute(R.attr.vctr_content_secondary)
                val descriptionDrawable = if (device.isInactive) host.drawableProvider.getDrawable(R.drawable.ic_inactive_sessions, drawableColor) else null
                val sessionName = device.deviceInfo.displayName ?: device.deviceInfo.deviceId

                otherSessionItem {
                    id(device.deviceInfo.deviceId)
                    deviceType(device.deviceExtendedInfo.deviceType)
                    roomEncryptionTrustLevel(device.roomEncryptionTrustLevel)
                    sessionName(sessionName)
                    sessionDescription(description)
                    sessionDescriptionDrawable(descriptionDrawable)
                    sessionDescriptionColor(descriptionColor)
                    ipAddress(device.deviceInfo.lastSeenIp)
                    stringProvider(host.stringProvider)
                    colorProvider(host.colorProvider)
                    drawableProvider(host.drawableProvider)
                    selected(device.isSelected)
                    clickListener { device.deviceInfo.deviceId?.let { host.callback?.onItemClicked(it) } }
                    onLongClickListener(View.OnLongClickListener {
                        device.deviceInfo.deviceId?.let { host.callback?.onItemLongClicked(it) }
                        true
                    })
                }
            }
        }
    }

    private fun buildDescription(device: DeviceFullInfo, formattedLastActivityDate: String): String {
        return when {
            device.isInactive -> {
                stringProvider.getQuantityString(
                        R.plurals.device_manager_other_sessions_description_inactive,
                        SESSION_IS_MARKED_AS_INACTIVE_AFTER_DAYS,
                        SESSION_IS_MARKED_AS_INACTIVE_AFTER_DAYS,
                        formattedLastActivityDate
                )
            }
            device.roomEncryptionTrustLevel == RoomEncryptionTrustLevel.Trusted -> {
                stringProvider.getString(R.string.device_manager_other_sessions_description_verified, formattedLastActivityDate)
            }
            device.isCurrentDevice -> {
                stringProvider.getString(R.string.device_manager_other_sessions_description_unverified_current_session)
            }
            device.roomEncryptionTrustLevel == RoomEncryptionTrustLevel.Default -> {
                stringProvider.getString(R.string.device_manager_session_last_activity, formattedLastActivityDate)
            }
            else -> {
                stringProvider.getString(R.string.device_manager_other_sessions_description_unverified, formattedLastActivityDate)
            }
        }
    }
}
