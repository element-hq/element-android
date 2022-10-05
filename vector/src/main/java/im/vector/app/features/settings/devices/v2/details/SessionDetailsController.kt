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

package im.vector.app.features.settings.devices.v2.details

import android.view.View
import androidx.annotation.StringRes
import com.airbnb.epoxy.TypedEpoxyController
import im.vector.app.R
import im.vector.app.core.date.DateFormatKind
import im.vector.app.core.date.VectorDateFormatter
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.session.clientinfo.MatrixClientInfoContent
import im.vector.app.core.utils.DimensionConverter
import im.vector.app.features.settings.devices.v2.DeviceFullInfo
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

            if (hasSectionDevice(deviceInfo)) {
                buildSectionDevice(deviceInfo, addExtraTopMargin = hasSectionSession || hasApplicationSection)
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

        buildHeaderItem(R.string.device_manager_session_title)

        if (sessionName.isNotEmpty()) {
            val hasDivider = sessionId.isNotEmpty() || sessionLastSeenTs > 0
            buildContentItem(R.string.device_manager_session_details_session_name, sessionName, hasDivider)
        }
        if (sessionId.isNotEmpty()) {
            val hasDivider = sessionLastSeenTs > 0
            buildContentItem(R.string.device_manager_session_details_session_id, sessionId, hasDivider)
        }
        if (sessionLastSeenTs > 0) {
            val formattedDate = dateFormatter.format(sessionLastSeenTs, DateFormatKind.MESSAGE_DETAIL)
            val hasDivider = false
            buildContentItem(R.string.device_manager_session_details_session_last_activity, formattedDate, hasDivider)
        }
    }

    private fun hasSectionApplication(matrixClientInfoContent: MatrixClientInfoContent?): Boolean {
        return checkIfSectionApplicationIsVisibleUseCase.execute(matrixClientInfoContent)
    }

    private fun buildSectionApplication(matrixClientInfoContent: MatrixClientInfoContent, addExtraTopMargin: Boolean) {
        val name = matrixClientInfoContent.name.orEmpty()
        val version = matrixClientInfoContent.version.orEmpty()
        val url = matrixClientInfoContent.url.orEmpty()

        buildHeaderItem(R.string.device_manager_session_details_application, addExtraTopMargin)

        if (name.isNotEmpty()) {
            val hasDivider = version.isNotEmpty() || url.isNotEmpty()
            buildContentItem(R.string.device_manager_session_details_application_name, name, hasDivider)
        }
        if (version.isNotEmpty()) {
            val hasDivider = url.isNotEmpty()
            buildContentItem(R.string.device_manager_session_details_application_version, version, hasDivider)
        }
        if (url.isNotEmpty()) {
            val hasDivider = false
            buildContentItem(R.string.device_manager_session_details_application_url, url, hasDivider)
        }
    }

    private fun hasSectionDevice(data: DeviceInfo): Boolean {
        return checkIfSectionDeviceIsVisibleUseCase.execute(data)
    }

    private fun buildSectionDevice(data: DeviceInfo, addExtraTopMargin: Boolean) {
        val lastSeenIp = data.lastSeenIp.orEmpty()

        buildHeaderItem(R.string.device_manager_device_title, addExtraTopMargin)

        if (lastSeenIp.isNotEmpty()) {
            val hasDivider = false
            buildContentItem(R.string.device_manager_session_details_device_ip_address, lastSeenIp, hasDivider)
        }
    }
}
