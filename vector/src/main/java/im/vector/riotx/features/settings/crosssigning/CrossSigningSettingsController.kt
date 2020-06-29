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
 */
package im.vector.riotx.features.settings.crosssigning

import com.airbnb.epoxy.TypedEpoxyController
import im.vector.riotx.R
import im.vector.riotx.core.resources.ColorProvider
import im.vector.riotx.core.resources.StringProvider
import im.vector.riotx.core.ui.list.genericItem
import im.vector.riotx.core.ui.list.genericItemWithValue
import im.vector.riotx.core.utils.DimensionConverter
import im.vector.riotx.features.crypto.verification.epoxy.bottomSheetVerificationActionItem
import me.gujun.android.span.span
import javax.inject.Inject

class CrossSigningSettingsController @Inject constructor(
        private val stringProvider: StringProvider,
        private val colorProvider: ColorProvider,
        private val dimensionConverter: DimensionConverter
) : TypedEpoxyController<CrossSigningSettingsViewState>() {

    interface InteractionListener {
        fun setupRecovery()
        fun verifySession()
        fun initCrossSigning()
    }

    var interactionListener: InteractionListener? = null

    override fun buildModels(data: CrossSigningSettingsViewState?) {
        if (data == null) return
        when {
            data.xSigningKeyCanSign        -> {
                genericItem {
                    id("can")
                    titleIconResourceId(R.drawable.ic_shield_trusted)
                    title(stringProvider.getString(R.string.encryption_information_dg_xsigning_complete))
                }
            }
            data.xSigningKeysAreTrusted    -> {
                genericItem {
                    id("trusted")
                    titleIconResourceId(R.drawable.ic_shield_custom)
                    title(stringProvider.getString(R.string.encryption_information_dg_xsigning_trusted))
                }
            }
            data.xSigningIsEnableInAccount -> {
                genericItem {
                    id("enable")
                    titleIconResourceId(R.drawable.ic_shield_black)
                    title(stringProvider.getString(R.string.encryption_information_dg_xsigning_not_trusted))
                }
            }
            else                           -> {
                genericItem {
                    id("not")
                    title(stringProvider.getString(R.string.encryption_information_dg_xsigning_disabled))
                }
            }
        }

        if (data.recoveryHasToBeSetUp) {
            if (data.xSigningIsEnableInAccount) {
                bottomSheetVerificationActionItem {
                    id("setup_recovery")
                    title(stringProvider.getString(R.string.settings_setup_secure_backup))
                    titleColor(colorProvider.getColorFromAttribute(R.attr.riotx_text_primary))
                    iconRes(R.drawable.ic_arrow_right)
                    listener {
                        interactionListener?.setupRecovery()
                    }
                }
            } else {
                // Propose to setup cross signing
                bottomSheetVerificationActionItem {
                    id("setup_xSgning")
                    title(stringProvider.getString(R.string.setup_cross_signing))
                    titleColor(colorProvider.getColorFromAttribute(R.attr.riotx_text_primary))
                    subTitle(stringProvider.getString(R.string.security_prompt_text))
                    iconRes(R.drawable.ic_arrow_right)
                    listener {
                        interactionListener?.initCrossSigning()
                    }
                }
            }
        }

        if (data.deviceHasToBeVerified) {
            bottomSheetVerificationActionItem {
                id("verify")
                title(stringProvider.getString(R.string.crosssigning_verify_this_session))
                titleColor(colorProvider.getColor(R.color.riotx_positive_accent))
                iconRes(R.drawable.ic_arrow_right)
                iconColor(colorProvider.getColor(R.color.riotx_positive_accent))
                listener {
                    interactionListener?.verifySession()
                }
            }
        }

        val crossSigningKeys = data.crossSigningInfo

        crossSigningKeys?.masterKey()?.let {
            genericItemWithValue {
                id("msk")
                titleIconResourceId(R.drawable.key_small)
                title(
                        span {
                            +"Master Key:\n"
                            span {
                                text = it.unpaddedBase64PublicKey ?: ""
                                textColor = colorProvider.getColorFromAttribute(R.attr.riotx_text_secondary)
                                textSize = dimensionConverter.spToPx(12)
                            }
                        }
                )
            }
        }
        crossSigningKeys?.userKey()?.let {
            genericItemWithValue {
                id("usk")
                titleIconResourceId(R.drawable.key_small)
                title(
                        span {
                            +"User Key:\n"
                            span {
                                text = it.unpaddedBase64PublicKey ?: ""
                                textColor = colorProvider.getColorFromAttribute(R.attr.riotx_text_secondary)
                                textSize = dimensionConverter.spToPx(12)
                            }
                        }
                )
            }
        }
        crossSigningKeys?.selfSigningKey()?.let {
            genericItemWithValue {
                id("ssk")
                titleIconResourceId(R.drawable.key_small)
                title(
                        span {
                            +"Self Signed Key:\n"
                            span {
                                text = it.unpaddedBase64PublicKey ?: ""
                                textColor = colorProvider.getColorFromAttribute(R.attr.riotx_text_secondary)
                                textSize = dimensionConverter.spToPx(12)
                            }
                        }
                )
            }
        }
    }
}
