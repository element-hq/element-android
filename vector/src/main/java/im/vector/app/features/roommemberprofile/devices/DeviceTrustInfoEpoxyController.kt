/*
 * Copyright 2020-2025 New Vector Ltd.
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
import im.vector.app.features.crypto.verification.epoxy.bottomSheetVerificationActionItem
import im.vector.app.features.settings.VectorPreferences
import im.vector.lib.core.utils.epoxy.charsequence.toEpoxyCharSequence
import me.gujun.android.span.span
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import javax.inject.Inject

class DeviceTrustInfoEpoxyController @Inject constructor(
        private val stringProvider: StringProvider,
        private val colorProvider: ColorProvider,
        private val dimensionConverter: DimensionConverter,
        private val vectorPreferences: VectorPreferences
) :
        TypedEpoxyController<DeviceListViewState>() {

    interface InteractionListener {
        fun onVerifyManually(device: CryptoDeviceInfo)
    }

    var interactionListener: InteractionListener? = null

    override fun buildModels(data: DeviceListViewState?) {
        val host = this
        data?.selectedDevice?.let { cryptoDeviceInfo ->
            val isVerified = cryptoDeviceInfo.trustLevel?.isVerified() == true
            genericItem {
                id("title")
                style(ItemStyle.BIG_TEXT)
                titleIconResourceId(if (isVerified) R.drawable.ic_shield_trusted else R.drawable.ic_shield_warning)
                title(
                        host.stringProvider
                                .getString(if (isVerified) R.string.verification_profile_verified else R.string.verification_profile_warning)
                                .toEpoxyCharSequence()
                )
            }
            genericFooterItem {
                id("desc")
                centered(false)
                textColor(host.colorProvider.getColorFromAttribute(R.attr.vctr_content_primary))
                apply {
                    if (isVerified) {
                        // TODO FORMAT
                        text(
                                host.stringProvider.getString(
                                        R.string.verification_profile_device_verified_because,
                                        data.userItem?.displayName ?: "",
                                        data.userItem?.id ?: ""
                                ).toEpoxyCharSequence()
                        )
                    } else {
                        // TODO what if mine
                        text(
                                host.stringProvider.getString(
                                        R.string.verification_profile_device_new_signing,
                                        data.userItem?.displayName ?: "",
                                        data.userItem?.id ?: ""
                                ).toEpoxyCharSequence()
                        )
                    }
                }
//                    text(stringProvider.getString(R.string.verification_profile_device_untrust_info))
            }

            genericWithValueItem {
                id(cryptoDeviceInfo.deviceId)
                titleIconResourceId(if (isVerified) R.drawable.ic_shield_trusted else R.drawable.ic_shield_warning)
                title(
                        span {
                            +(cryptoDeviceInfo.displayName() ?: "")
                            span {
                                text = " (${cryptoDeviceInfo.deviceId})"
                                textColor = host.colorProvider.getColorFromAttribute(R.attr.vctr_content_secondary)
                                textSize = host.dimensionConverter.spToPx(14)
                            }
                        }.toEpoxyCharSequence()
                )
            }

            if (!isVerified) {
                genericFooterItem {
                    id("warn")
                    centered(false)
                    textColor(host.colorProvider.getColorFromAttribute(R.attr.vctr_content_primary))
                    text(host.stringProvider.getString(R.string.verification_profile_device_untrust_info).toEpoxyCharSequence())
                }

                bottomSheetVerificationActionItem {
                    id("verify")
                    title(host.stringProvider.getString(R.string.cross_signing_verify_by_emoji))
                    titleColor(host.colorProvider.getColorFromAttribute(R.attr.colorPrimary))
                    iconRes(R.drawable.ic_arrow_right)
                    iconColor(host.colorProvider.getColorFromAttribute(R.attr.colorPrimary))
                    listener {
                        host.interactionListener?.onVerifyManually(cryptoDeviceInfo)
                    }
                }
            }
        }
    }
}
