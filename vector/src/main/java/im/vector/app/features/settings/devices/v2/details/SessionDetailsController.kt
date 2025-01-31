/*
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.details

import android.view.View
import androidx.annotation.StringRes
import com.airbnb.epoxy.TypedEpoxyController
import im.vector.app.R
import im.vector.app.core.date.DateFormatKind
import im.vector.app.core.date.VectorDateFormatter
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.utils.DimensionConverter
import org.matrix.android.sdk.api.session.crypto.model.DeviceInfo
import javax.inject.Inject

class SessionDetailsController @Inject constructor(
        private val checkIfSectionSessionIsVisibleUseCase: CheckIfSectionSessionIsVisibleUseCase,
        private val checkIfSectionDeviceIsVisibleUseCase: CheckIfSectionDeviceIsVisibleUseCase,
        private val stringProvider: StringProvider,
        private val dateFormatter: VectorDateFormatter,
        private val dimensionConverter: DimensionConverter,
) : TypedEpoxyController<DeviceInfo>() {

    var callback: Callback? = null

    interface Callback {
        fun onItemLongClicked(content: String)
    }

    override fun buildModels(data: DeviceInfo?) {
        data?.let { info ->
            val hasSectionSession = hasSectionSession(data)
            if (hasSectionSession) {
                buildSectionSession(info)
            }

            if (hasSectionDevice(data)) {
                buildSectionDevice(info, addExtraTopMargin = hasSectionSession)
            }
        }
    }

    private fun buildHeaderItem(@StringRes titleResId: Int, addExtraTopMargin: Boolean = false) {
        val host = this
        sessionDetailsHeaderItem {
            id(titleResId)
            title(host.stringProvider.getString(titleResId))
            addExtraTopMargin(addExtraTopMargin)
            dimensionConverter(host.dimensionConverter)
        }
    }

    private fun buildContentItem(@StringRes titleResId: Int, value: String, hasDivider: Boolean) {
        val host = this
        sessionDetailsContentItem {
            id(titleResId)
            title(host.stringProvider.getString(titleResId))
            description(value)
            hasDivider(hasDivider)
            onLongClickListener(View.OnLongClickListener {
                host.callback?.onItemLongClicked(value)
                true
            })
        }
    }

    private fun hasSectionSession(data: DeviceInfo): Boolean {
        return checkIfSectionSessionIsVisibleUseCase.execute(data)
    }

    private fun buildSectionSession(data: DeviceInfo) {
        val sessionName = data.displayName
        val sessionId = data.deviceId
        val sessionLastSeenTs = data.lastSeenTs

        buildHeaderItem(R.string.device_manager_session_title)

        sessionName?.let {
            val hasDivider = sessionId != null || sessionLastSeenTs != null
            buildContentItem(R.string.device_manager_session_details_session_name, it, hasDivider)
        }
        sessionId?.let {
            val hasDivider = sessionLastSeenTs != null
            buildContentItem(R.string.device_manager_session_details_session_id, it, hasDivider)
        }
        sessionLastSeenTs?.let {
            val formattedDate = dateFormatter.format(it, DateFormatKind.MESSAGE_DETAIL)
            val hasDivider = false
            buildContentItem(R.string.device_manager_session_details_session_last_activity, formattedDate, hasDivider)
        }
    }

    private fun hasSectionDevice(data: DeviceInfo): Boolean {
        return checkIfSectionDeviceIsVisibleUseCase.execute(data)
    }

    private fun buildSectionDevice(data: DeviceInfo, addExtraTopMargin: Boolean) {
        val lastSeenIp = data.lastSeenIp

        buildHeaderItem(R.string.device_manager_device_title, addExtraTopMargin)

        lastSeenIp?.let {
            val hasDivider = false
            buildContentItem(R.string.device_manager_session_details_device_ip_address, it, hasDivider)
        }
    }
}
