/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
import im.vector.app.core.extensions.setTextOrHide
import im.vector.app.core.extensions.setTextWithColoredPart
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.DrawableProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.databinding.ViewSessionInfoBinding
import im.vector.app.features.themes.ThemeUtils
import im.vector.lib.strings.CommonPlurals
import im.vector.lib.strings.CommonStrings
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
    val viewVerifyButton = views.sessionInfoVerifySessionButton

    private val setDeviceTypeIconUseCase = SetDeviceTypeIconUseCase()

    fun render(
            sessionInfoViewState: SessionInfoViewState,
            dateFormatter: VectorDateFormatter,
            drawableProvider: DrawableProvider,
            colorProvider: ColorProvider,
            stringProvider: StringProvider,
    ) {
        renderDeviceInfo(
                sessionName = sessionInfoViewState.deviceFullInfo.deviceInfo.displayName
                        ?: sessionInfoViewState.deviceFullInfo.deviceInfo.deviceId.orEmpty(),
                deviceType = sessionInfoViewState.deviceFullInfo.deviceExtendedInfo.deviceType,
                stringProvider = stringProvider,
        )
        renderVerificationStatus(
                sessionInfoViewState.deviceFullInfo.roomEncryptionTrustLevel,
                sessionInfoViewState.isCurrentSession,
                sessionInfoViewState.isLearnMoreLinkVisible,
                sessionInfoViewState.isVerifyButtonVisible,
        )
        renderDeviceLastSeenDetails(
                sessionInfoViewState.deviceFullInfo.isInactive,
                sessionInfoViewState.deviceFullInfo.deviceInfo,
                sessionInfoViewState.isLastActivityVisible,
                sessionInfoViewState.isShowingIpAddress,
                dateFormatter,
                drawableProvider,
                colorProvider,
        )
        renderDetailsButton(sessionInfoViewState.isDetailsButtonVisible)
    }

    private fun renderVerificationStatus(
            encryptionTrustLevel: RoomEncryptionTrustLevel?,
            isCurrentSession: Boolean,
            hasLearnMoreLink: Boolean,
            isVerifyButtonVisible: Boolean,
    ) {
        views.sessionInfoVerificationStatusImageView.renderDeviceShield(encryptionTrustLevel)
        when {
            encryptionTrustLevel == null -> renderCrossSigningEncryptionNotSupported()
            encryptionTrustLevel == RoomEncryptionTrustLevel.Trusted -> renderCrossSigningVerified(isCurrentSession)
            encryptionTrustLevel == RoomEncryptionTrustLevel.Default && !isCurrentSession -> renderCrossSigningUnknown()
            else -> renderCrossSigningUnverified(isCurrentSession, isVerifyButtonVisible)
        }
        if (hasLearnMoreLink) {
            appendLearnMoreToVerificationStatus()
        }
    }

    private fun appendLearnMoreToVerificationStatus() {
        val status = views.sessionInfoVerificationStatusDetailTextView.text
        val learnMore = context.getString(CommonStrings.action_learn_more)
        val statusText = buildString {
            append(status)
            append(" ")
            append(learnMore)
        }

        views.sessionInfoVerificationStatusDetailTextView.setTextWithColoredPart(
                fullText = statusText,
                coloredPart = learnMore,
                underline = false
        ) {
            onLearnMoreClickListener?.invoke()
        }
    }

    private fun renderCrossSigningVerified(isCurrentSession: Boolean) {
        views.sessionInfoVerificationStatusTextView.text = context.getString(CommonStrings.device_manager_verification_status_verified)
        views.sessionInfoVerificationStatusTextView.setTextColor(ThemeUtils.getColor(context, com.google.android.material.R.attr.colorPrimary))
        val statusResId = if (isCurrentSession) {
            CommonStrings.device_manager_verification_status_detail_current_session_verified
        } else {
            CommonStrings.device_manager_verification_status_detail_other_session_verified
        }
        views.sessionInfoVerificationStatusDetailTextView.text = context.getString(statusResId)
        views.sessionInfoVerifySessionButton.isVisible = false
    }

    private fun renderCrossSigningUnverified(isCurrentSession: Boolean, isVerifyButtonVisible: Boolean) {
        views.sessionInfoVerificationStatusTextView.text = context.getString(CommonStrings.device_manager_verification_status_unverified)
        views.sessionInfoVerificationStatusTextView.setTextColor(ThemeUtils.getColor(context, com.google.android.material.R.attr.colorError))
        val statusResId = if (isCurrentSession) {
            CommonStrings.device_manager_verification_status_detail_current_session_unverified
        } else {
            CommonStrings.device_manager_verification_status_detail_other_session_unverified
        }
        views.sessionInfoVerificationStatusDetailTextView.text = context.getString(statusResId)
        views.sessionInfoVerifySessionButton.isVisible = isVerifyButtonVisible
    }

    private fun renderCrossSigningUnknown() {
        views.sessionInfoVerificationStatusTextView.text = context.getString(CommonStrings.device_manager_verification_status_unknown)
        views.sessionInfoVerificationStatusDetailTextView.text = context.getString(
                CommonStrings.device_manager_verification_status_detail_other_session_unknown
        )
        views.sessionInfoVerifySessionButton.isVisible = false
    }

    private fun renderCrossSigningEncryptionNotSupported() {
        views.sessionInfoVerificationStatusTextView.text = context.getString(CommonStrings.device_manager_verification_status_unverified)
        views.sessionInfoVerificationStatusTextView.setTextColor(ThemeUtils.getColor(context, com.google.android.material.R.attr.colorError))
        views.sessionInfoVerificationStatusDetailTextView.text =
                context.getString(CommonStrings.device_manager_verification_status_detail_session_encryption_not_supported)
        views.sessionInfoVerifySessionButton.isVisible = false
    }

    private fun renderDeviceInfo(sessionName: String, deviceType: DeviceType, stringProvider: StringProvider) {
        setDeviceTypeIconUseCase.execute(deviceType, views.sessionInfoDeviceTypeImageView, stringProvider)
        views.sessionInfoNameTextView.text = sessionName
    }

    private fun renderDeviceLastSeenDetails(
            isInactive: Boolean,
            deviceInfo: DeviceInfo,
            isLastSeenDetailsVisible: Boolean,
            isShowingIpAddress: Boolean,
            dateFormatter: VectorDateFormatter,
            drawableProvider: DrawableProvider,
            colorProvider: ColorProvider,
    ) {
        if (deviceInfo.lastSeenTs != null && isLastSeenDetailsVisible) {
            val timestamp = deviceInfo.lastSeenTs
            views.sessionInfoLastActivityTextView.isVisible = true
            views.sessionInfoLastActivityTextView.text = if (isInactive) {
                val formattedTs = dateFormatter.format(timestamp, DateFormatKind.TIMELINE_DAY_DIVIDER)
                context.resources.getQuantityString(
                        CommonPlurals.device_manager_other_sessions_description_inactive,
                        SESSION_IS_MARKED_AS_INACTIVE_AFTER_DAYS,
                        SESSION_IS_MARKED_AS_INACTIVE_AFTER_DAYS,
                        formattedTs
                )
            } else {
                val formattedTs = dateFormatter.format(timestamp, DateFormatKind.DEFAULT_DATE_AND_TIME)
                context.getString(CommonStrings.device_manager_session_last_activity, formattedTs)
            }
            val drawable = if (isInactive) {
                val drawableColor = colorProvider.getColorFromAttribute(im.vector.lib.ui.styles.R.attr.vctr_content_secondary)
                drawableProvider.getDrawable(R.drawable.ic_inactive_sessions, drawableColor)
            } else {
                null
            }
            views.sessionInfoLastActivityTextView.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
        } else {
            views.sessionInfoLastActivityTextView.isGone = true
        }
        views.sessionInfoLastIPAddressTextView.setTextOrHide(deviceInfo.lastSeenIp?.takeIf { isShowingIpAddress })
    }

    private fun renderDetailsButton(isDetailsButtonVisible: Boolean) {
        views.sessionInfoViewDetailsButton.isVisible = isDetailsButtonVisible
    }
}
