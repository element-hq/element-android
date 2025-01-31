/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.list

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
                val description = if (device.isInactive) {
                    stringProvider.getQuantityString(
                            R.plurals.device_manager_other_sessions_description_inactive,
                            SESSION_IS_MARKED_AS_INACTIVE_AFTER_DAYS,
                            SESSION_IS_MARKED_AS_INACTIVE_AFTER_DAYS,
                            formattedLastActivityDate
                    )
                } else if (device.roomEncryptionTrustLevel == RoomEncryptionTrustLevel.Trusted) {
                    stringProvider.getString(R.string.device_manager_other_sessions_description_verified, formattedLastActivityDate)
                } else {
                    stringProvider.getString(R.string.device_manager_other_sessions_description_unverified, formattedLastActivityDate)
                }
                val drawableColor = colorProvider.getColorFromAttribute(R.attr.vctr_content_secondary)
                val descriptionDrawable = if (device.isInactive) drawableProvider.getDrawable(R.drawable.ic_inactive_sessions, drawableColor) else null

                otherSessionItem {
                    id(device.deviceInfo.deviceId)
                    deviceType(DeviceType.UNKNOWN) // TODO. We don't have this info yet. Update accordingly.
                    roomEncryptionTrustLevel(device.roomEncryptionTrustLevel)
                    sessionName(device.deviceInfo.displayName)
                    sessionDescription(description)
                    sessionDescriptionDrawable(descriptionDrawable)
                    stringProvider(this@OtherSessionsController.stringProvider)
                    clickListener { device.deviceInfo.deviceId?.let { host.callback?.onItemClicked(it) } }
                }
            }
        }
    }
}
