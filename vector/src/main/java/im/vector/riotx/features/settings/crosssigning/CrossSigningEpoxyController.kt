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
import im.vector.riotx.core.epoxy.loadingItem
import im.vector.riotx.core.resources.ColorProvider
import im.vector.riotx.core.resources.StringProvider
import im.vector.riotx.core.ui.list.genericItem
import im.vector.riotx.core.ui.list.genericItemWithValue
import im.vector.riotx.core.utils.DimensionConverter
import im.vector.riotx.features.crypto.verification.epoxy.bottomSheetVerificationActionItem
import me.gujun.android.span.span
import javax.inject.Inject

class CrossSigningEpoxyController @Inject constructor(
        private val stringProvider: StringProvider,
        private val colorProvider: ColorProvider,
        private val dimensionConverter: DimensionConverter
) : TypedEpoxyController<CrossSigningSettingsViewState>() {

    interface InteractionListener {
        fun onInitializeCrossSigningKeys()
        fun onResetCrossSigningKeys()
        fun verifySession()
    }

    var interactionListener: InteractionListener? = null

    override fun buildModels(data: CrossSigningSettingsViewState?) {
        if (data == null) return
        if (data.xSigningKeyCanSign) {
            genericItem {
                id("can")
                titleIconResourceId(R.drawable.ic_shield_trusted)
                title(stringProvider.getString(R.string.encryption_information_dg_xsigning_complete))
            }
            if (!data.isUploadingKeys) {
                bottomSheetVerificationActionItem {
                    id("resetkeys")
                    title("Reset keys")
                    titleColor(colorProvider.getColor(R.color.riotx_destructive_accent))
                    iconRes(R.drawable.ic_arrow_right)
                    iconColor(colorProvider.getColor(R.color.riotx_destructive_accent))
                    listener {
                        interactionListener?.onResetCrossSigningKeys()
                    }
                }
            }
        } else if (data.xSigningKeysAreTrusted) {
            genericItem {
                id("trusted")
                titleIconResourceId(R.drawable.ic_shield_custom)
                title(stringProvider.getString(R.string.encryption_information_dg_xsigning_trusted))
            }
            if (!data.isUploadingKeys) {
                bottomSheetVerificationActionItem {
                    id("resetkeys")
                    title("Reset keys")
                    titleColor(colorProvider.getColor(R.color.riotx_destructive_accent))
                    iconRes(R.drawable.ic_arrow_right)
                    iconColor(colorProvider.getColor(R.color.riotx_destructive_accent))
                    listener {
                        interactionListener?.onResetCrossSigningKeys()
                    }
                }

                bottomSheetVerificationActionItem {
                    id("verify")
                    title(stringProvider.getString(R.string.complete_security))
                    titleColor(colorProvider.getColor(R.color.riotx_positive_accent))
                    iconRes(R.drawable.ic_arrow_right)
                    iconColor(colorProvider.getColor(R.color.riotx_positive_accent))
                    listener {
                        interactionListener?.verifySession()
                    }
                }
            }
        } else if (data.xSigningIsEnableInAccount) {
            genericItem {
                id("enable")
                titleIconResourceId(R.drawable.ic_shield_black)
                title(stringProvider.getString(R.string.encryption_information_dg_xsigning_not_trusted))
            }
            bottomSheetVerificationActionItem {
                id("verify")
                title(stringProvider.getString(R.string.complete_security))
                titleColor(colorProvider.getColor(R.color.riotx_positive_accent))
                iconRes(R.drawable.ic_arrow_right)
                iconColor(colorProvider.getColor(R.color.riotx_positive_accent))
                listener {
                    interactionListener?.verifySession()
                }
            }
            bottomSheetVerificationActionItem {
                id("resetkeys")
                title("Reset keys")
                titleColor(colorProvider.getColor(R.color.riotx_destructive_accent))
                iconRes(R.drawable.ic_arrow_right)
                iconColor(colorProvider.getColor(R.color.riotx_destructive_accent))
                listener {
                    interactionListener?.onResetCrossSigningKeys()
                }
            }
        } else {
            genericItem {
                id("not")
                title(stringProvider.getString(R.string.encryption_information_dg_xsigning_disabled))
            }
            if (!data.isUploadingKeys) {
                bottomSheetVerificationActionItem {
                    id("initKeys")
                    title("Initialize keys")
                    titleColor(colorProvider.getColor(R.color.riotx_positive_accent))
                    iconRes(R.drawable.ic_arrow_right)
                    iconColor(colorProvider.getColor(R.color.riotx_positive_accent))
                    listener {
                        interactionListener?.onInitializeCrossSigningKeys()
                    }
                }
            }
        }

        if (data.isUploadingKeys) {
            loadingItem {
                id("loading")
            }
        } else {
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
}
