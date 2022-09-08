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

import androidx.annotation.StringRes
import com.airbnb.epoxy.TypedEpoxyController
import im.vector.app.R
import im.vector.app.core.date.DateFormatKind
import im.vector.app.core.date.VectorDateFormatter
import im.vector.app.core.resources.StringProvider
import org.matrix.android.sdk.api.session.crypto.model.DeviceInfo
import javax.inject.Inject

class SessionDetailsController @Inject constructor(
        private val checkIfSectionSessionIsVisibleUseCase: CheckIfSectionSessionIsVisibleUseCase,
        private val checkIfSectionDeviceIsVisibleUseCase: CheckIfSectionDeviceIsVisibleUseCase,
        private val stringProvider: StringProvider,
        private val dateFormatter: VectorDateFormatter,
) : TypedEpoxyController<DeviceInfo>() {

    var callback: Callback? = null

    interface Callback {
        fun onItemLongClicked(content: String)
    }

    override fun buildModels(data: DeviceInfo?) {
        data?.let { info ->
            if (hasSectionSession(data)) {
                buildSectionSession(info)
            }

            if (hasSectionDevice(data)) {
                // TODO add a marginTop of 48dp if the session section is visible
                buildSectionDevice(info)
            }
        }
    }

    private fun buildHeaderItem(@StringRes titleResId: Int) {
        val host = this
        sessionDetailsHeaderItem {
            id(titleResId)
            title(host.stringProvider.getString(titleResId))
        }
    }

    private fun buildContentItem(@StringRes titleResId: Int, value: String) {
        val host = this
        sessionDetailsContentItem {
            id(titleResId)
            title(host.stringProvider.getString(titleResId))
            description(value)
        }
    }

    private fun hasSectionSession(data: DeviceInfo): Boolean {
        return checkIfSectionSessionIsVisibleUseCase.execute(data)
    }

    private fun buildSectionSession(data: DeviceInfo) {
        val sessionName = data.displayName
        val sessionId = data.deviceId
        val sessionLastSeenTs = data.lastSeenTs

        buildHeaderItem(R.string.device_manager_session_details_section_session_title)

        // TODO hide divider on the last visible item
        sessionName?.let {
            buildContentItem(R.string.device_manager_session_details_session_name, it)
        }
        sessionId?.let {
            buildContentItem(R.string.device_manager_session_details_session_id, it)
        }
        sessionLastSeenTs?.let {
            val formattedDate = dateFormatter.format(it, DateFormatKind.MESSAGE_DETAIL)
            buildContentItem(R.string.device_manager_session_details_session_last_activity, formattedDate)
        }
    }

    private fun hasSectionDevice(data: DeviceInfo): Boolean {
        return checkIfSectionDeviceIsVisibleUseCase.execute(data)
    }

    private fun buildSectionDevice(data: DeviceInfo) {
        val lastSeenIp = data.lastSeenIp

        buildHeaderItem(R.string.device_manager_session_details_section_device_title)

        // TODO hide divider on the last visible item
        lastSeenIp?.let {
            buildContentItem(R.string.device_manager_session_details_device_ip_address, it)
        }
    }
}
