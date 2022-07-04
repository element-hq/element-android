/*
 * Copyright 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package im.vector.app.features.roommemberprofile.devices

import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import im.vector.app.R
import im.vector.app.core.epoxy.errorWithRetryItem
import im.vector.app.core.epoxy.loadingItem
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.core.ui.list.ItemStyle
import im.vector.app.core.ui.list.genericFooterItem
import im.vector.app.core.ui.list.genericItem
import im.vector.app.core.ui.list.genericWithValueItem
import im.vector.app.core.utils.DimensionConverter
import im.vector.app.features.settings.VectorPreferences
import im.vector.lib.core.utils.epoxy.charsequence.toEpoxyCharSequence
import me.gujun.android.span.span
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import javax.inject.Inject

class DeviceListEpoxyController @Inject constructor(
        private val stringProvider: StringProvider,
        private val colorProvider: ColorProvider,
        private val dimensionConverter: DimensionConverter,
        private val vectorPreferences: VectorPreferences
) :
        TypedEpoxyController<DeviceListViewState>() {

    interface InteractionListener {
        fun onDeviceSelected(device: CryptoDeviceInfo)
    }

    var interactionListener: InteractionListener? = null

    override fun buildModels(data: DeviceListViewState?) {
        data ?: return
        val host = this
        when (data.cryptoDevices) {
            Uninitialized -> {
            }
            is Loading -> {
                loadingItem {
                    id("loading")
                    loadingText(host.stringProvider.getString(R.string.loading))
                }
            }
            is Success -> {
                val deviceList = data.cryptoDevices.invoke().sortedBy {
                    it.isVerified
                }

                // Build top header
                val allGreen = deviceList.fold(true, { prev, device ->
                    prev && device.isVerified
                })

                genericItem {
                    id("title")
                    style(ItemStyle.BIG_TEXT)
                    titleIconResourceId(if (allGreen) R.drawable.ic_shield_trusted else R.drawable.ic_shield_warning)
                    title(
                            host.stringProvider
                                    .getString(if (allGreen) R.string.verification_profile_verified else R.string.verification_profile_warning)
                                    .toEpoxyCharSequence()
                    )
                    description(host.stringProvider.getString(R.string.verification_conclusion_ok_notice).toEpoxyCharSequence())
                }

                if (vectorPreferences.developerMode()) {
                    // Display the cross signing keys
                    addDebugInfo(data)
                }

                genericItem {
                    id("sessions")
                    style(ItemStyle.BIG_TEXT)
                    title(host.stringProvider.getString(R.string.room_member_profile_sessions_section_title).toEpoxyCharSequence())
                }
                if (deviceList.isEmpty()) {
                    // Can this really happen?
                    genericFooterItem {
                        id("empty")
                        text(host.stringProvider.getString(R.string.search_no_results).toEpoxyCharSequence())
                    }
                } else {
                    // Build list of device with status
                    deviceList.forEach { device ->
                        genericWithValueItem {
                            id(device.deviceId)
                            titleIconResourceId(if (device.isVerified) R.drawable.ic_shield_trusted else R.drawable.ic_shield_warning)
                            apply {
                                val title = if (host.vectorPreferences.developerMode()) {
                                    val seq = span {
                                        +(device.displayName() ?: device.deviceId)
                                        +"\n"
                                        span {
                                            text = "(${device.deviceId})"
                                            textColor = host.colorProvider.getColorFromAttribute(R.attr.vctr_content_secondary)
                                            textSize = host.dimensionConverter.spToPx(14)
                                        }
                                    }
                                    seq
                                } else {
                                    device.displayName() ?: device.deviceId
                                }
                                title(title.toEpoxyCharSequence())
                            }
                            value(
                                    host.stringProvider.getString(
                                            if (device.isVerified) R.string.trusted else R.string.not_trusted
                                    )
                            )
                            valueColorInt(
                                    host.colorProvider.getColorFromAttribute(
                                            if (device.isVerified) R.attr.colorPrimary else R.attr.colorError
                                    )
                            )
                            itemClickAction {
                                host.interactionListener?.onDeviceSelected(device)
                            }
                        }
                    }
                }
            }
            is Fail -> {
                errorWithRetryItem {
                    id("error")
                    text(host.stringProvider.getString(R.string.room_member_profile_failed_to_get_devices))
                    listener {
                        // TODO
                    }
                }
            }
        }
    }

    private fun addDebugInfo(data: DeviceListViewState) {
        val host = this
        data.memberCrossSigningKey?.masterKey()?.let {
            genericWithValueItem {
                id("msk")
                titleIconResourceId(R.drawable.key_small)
                title(
                        span {
                            +"Master Key:\n"
                            span {
                                text = it.unpaddedBase64PublicKey ?: ""
                                textColor = host.colorProvider.getColorFromAttribute(R.attr.vctr_content_secondary)
                                textSize = host.dimensionConverter.spToPx(12)
                            }
                        }.toEpoxyCharSequence()
                )
            }
        }
        data.memberCrossSigningKey?.userKey()?.let {
            genericWithValueItem {
                id("usk")
                titleIconResourceId(R.drawable.key_small)
                title(
                        span {
                            +"User Key:\n"
                            span {
                                text = it.unpaddedBase64PublicKey ?: ""
                                textColor = host.colorProvider.getColorFromAttribute(R.attr.vctr_content_secondary)
                                textSize = host.dimensionConverter.spToPx(12)
                            }
                        }.toEpoxyCharSequence()
                )
            }
        }
        data.memberCrossSigningKey?.selfSigningKey()?.let {
            genericWithValueItem {
                id("ssk")
                titleIconResourceId(R.drawable.key_small)
                title(
                        span {
                            +"Self Signed Key:\n"
                            span {
                                text = it.unpaddedBase64PublicKey ?: ""
                                textColor = host.colorProvider.getColorFromAttribute(R.attr.vctr_content_secondary)
                                textSize = host.dimensionConverter.spToPx(12)
                            }
                        }.toEpoxyCharSequence()
                )
            }
        }
    }
}
