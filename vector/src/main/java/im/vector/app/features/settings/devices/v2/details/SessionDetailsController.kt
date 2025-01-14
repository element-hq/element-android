/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings.devices.v2.details

import android.view.View
import androidx.annotation.StringRes
import com.airbnb.epoxy.TypedEpoxyController
import im.vector.app.core.date.DateFormatKind
import im.vector.app.core.date.VectorDateFormatter
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.session.clientinfo.MatrixClientInfoContent
import im.vector.app.core.utils.DimensionConverter
import im.vector.app.features.settings.devices.v2.DeviceFullInfo
import im.vector.app.features.settings.devices.v2.list.DeviceType
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.session.crypto.model.DeviceInfo
import javax.inject.Inject

class SessionDetailsController @Inject constructor(
        private val checkIfSectionSessionIsVisibleUseCase: CheckIfSectionSessionIsVisibleUseCase,
        private val checkIfSectionDeviceIsVisibleUseCase: CheckIfSectionDeviceIsVisibleUseCase,
        private val checkIfSectionApplicationIsVisibleUseCase: CheckIfSectionApplicationIsVisibleUseCase,
        private val stringProvider: StringProvider,
        private val dateFormatter: VectorDateFormatter,
        private val dimensionConverter: DimensionConverter,
) : TypedEpoxyController<DeviceFullInfo>() {

    var callback: Callback? = null

    interface Callback {
        fun onItemLongClicked(content: String)
    }

    override fun buildModels(data: DeviceFullInfo?) {
        data?.let { fullInfo ->
            val deviceInfo = fullInfo.deviceInfo
            val matrixClientInfo = fullInfo.matrixClientInfo
            val hasSectionSession = hasSectionSession(deviceInfo)
            if (hasSectionSession) {
                buildSectionSession(deviceInfo)
            }

            val hasApplicationSection = hasSectionApplication(matrixClientInfo)
            if (hasApplicationSection && matrixClientInfo != null) {
                buildSectionApplication(matrixClientInfo, addExtraTopMargin = hasSectionSession)
            }

            if (hasSectionDevice(fullInfo)) {
                buildSectionDevice(fullInfo, addExtraTopMargin = hasSectionSession || hasApplicationSection)
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
        val sessionName = data.displayName.orEmpty()
        val sessionId = data.deviceId.orEmpty()
        val sessionLastSeenTs = data.lastSeenTs ?: -1

        buildHeaderItem(CommonStrings.device_manager_session_title)

        if (sessionName.isNotEmpty()) {
            val hasDivider = sessionId.isNotEmpty() || sessionLastSeenTs > 0
            buildContentItem(CommonStrings.device_manager_session_details_session_name, sessionName, hasDivider)
        }
        if (sessionId.isNotEmpty()) {
            val hasDivider = sessionLastSeenTs > 0
            buildContentItem(CommonStrings.device_manager_session_details_session_id, sessionId, hasDivider)
        }
        if (sessionLastSeenTs > 0) {
            val formattedDate = dateFormatter.format(sessionLastSeenTs, DateFormatKind.MESSAGE_DETAIL)
            val hasDivider = false
            buildContentItem(CommonStrings.device_manager_session_details_session_last_activity, formattedDate, hasDivider)
        }
    }

    private fun hasSectionApplication(matrixClientInfoContent: MatrixClientInfoContent?): Boolean {
        return checkIfSectionApplicationIsVisibleUseCase.execute(matrixClientInfoContent)
    }

    private fun buildSectionApplication(matrixClientInfoContent: MatrixClientInfoContent, addExtraTopMargin: Boolean) {
        val name = matrixClientInfoContent.name.orEmpty()
        val version = matrixClientInfoContent.version.orEmpty()
        val url = matrixClientInfoContent.url.orEmpty()

        buildHeaderItem(CommonStrings.device_manager_session_details_application, addExtraTopMargin)

        if (name.isNotEmpty()) {
            val hasDivider = version.isNotEmpty() || url.isNotEmpty()
            buildContentItem(CommonStrings.device_manager_session_details_application_name, name, hasDivider)
        }
        if (version.isNotEmpty()) {
            val hasDivider = url.isNotEmpty()
            buildContentItem(CommonStrings.device_manager_session_details_application_version, version, hasDivider)
        }
        if (url.isNotEmpty()) {
            val hasDivider = false
            buildContentItem(CommonStrings.device_manager_session_details_application_url, url, hasDivider)
        }
    }

    private fun hasSectionDevice(data: DeviceFullInfo): Boolean {
        return checkIfSectionDeviceIsVisibleUseCase.execute(data)
    }

    private fun buildSectionDevice(data: DeviceFullInfo, addExtraTopMargin: Boolean) {
        buildHeaderItem(CommonStrings.device_manager_device_title, addExtraTopMargin)

        when (data.deviceExtendedInfo.deviceType) {
            DeviceType.MOBILE -> buildSectionDeviceMobile(data)
            DeviceType.WEB -> buildSectionDeviceWeb(data)
            DeviceType.DESKTOP -> buildSectionDeviceDesktop(data)
            DeviceType.UNKNOWN -> buildSectionDeviceUnknown(data)
        }
    }

    private fun buildSectionDeviceWeb(data: DeviceFullInfo) {
        val browserName = data.deviceExtendedInfo.clientName.orEmpty()
        val browserVersion = data.deviceExtendedInfo.clientVersion.orEmpty()
        val browser = "$browserName $browserVersion"
        val operatingSystem = data.deviceExtendedInfo.deviceOperatingSystem.orEmpty()
        val lastSeenIp = data.deviceInfo.lastSeenIp.orEmpty()

        if (browser.isNotEmpty()) {
            val hasDivider = operatingSystem.isNotEmpty() || lastSeenIp.isNotEmpty()
            buildContentItem(CommonStrings.device_manager_session_details_device_browser, browser, hasDivider)
        }

        if (operatingSystem.isNotEmpty()) {
            val hasDivider = lastSeenIp.isNotEmpty()
            buildContentItem(CommonStrings.device_manager_session_details_device_operating_system, operatingSystem, hasDivider)
        }

        buildIpAddressContentItem(lastSeenIp)
    }

    private fun buildSectionDeviceDesktop(data: DeviceFullInfo) {
        val operatingSystem = data.deviceExtendedInfo.deviceOperatingSystem.orEmpty()
        val lastSeenIp = data.deviceInfo.lastSeenIp.orEmpty()

        if (operatingSystem.isNotEmpty()) {
            val hasDivider = lastSeenIp.isNotEmpty()
            buildContentItem(CommonStrings.device_manager_session_details_device_operating_system, operatingSystem, hasDivider)
        }

        buildIpAddressContentItem(lastSeenIp)
    }

    private fun buildSectionDeviceMobile(data: DeviceFullInfo) {
        val model = data.deviceExtendedInfo.deviceModel.orEmpty()
        val operatingSystem = data.deviceExtendedInfo.deviceOperatingSystem.orEmpty()
        val lastSeenIp = data.deviceInfo.lastSeenIp.orEmpty()

        if (model.isNotEmpty()) {
            val hasDivider = operatingSystem.isNotEmpty() || lastSeenIp.isNotEmpty()
            buildContentItem(CommonStrings.device_manager_session_details_device_model, model, hasDivider)
        }

        if (operatingSystem.isNotEmpty()) {
            val hasDivider = lastSeenIp.isNotEmpty()
            buildContentItem(CommonStrings.device_manager_session_details_device_operating_system, operatingSystem, hasDivider)
        }

        buildIpAddressContentItem(lastSeenIp)
    }

    private fun buildSectionDeviceUnknown(data: DeviceFullInfo) {
        val lastSeenIp = data.deviceInfo.lastSeenIp.orEmpty()
        buildIpAddressContentItem(lastSeenIp)
    }

    private fun buildIpAddressContentItem(lastSeenIp: String) {
        if (lastSeenIp.isNotEmpty()) {
            val hasDivider = false
            buildContentItem(CommonStrings.device_manager_session_details_device_ip_address, lastSeenIp, hasDivider)
        }
    }
}
