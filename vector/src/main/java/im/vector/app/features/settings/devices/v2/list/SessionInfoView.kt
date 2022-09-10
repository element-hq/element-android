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
import androidx.core.view.isGone
import androidx.core.view.isVisible
import im.vector.app.R
import im.vector.app.core.date.DateFormatKind
import im.vector.app.core.date.VectorDateFormatter
import im.vector.app.core.extensions.setTextWithColoredPart
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.DrawableProvider
import im.vector.app.databinding.ViewSessionInfoBinding
import im.vector.app.features.themes.ThemeUtils
import org.matrix.android.sdk.api.session.crypto.model.DeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel

class SessionInfoView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val views: ViewSessionInfoBinding

    var onLearnMoreClickListener: (() -> Unit)? = null

    init {
        inflate(context, R.layout.view_session_info, this)
        views = ViewSessionInfoBinding.bind(this)
    }

    val viewDetailsButton = views.sessionInfoViewDetailsButton

    fun render(
            sessionInfoViewState: SessionInfoViewState,
            dateFormatter: VectorDateFormatter,
            drawableProvider: DrawableProvider,
            colorProvider: ColorProvider,
    ) {
        renderDeviceInfo(sessionInfoViewState.deviceFullInfo.deviceInfo.displayName.orEmpty())
        renderVerificationStatus(
                sessionInfoViewState.deviceFullInfo.roomEncryptionTrustLevel,
                sessionInfoViewState.isCurrentSession,
                sessionInfoViewState.isLearnMoreLinkVisible,
        )
        renderDeviceLastSeenDetails(
                sessionInfoViewState.deviceFullInfo.isInactive,
                sessionInfoViewState.deviceFullInfo.deviceInfo,
                sessionInfoViewState.isLastSeenDetailsVisible,
                dateFormatter,
                drawableProvider,
                colorProvider,
        )
        renderDetailsButton(sessionInfoViewState.isDetailsButtonVisible)
    }

    private fun renderVerificationStatus(
            encryptionTrustLevel: RoomEncryptionTrustLevel,
            isCurrentSession: Boolean,
            hasLearnMoreLink: Boolean,
    ) {
        views.sessionInfoVerificationStatusImageView.render(encryptionTrustLevel)
        if (encryptionTrustLevel == RoomEncryptionTrustLevel.Trusted) {
            renderCrossSigningVerified(isCurrentSession)
        } else {
            renderCrossSigningUnverified(isCurrentSession)
        }
        if (hasLearnMoreLink) {
            appendLearnMoreToVerificationStatus()
        }
    }

    private fun appendLearnMoreToVerificationStatus() {
        val status = views.sessionInfoVerificationStatusDetailTextView.text
        val learnMore = context.getString(R.string.action_learn_more)
        val stringBuilder = StringBuilder()
        stringBuilder.append(status)
        stringBuilder.append(" ")
        stringBuilder.append(learnMore)

        views.sessionInfoVerificationStatusDetailTextView.setTextWithColoredPart(
                fullText = stringBuilder.toString(),
                coloredPart = learnMore,
                underline = false
        ) {
            onLearnMoreClickListener?.invoke()
        }
    }

    private fun renderCrossSigningVerified(isCurrentSession: Boolean) {
        views.sessionInfoVerificationStatusTextView.text = context.getString(R.string.device_manager_verification_status_verified)
        views.sessionInfoVerificationStatusTextView.setTextColor(ThemeUtils.getColor(context, R.attr.colorPrimary))
        val statusResId = if (isCurrentSession) {
            R.string.device_manager_verification_status_detail_current_session_verified
        } else {
            R.string.device_manager_verification_status_detail_other_session_verified
        }
        views.sessionInfoVerificationStatusDetailTextView.text = context.getString(statusResId)
        views.sessionInfoVerifySessionButton.isVisible = false
    }

    private fun renderCrossSigningUnverified(isCurrentSession: Boolean) {
        views.sessionInfoVerificationStatusTextView.text = context.getString(R.string.device_manager_verification_status_unverified)
        views.sessionInfoVerificationStatusTextView.setTextColor(ThemeUtils.getColor(context, R.attr.colorError))
        val statusResId = if (isCurrentSession) {
            R.string.device_manager_verification_status_detail_current_session_unverified
        } else {
            R.string.device_manager_verification_status_detail_other_session_unverified
        }
        views.sessionInfoVerificationStatusDetailTextView.text = context.getString(statusResId)
        views.sessionInfoVerifySessionButton.isVisible = true
    }

    // TODO. We don't have this info yet. Update later accordingly.
    private fun renderDeviceInfo(sessionName: String) {
        views.sessionInfoDeviceTypeImageView.setImageResource(R.drawable.ic_device_type_mobile)
        views.sessionInfoDeviceTypeImageView.contentDescription = context.getString(R.string.a11y_device_manager_device_type_mobile)
        views.sessionInfoNameTextView.text = sessionName
    }

    private fun renderDeviceLastSeenDetails(
            isInactive: Boolean,
            deviceInfo: DeviceInfo,
            isLastSeenDetailsVisible: Boolean,
            dateFormatter: VectorDateFormatter,
            drawableProvider: DrawableProvider,
            colorProvider: ColorProvider,
    ) {
        deviceInfo.lastSeenTs
                ?.takeIf { isLastSeenDetailsVisible }
                ?.let { timestamp ->
                    views.sessionInfoLastActivityTextView.isVisible = true
                    views.sessionInfoLastActivityTextView.text = if (isInactive) {
                        val formattedTs = dateFormatter.format(timestamp, DateFormatKind.TIMELINE_DAY_DIVIDER)
                        context.resources.getQuantityString(
                                R.plurals.device_manager_other_sessions_description_inactive,
                                SESSION_IS_MARKED_AS_INACTIVE_AFTER_DAYS,
                                SESSION_IS_MARKED_AS_INACTIVE_AFTER_DAYS,
                                formattedTs
                        )
                    } else {
                        val formattedTs = dateFormatter.format(timestamp, DateFormatKind.DEFAULT_DATE_AND_TIME)
                        context.getString(R.string.device_manager_session_last_activity, formattedTs)
                    }
                    val drawable = if (isInactive) {
                        val drawableColor = colorProvider.getColorFromAttribute(R.attr.vctr_content_secondary)
                        drawableProvider.getDrawable(R.drawable.ic_inactive_sessions, drawableColor)
                    } else {
                        null
                    }
                    views.sessionInfoLastActivityTextView.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
                }
                ?: run {
                    views.sessionInfoLastActivityTextView.isGone = true
                }

        deviceInfo.lastSeenIp
                ?.takeIf { isLastSeenDetailsVisible }
                ?.let { ipAddress ->
                    views.sessionInfoLastIPAddressTextView.isVisible = true
                    views.sessionInfoLastIPAddressTextView.text = ipAddress
                }
                ?: run {
                    views.sessionInfoLastIPAddressTextView.isGone = true
                }
    }

    private fun renderDetailsButton(isDetailsButtonVisible: Boolean) {
        views.sessionInfoViewDetailsButton.isVisible = isDetailsButtonVisible
    }
}
