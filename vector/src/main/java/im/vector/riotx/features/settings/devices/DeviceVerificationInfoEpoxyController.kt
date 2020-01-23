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
import im.vector.matrix.android.api.session.Session
import im.vector.riotx.R
import im.vector.riotx.core.epoxy.dividerItem
import im.vector.riotx.core.epoxy.loadingItem
import im.vector.riotx.core.resources.ColorProvider
import im.vector.riotx.core.resources.StringProvider
import im.vector.riotx.core.ui.list.GenericItem
import im.vector.riotx.core.ui.list.genericItem
import im.vector.riotx.features.crypto.verification.epoxy.bottomSheetVerificationActionItem
import im.vector.riotx.features.home.AvatarRenderer
import javax.inject.Inject

class DeviceVerificationInfoEpoxyController @Inject constructor(private val stringProvider: StringProvider,
                                                                private val colorProvider: ColorProvider,
                                                                private val session: Session,
                                                                private val avatarRender: AvatarRenderer)
    : TypedEpoxyController<DeviceVerificationInfoBottomSheetViewState>() {

    var callback: Callback? = null

    override fun buildModels(data: DeviceVerificationInfoBottomSheetViewState?) {
        val device = data?.cryptoDeviceInfo?.invoke()
        if (device == null) {
            loadingItem {
                id("loading")
            }
        } else {

            if (device.isVerified) {
                genericItem {
                    id("trust${device.deviceId}")
                    style(GenericItem.STYLE.BIG_TEXT)
                    titleIconResourceId(R.drawable.ic_shield_trusted)
                    title(stringProvider.getString(R.string.encryption_information_verified))
                    description(stringProvider.getString(R.string.settings_active_sessions_verified_device_desc))
                }
            } else {
                genericItem {
                    id("trust${device.deviceId}")
                    titleIconResourceId(R.drawable.ic_shield_warning)
                    style(GenericItem.STYLE.BIG_TEXT)
                    title(stringProvider.getString(R.string.encryption_information_not_verified))
                    description(stringProvider.getString(R.string.settings_active_sessions_unverified_device_desc))
                }
            }

            genericItem {
                id("info${device.deviceId}")
                title(device.displayName() ?: "")
                description("(${device.deviceId})")
            }

            if (!device.isVerified) {
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
                        callback?.onAction(DevicesAction.VerifyMyDevice(device.deviceId))
                    }
                }
            }

            if (device.deviceId != session.sessionParams.credentials.deviceId) {
                //Add the delete option
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
                        callback?.onAction(DevicesAction.Delete(device.deviceId))
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
                   callback?.onAction(DevicesAction.PromptRename(device.deviceId))
                }
            }
        }
    }

    interface Callback {
        fun onAction(action: DevicesAction)
    }
}
