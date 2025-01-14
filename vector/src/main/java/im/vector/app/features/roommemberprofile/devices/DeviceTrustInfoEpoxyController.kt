/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.roommemberprofile.devices

import com.airbnb.epoxy.TypedEpoxyController
import im.vector.app.R
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.ui.list.ItemStyle
import im.vector.app.core.ui.list.genericFooterItem
import im.vector.app.core.ui.list.genericItem
import im.vector.app.core.ui.list.genericWithValueItem
import im.vector.app.core.utils.DimensionConverter
import im.vector.app.features.settings.devices.TrustUtils
import im.vector.lib.core.utils.epoxy.charsequence.toEpoxyCharSequence
import im.vector.lib.strings.CommonStrings
import me.gujun.android.span.span
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel
import javax.inject.Inject

class DeviceTrustInfoEpoxyController @Inject constructor(
        private val stringProvider: StringProvider,
        private val colorProvider: ColorProvider,
        private val dimensionConverter: DimensionConverter,
) :
        TypedEpoxyController<DeviceListViewState>() {

    interface InteractionListener

    var interactionListener: InteractionListener? = null

    override fun buildModels(data: DeviceListViewState?) {
        val host = this
        data?.selectedDevice?.let { cryptoDeviceInfo ->
            val trustMSK = data.memberCrossSigningKey?.isTrusted().orFalse()
            val legacyMode = data.memberCrossSigningKey == null
            val isMyDevice = data.userId == data.myUserId && data.myDeviceId == cryptoDeviceInfo.deviceId
            val trustLevel = TrustUtils.shieldForTrust(
                    isMyDevice,
                    trustMSK,
                    legacyMode,
                    cryptoDeviceInfo.trustLevel
            )
            val isVerified = trustLevel == RoomEncryptionTrustLevel.Trusted
            val shield = when (trustLevel) {
                RoomEncryptionTrustLevel.Default -> R.drawable.ic_shield_unknown
                RoomEncryptionTrustLevel.Warning -> R.drawable.ic_shield_warning
                RoomEncryptionTrustLevel.Trusted -> R.drawable.ic_shield_trusted
                RoomEncryptionTrustLevel.E2EWithUnsupportedAlgorithm -> R.drawable.ic_warning_badge
            }
            genericItem {
                id("title")
                style(ItemStyle.BIG_TEXT)
                titleIconResourceId(shield)
                title(
                        host.stringProvider
                                .getString(if (isVerified) CommonStrings.verification_profile_verified else CommonStrings.verification_profile_warning)
                                .toEpoxyCharSequence()
                )
            }
            genericFooterItem {
                id("desc")
                centered(false)
                textColor(host.colorProvider.getColorFromAttribute(im.vector.lib.ui.styles.R.attr.vctr_content_primary))
                apply {
                    if (isVerified) {
                        // TODO FORMAT
                        text(
                                host.stringProvider.getString(
                                        CommonStrings.verification_profile_device_verified_because,
                                        data.userItem?.displayName ?: "",
                                        data.userItem?.id ?: ""
                                ).toEpoxyCharSequence()
                        )
                    } else {
                        // TODO what if mine
                        text(
                                host.stringProvider.getString(
                                        CommonStrings.verification_profile_device_new_signing,
                                        data.userItem?.displayName ?: "",
                                        data.userItem?.id ?: ""
                                ).toEpoxyCharSequence()
                        )
                    }
                }
//                    text(stringProvider.getString(CommonStrings.verification_profile_device_untrust_info))
            }

            genericWithValueItem {
                id(cryptoDeviceInfo.deviceId)
                titleIconResourceId(shield)
                title(
                        span {
                            +(cryptoDeviceInfo.displayName() ?: "")
                            span {
                                text = " (${cryptoDeviceInfo.deviceId})"
                                textColor = host.colorProvider.getColorFromAttribute(im.vector.lib.ui.styles.R.attr.vctr_content_secondary)
                                textSize = host.dimensionConverter.spToPx(14)
                            }
                        }.toEpoxyCharSequence()
                )
            }

            if (!isVerified && !isMyDevice) {
                genericFooterItem {
                    id("warn")
                    centered(false)
                    textColor(host.colorProvider.getColorFromAttribute(im.vector.lib.ui.styles.R.attr.vctr_content_primary))
                    text(host.stringProvider.getString(CommonStrings.verification_profile_other_device_untrust_info).toEpoxyCharSequence())
                }
            }
        }
    }
}
