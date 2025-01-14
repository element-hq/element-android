/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
import im.vector.lib.strings.CommonPlurals
import im.vector.lib.strings.CommonStrings
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
                text(host.stringProvider.getString(CommonStrings.no_result_placeholder))
            }
        } else {
            data.forEach { device ->
                val dateFormatKind = if (device.isInactive) DateFormatKind.TIMELINE_DAY_DIVIDER else DateFormatKind.DEFAULT_DATE_AND_TIME
                val formattedLastActivityDate = host.dateFormatter.format(device.deviceInfo.lastSeenTs, dateFormatKind)
                val description = buildDescription(device, formattedLastActivityDate)
                val descriptionColor = if (device.isCurrentDevice) {
                    host.colorProvider.getColorFromAttribute(com.google.android.material.R.attr.colorError)
                } else {
                    host.colorProvider.getColorFromAttribute(im.vector.lib.ui.styles.R.attr.vctr_content_secondary)
                }
                val drawableColor = host.colorProvider.getColorFromAttribute(im.vector.lib.ui.styles.R.attr.vctr_content_secondary)
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
                        CommonPlurals.device_manager_other_sessions_description_inactive,
                        SESSION_IS_MARKED_AS_INACTIVE_AFTER_DAYS,
                        SESSION_IS_MARKED_AS_INACTIVE_AFTER_DAYS,
                        formattedLastActivityDate
                )
            }
            device.roomEncryptionTrustLevel == RoomEncryptionTrustLevel.Trusted -> {
                stringProvider.getString(CommonStrings.device_manager_other_sessions_description_verified, formattedLastActivityDate)
            }
            device.isCurrentDevice -> {
                stringProvider.getString(CommonStrings.device_manager_other_sessions_description_unverified_current_session)
            }
            device.roomEncryptionTrustLevel == RoomEncryptionTrustLevel.Default -> {
                stringProvider.getString(CommonStrings.device_manager_session_last_activity, formattedLastActivityDate)
            }
            else -> {
                stringProvider.getString(CommonStrings.device_manager_other_sessions_description_unverified, formattedLastActivityDate)
            }
        }
    }
}
