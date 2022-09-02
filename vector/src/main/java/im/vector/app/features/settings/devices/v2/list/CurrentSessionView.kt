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
import im.vector.app.databinding.ViewCurrentSessionBinding
import im.vector.app.features.settings.devices.DeviceFullInfo
import im.vector.app.features.themes.ThemeUtils
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel

class CurrentSessionView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val views: ViewCurrentSessionBinding

    init {
        inflate(context, R.layout.view_current_session, this)
        views = ViewCurrentSessionBinding.bind(this)
    }

    fun render(currentDeviceInfo: DeviceFullInfo) {
        renderDeviceInfo(currentDeviceInfo.deviceInfo.displayName.orEmpty())
        renderVerificationStatus(currentDeviceInfo.trustLevelForShield)
    }

    private fun renderVerificationStatus(trustLevelForShield: RoomEncryptionTrustLevel) {
        views.currentSessionVerificationStatusImageView.render(trustLevelForShield)
        if (trustLevelForShield == RoomEncryptionTrustLevel.Trusted) {
            renderCrossSigningVerified()
        } else {
            renderCrossSigningUnverified()
        }
    }

    private fun renderCrossSigningVerified() {
        views.currentSessionVerificationStatusTextView.text = context.getString(R.string.device_manager_verification_status_verified)
        views.currentSessionVerificationStatusTextView.setTextColor(ThemeUtils.getColor(context, R.attr.colorPrimary))
        views.currentSessionVerificationStatusDetailTextView.text = context.getString(R.string.device_manager_verification_status_detail_verified)
        views.currentSessionVerifySessionButton.isVisible = false
    }

    private fun renderCrossSigningUnverified() {
        views.currentSessionVerificationStatusTextView.text = context.getString(R.string.device_manager_verification_status_unverified)
        views.currentSessionVerificationStatusTextView.setTextColor(ThemeUtils.getColor(context, R.attr.colorError))
        views.currentSessionVerificationStatusDetailTextView.text = context.getString(R.string.device_manager_verification_status_detail_unverified)
        views.currentSessionVerifySessionButton.isVisible = true
    }

    // TODO. We don't have this info yet. Update later accordingly.
    private fun renderDeviceInfo(sessionName: String) {
        views.currentSessionDeviceTypeImageView.setImageResource(R.drawable.ic_device_type_mobile)
        views.currentSessionDeviceTypeImageView.contentDescription = context.getString(R.string.a11y_device_manager_device_type_mobile)
        views.currentSessionNameTextView.text = sessionName
    }
}
