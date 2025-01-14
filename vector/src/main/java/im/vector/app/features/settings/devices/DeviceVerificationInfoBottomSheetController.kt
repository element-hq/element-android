/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.settings.devices

import com.airbnb.epoxy.TypedEpoxyController
import im.vector.app.R
import im.vector.app.core.epoxy.bottomSheetDividerItem
import im.vector.app.core.epoxy.loadingItem
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.ui.list.ItemStyle
import im.vector.app.core.ui.list.genericFooterItem
import im.vector.app.core.ui.list.genericItem
import im.vector.app.core.ui.views.toDrawableRes
import im.vector.app.features.crypto.verification.epoxy.bottomSheetVerificationActionItem
import im.vector.lib.core.utils.epoxy.charsequence.toEpoxyCharSequence
import im.vector.lib.strings.CommonStrings
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import timber.log.Timber
import javax.inject.Inject

class DeviceVerificationInfoBottomSheetController @Inject constructor(
        private val stringProvider: StringProvider,
        private val colorProvider: ColorProvider
) :
        TypedEpoxyController<DeviceVerificationInfoBottomSheetViewState>() {

    var callback: Callback? = null

    override fun buildModels(data: DeviceVerificationInfoBottomSheetViewState?) {
        val cryptoDeviceInfo = data?.cryptoDeviceInfo?.invoke()
        when {
            cryptoDeviceInfo != null -> {
                // It's a E2E capable device
                handleE2ECapableDevice(data, cryptoDeviceInfo)
            }
            data?.deviceInfo?.invoke() != null -> {
                // It's a non E2E capable device
                handleNonE2EDevice(data)
            }
            else -> {
                loadingItem {
                    id("loading")
                }
            }
        }
    }

    private fun handleE2ECapableDevice(data: DeviceVerificationInfoBottomSheetViewState, cryptoDeviceInfo: CryptoDeviceInfo) {
        val shield = TrustUtils.shieldForTrust(
                currentDevice = data.isMine,
                trustMSK = data.accountCrossSigningIsTrusted,
                legacyMode = !data.hasAccountCrossSigning,
                deviceTrustLevel = cryptoDeviceInfo.trustLevel
        ).toDrawableRes()

        if (data.hasAccountCrossSigning) {
            // Cross Signing is enabled
            handleE2EWithCrossSigning(data, cryptoDeviceInfo, shield)
        } else {
            handleE2EInLegacy(data, cryptoDeviceInfo, shield)
        }

        // COMMON ACTIONS (Rename / signout)
        addGenericDeviceManageActions(data, cryptoDeviceInfo.deviceId)
    }

    private fun handleE2EWithCrossSigning(data: DeviceVerificationInfoBottomSheetViewState, cryptoDeviceInfo: CryptoDeviceInfo, shield: Int) {
        val isMine = data.isMine
        val currentSessionIsTrusted = data.accountCrossSigningIsTrusted
        Timber.v("handleE2EWithCrossSigning $isMine, $cryptoDeviceInfo, $shield")
        val host = this

        if (isMine) {
            if (currentSessionIsTrusted) {
                genericItem {
                    id("trust${cryptoDeviceInfo.deviceId}")
                    style(ItemStyle.BIG_TEXT)
                    titleIconResourceId(shield)
                    title(host.stringProvider.getString(CommonStrings.encryption_information_verified).toEpoxyCharSequence())
                    description(host.stringProvider.getString(CommonStrings.settings_active_sessions_verified_device_desc).toEpoxyCharSequence())
                }
            } else if (data.canVerifySession) {
                // You need to complete security, only if there are other session(s) available, or if 4S contains secrets
                genericItem {
                    id("trust${cryptoDeviceInfo.deviceId}")
                    style(ItemStyle.BIG_TEXT)
                    titleIconResourceId(shield)
                    title(host.stringProvider.getString(CommonStrings.crosssigning_verify_this_session).toEpoxyCharSequence())
                    description(
                            host.stringProvider
                                    .getString(if (data.hasOtherSessions) CommonStrings.confirm_your_identity else CommonStrings.confirm_your_identity_quad_s)
                                    .toEpoxyCharSequence()
                    )
                }
            } else {
                genericItem {
                    id("reset${cryptoDeviceInfo.deviceId}")
                    style(ItemStyle.BIG_TEXT)
                    titleIconResourceId(shield)
                    title(host.stringProvider.getString(CommonStrings.crosssigning_cannot_verify_this_session).toEpoxyCharSequence())
                    description(
                            host.stringProvider
                                    .getString(CommonStrings.crosssigning_cannot_verify_this_session_desc)
                                    .toEpoxyCharSequence()
                    )
                }
            }
        } else {
            if (!currentSessionIsTrusted) {
                // we don't know if this session is trusted...
                // for now we show nothing?
            } else {
                // we rely on cross signing status
                val trust = cryptoDeviceInfo.trustLevel?.isCrossSigningVerified() == true
                if (trust) {
                    genericItem {
                        id("trust${cryptoDeviceInfo.deviceId}")
                        style(ItemStyle.BIG_TEXT)
                        titleIconResourceId(shield)
                        title(host.stringProvider.getString(CommonStrings.encryption_information_verified).toEpoxyCharSequence())
                        description(host.stringProvider.getString(CommonStrings.settings_active_sessions_verified_device_desc).toEpoxyCharSequence())
                    }
                } else {
                    genericItem {
                        id("trust${cryptoDeviceInfo.deviceId}")
                        titleIconResourceId(shield)
                        style(ItemStyle.BIG_TEXT)
                        title(host.stringProvider.getString(CommonStrings.encryption_information_not_verified).toEpoxyCharSequence())
                        description(host.stringProvider.getString(CommonStrings.settings_active_sessions_unverified_device_desc).toEpoxyCharSequence())
                    }
                }
            }
        }

        // DEVICE INFO SECTION
        genericItem {
            id("info${cryptoDeviceInfo.deviceId}")
            title(cryptoDeviceInfo.displayName().orEmpty().toEpoxyCharSequence())
            description("(${cryptoDeviceInfo.deviceId})".toEpoxyCharSequence())
        }

        if (isMine) {
            if (!currentSessionIsTrusted) {
                if (data.canVerifySession) {
                    // Add complete security
                    bottomSheetDividerItem {
                        id("completeSecurityDiv")
                    }
                    bottomSheetVerificationActionItem {
                        id("completeSecurity")
                        title(host.stringProvider.getString(CommonStrings.crosssigning_verify_this_session))
                        titleColor(host.colorProvider.getColorFromAttribute(com.google.android.material.R.attr.colorPrimary))
                        iconRes(R.drawable.ic_arrow_right)
                        iconColor(host.colorProvider.getColorFromAttribute(com.google.android.material.R.attr.colorPrimary))
                        listener {
                            host.callback?.onAction(DevicesAction.CompleteSecurity)
                        }
                    }
                } else {
                    bottomSheetDividerItem {
                        id("resetSecurityDiv")
                    }
                    bottomSheetVerificationActionItem {
                        id("resetSecurity")
                        title(host.stringProvider.getString(CommonStrings.secure_backup_reset_all))
                        titleColor(host.colorProvider.getColorFromAttribute(com.google.android.material.R.attr.colorPrimary))
                        iconRes(R.drawable.ic_arrow_right)
                        iconColor(host.colorProvider.getColorFromAttribute(com.google.android.material.R.attr.colorPrimary))
                        listener {
                            host.callback?.onAction(DevicesAction.ResetSecurity)
                        }
                    }
                }
            }
        } else
        /** if (!isMine) **/
        {
            if (currentSessionIsTrusted) {
                // we can propose to verify it
                val isVerified = cryptoDeviceInfo.trustLevel?.crossSigningVerified.orFalse()
                if (!isVerified) {
                    addVerifyActions(cryptoDeviceInfo)
                }
            }
        }
    }

    private fun handleE2EInLegacy(data: DeviceVerificationInfoBottomSheetViewState, cryptoDeviceInfo: CryptoDeviceInfo, shield: Int) {
        val host = this
        // ==== Legacy
        val isMine = data.isMine

        // TRUST INFO SECTION
        if (cryptoDeviceInfo.trustLevel?.isLocallyVerified() == true) {
            genericItem {
                id("trust${cryptoDeviceInfo.deviceId}")
                style(ItemStyle.BIG_TEXT)
                titleIconResourceId(shield)
                title(host.stringProvider.getString(CommonStrings.encryption_information_verified).toEpoxyCharSequence())
                description(host.stringProvider.getString(CommonStrings.settings_active_sessions_verified_device_desc).toEpoxyCharSequence())
            }
        } else {
            genericItem {
                id("trust${cryptoDeviceInfo.deviceId}")
                titleIconResourceId(shield)
                style(ItemStyle.BIG_TEXT)
                title(host.stringProvider.getString(CommonStrings.encryption_information_not_verified).toEpoxyCharSequence())
                description(host.stringProvider.getString(CommonStrings.settings_active_sessions_unverified_device_desc).toEpoxyCharSequence())
            }
        }

        // DEVICE INFO SECTION
        genericItem {
            id("info${cryptoDeviceInfo.deviceId}")
            title(cryptoDeviceInfo.displayName().orEmpty().toEpoxyCharSequence())
            description("(${cryptoDeviceInfo.deviceId})".toEpoxyCharSequence())
        }

        // ACTIONS

        if (!isMine) {
            // if it's not the current device you can trigger a verification
            bottomSheetDividerItem {
                id("d1")
            }
            bottomSheetVerificationActionItem {
                id("verify${cryptoDeviceInfo.deviceId}")
                title(host.stringProvider.getString(CommonStrings.verification_verify_device))
                titleColor(host.colorProvider.getColorFromAttribute(com.google.android.material.R.attr.colorPrimary))
                iconRes(R.drawable.ic_arrow_right)
                iconColor(host.colorProvider.getColorFromAttribute(com.google.android.material.R.attr.colorPrimary))
                listener {
                    host.callback?.onAction(DevicesAction.VerifyMyDevice(cryptoDeviceInfo.deviceId))
                }
            }
        }
    }

    private fun addVerifyActions(cryptoDeviceInfo: CryptoDeviceInfo) {
        val host = this
        bottomSheetDividerItem {
            id("verifyDiv")
        }
        bottomSheetVerificationActionItem {
            id("verify_text")
            title(host.stringProvider.getString(CommonStrings.cross_signing_verify_by_text))
            titleColor(host.colorProvider.getColorFromAttribute(com.google.android.material.R.attr.colorPrimary))
            iconRes(R.drawable.ic_arrow_right)
            iconColor(host.colorProvider.getColorFromAttribute(com.google.android.material.R.attr.colorPrimary))
            listener {
                host.callback?.onAction(DevicesAction.VerifyMyDeviceManually(cryptoDeviceInfo.deviceId))
            }
        }
        bottomSheetDividerItem {
            id("verifyDiv2")
        }
        bottomSheetVerificationActionItem {
            id("verify_emoji")
            title(host.stringProvider.getString(CommonStrings.cross_signing_verify_by_emoji))
            titleColor(host.colorProvider.getColorFromAttribute(com.google.android.material.R.attr.colorPrimary))
            iconRes(R.drawable.ic_arrow_right)
            iconColor(host.colorProvider.getColorFromAttribute(com.google.android.material.R.attr.colorPrimary))
            listener {
                host.callback?.onAction(DevicesAction.VerifyMyDevice(cryptoDeviceInfo.deviceId))
            }
        }
    }

    private fun addGenericDeviceManageActions(data: DeviceVerificationInfoBottomSheetViewState, deviceId: String) {
        val host = this
        // Offer delete session if not me
        if (!data.isMine) {
            // Add the delete option
            bottomSheetDividerItem {
                id("manageD1")
            }
            bottomSheetVerificationActionItem {
                id("delete")
                title(host.stringProvider.getString(CommonStrings.settings_active_sessions_signout_device))
                titleColor(host.colorProvider.getColorFromAttribute(com.google.android.material.R.attr.colorError))
                iconRes(R.drawable.ic_arrow_right)
                iconColor(host.colorProvider.getColorFromAttribute(com.google.android.material.R.attr.colorError))
                listener {
                    host.callback?.onAction(DevicesAction.Delete(deviceId))
                }
            }
        }

        // Always offer rename
        bottomSheetDividerItem {
            id("manageD2")
        }
        bottomSheetVerificationActionItem {
            id("rename")
            title(host.stringProvider.getString(CommonStrings.action_rename))
            titleColor(host.colorProvider.getColorFromAttribute(im.vector.lib.ui.styles.R.attr.vctr_content_primary))
            iconRes(R.drawable.ic_arrow_right)
            iconColor(host.colorProvider.getColorFromAttribute(im.vector.lib.ui.styles.R.attr.vctr_content_primary))
            listener {
                host.callback?.onAction(DevicesAction.PromptRename(deviceId))
            }
        }
    }

    private fun handleNonE2EDevice(data: DeviceVerificationInfoBottomSheetViewState) {
        val host = this
        val info = data.deviceInfo.invoke() ?: return
        genericItem {
            id("info${info.deviceId}")
            title(info.displayName.orEmpty().toEpoxyCharSequence())
            description("(${info.deviceId})".toEpoxyCharSequence())
        }

        genericFooterItem {
            id("infoCrypto${info.deviceId}")
            text(host.stringProvider.getString(CommonStrings.settings_failed_to_get_crypto_device_info).toEpoxyCharSequence())
        }

        info.deviceId?.let { addGenericDeviceManageActions(data, it) }
    }

    interface Callback {
        fun onAction(action: DevicesAction)
    }
}
