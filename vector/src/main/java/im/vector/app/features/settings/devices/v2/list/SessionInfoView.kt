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

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import im.vector.app.R
import im.vector.app.databinding.ViewSessionInfoBinding
import im.vector.app.features.settings.devices.DeviceFullInfo
import im.vector.app.features.themes.ThemeUtils
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel

class SessionInfoView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val views: ViewSessionInfoBinding

    init {
        inflate(context, R.layout.view_session_info, this)
        views = ViewSessionInfoBinding.bind(this)
    }

    val viewDetailsButton = views.sessionInfoViewDetailsButton

    fun render(deviceInfo: DeviceFullInfo) {
        renderDeviceInfo(deviceInfo.deviceInfo.displayName.orEmpty())
        renderVerificationStatus(deviceInfo.trustLevelForShield)
    }

    private fun renderVerificationStatus(trustLevelForShield: RoomEncryptionTrustLevel) {
        views.sessionInfoVerificationStatusImageView.render(trustLevelForShield)
        if (trustLevelForShield == RoomEncryptionTrustLevel.Trusted) {
            renderCrossSigningVerified()
        } else {
            renderCrossSigningUnverified()
        }
    }

    private fun renderCrossSigningVerified() {
        views.sessionInfoVerificationStatusTextView.text = context.getString(R.string.device_manager_verification_status_verified)
        views.sessionInfoVerificationStatusTextView.setTextColor(ThemeUtils.getColor(context, R.attr.colorPrimary))
        views.sessionInfoVerificationStatusDetailTextView.text = context.getString(R.string.device_manager_verification_status_detail_verified)
        views.sessionInfoVerifySessionButton.isVisible = false
    }

    private fun renderCrossSigningUnverified() {
        views.sessionInfoVerificationStatusTextView.text = context.getString(R.string.device_manager_verification_status_unverified)
        views.sessionInfoVerificationStatusTextView.setTextColor(ThemeUtils.getColor(context, R.attr.colorError))
        views.sessionInfoVerificationStatusDetailTextView.text = context.getString(R.string.device_manager_verification_status_detail_unverified)
        views.sessionInfoVerifySessionButton.isVisible = true
    }

    // TODO. We don't have this info yet. Update later accordingly.
    private fun renderDeviceInfo(sessionName: String) {
        views.sessionInfoDeviceTypeImageView.setImageResource(R.drawable.ic_device_type_mobile)
        views.sessionInfoDeviceTypeImageView.contentDescription = context.getString(R.string.a11y_device_manager_device_type_mobile)
        views.sessionInfoNameTextView.text = sessionName
    }
}
