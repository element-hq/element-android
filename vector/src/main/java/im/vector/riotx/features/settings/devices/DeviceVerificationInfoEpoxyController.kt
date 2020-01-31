/*
 * Copyright 2020 New Vector Ltd
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
package im.vector.riotx.features.settings.devices

import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.mvrx.Fail
import im.vector.matrix.android.api.session.Session
import im.vector.riotx.R
import im.vector.riotx.core.epoxy.dividerItem
import im.vector.riotx.core.epoxy.loadingItem
import im.vector.riotx.core.resources.ColorProvider
import im.vector.riotx.core.resources.StringProvider
import im.vector.riotx.core.ui.list.GenericItem
import im.vector.riotx.core.ui.list.genericFooterItem
import im.vector.riotx.core.ui.list.genericItem
import im.vector.riotx.features.crypto.verification.epoxy.bottomSheetVerificationActionItem
import javax.inject.Inject

class DeviceVerificationInfoEpoxyController @Inject constructor(private val stringProvider: StringProvider,
                                                                private val colorProvider: ColorProvider,
                                                                private val session: Session)
    : TypedEpoxyController<DeviceVerificationInfoBottomSheetViewState>() {

    var callback: Callback? = null

    override fun buildModels(data: DeviceVerificationInfoBottomSheetViewState?) {
        val cryptoDeviceInfo = data?.cryptoDeviceInfo?.invoke()
        if (cryptoDeviceInfo != null) {
            if (cryptoDeviceInfo.isVerified) {
                genericItem {
                    id("trust${cryptoDeviceInfo.deviceId}")
                    style(GenericItem.STYLE.BIG_TEXT)
                    titleIconResourceId(R.drawable.ic_shield_trusted)
                    title(stringProvider.getString(R.string.encryption_information_verified))
                    description(stringProvider.getString(R.string.settings_active_sessions_verified_device_desc))
                }
            } else {
                genericItem {
                    id("trust${cryptoDeviceInfo.deviceId}")
                    titleIconResourceId(R.drawable.ic_shield_warning)
                    style(GenericItem.STYLE.BIG_TEXT)
                    title(stringProvider.getString(R.string.encryption_information_not_verified))
                    description(stringProvider.getString(R.string.settings_active_sessions_unverified_device_desc))
                }
            }

            genericItem {
                id("info${cryptoDeviceInfo.deviceId}")
                title(cryptoDeviceInfo.displayName() ?: "")
                description("(${cryptoDeviceInfo.deviceId})")
            }

            if (!cryptoDeviceInfo.isVerified) {
                dividerItem {
                    id("d1")
                }
                bottomSheetVerificationActionItem {
                    id("verify")
                    title(stringProvider.getString(R.string.verification_verify_device))
                    titleColor(colorProvider.getColor(R.color.riotx_accent))
                    iconRes(R.drawable.ic_arrow_right)
                    iconColor(colorProvider.getColor(R.color.riotx_accent))
                    listener {
                        callback?.onAction(DevicesAction.VerifyMyDevice(cryptoDeviceInfo.deviceId))
                    }
                }
            }

            if (cryptoDeviceInfo.deviceId != session.sessionParams.credentials.deviceId) {
                // Add the delete option
                dividerItem {
                    id("d2")
                }
                bottomSheetVerificationActionItem {
                    id("delete")
                    title(stringProvider.getString(R.string.settings_active_sessions_signout_device))
                    titleColor(colorProvider.getColor(R.color.riotx_destructive_accent))
                    iconRes(R.drawable.ic_arrow_right)
                    iconColor(colorProvider.getColor(R.color.riotx_destructive_accent))
                    listener {
                        callback?.onAction(DevicesAction.Delete(cryptoDeviceInfo.deviceId))
                    }
                }
            }

            dividerItem {
                id("d3")
            }
            bottomSheetVerificationActionItem {
                id("rename")
                title(stringProvider.getString(R.string.rename))
                titleColor(colorProvider.getColorFromAttribute(R.attr.riotx_text_primary))
                iconRes(R.drawable.ic_arrow_right)
                iconColor(colorProvider.getColorFromAttribute(R.attr.riotx_text_primary))
                listener {
                    callback?.onAction(DevicesAction.PromptRename(cryptoDeviceInfo.deviceId))
                }
            }
        } else if (data?.deviceInfo?.invoke() != null) {
            val info = data.deviceInfo.invoke()
            genericItem {
                id("info${info?.deviceId}")
                title(info?.displayName ?: "")
                description("(${info?.deviceId})")
            }

            genericFooterItem {
                id("infoCrypto${info?.deviceId}")
                text(stringProvider.getString(R.string.settings_failed_to_get_crypto_device_info))
            }

            if (info?.deviceId != session.sessionParams.credentials.deviceId) {
                // Add the delete option
                dividerItem {
                    id("d2")
                }
                bottomSheetVerificationActionItem {
                    id("delete")
                    title(stringProvider.getString(R.string.settings_active_sessions_signout_device))
                    titleColor(colorProvider.getColor(R.color.riotx_destructive_accent))
                    iconRes(R.drawable.ic_arrow_right)
                    iconColor(colorProvider.getColor(R.color.riotx_destructive_accent))
                    listener {
                        callback?.onAction(DevicesAction.Delete(info?.deviceId ?: ""))
                    }
                }
            }
        } else {
            loadingItem {
                id("loading")
            }
        }
    }

    interface Callback {
        fun onAction(action: DevicesAction)
    }
}
