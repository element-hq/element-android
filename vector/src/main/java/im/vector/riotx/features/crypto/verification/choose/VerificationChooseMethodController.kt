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

package im.vector.riotx.features.crypto.verification.choose

import com.airbnb.epoxy.EpoxyController
import im.vector.riotx.R
import im.vector.riotx.core.epoxy.bottomsheet.bottomSheetSeparatorItem
import im.vector.riotx.core.resources.ColorProvider
import im.vector.riotx.core.resources.StringProvider
import im.vector.riotx.features.crypto.verification.epoxy.bottomSheetVerificationActionItem
import im.vector.riotx.features.crypto.verification.epoxy.bottomSheetVerificationBigImageItem
import im.vector.riotx.features.crypto.verification.epoxy.bottomSheetVerificationNoticeItem
import javax.inject.Inject

class VerificationChooseMethodController @Inject constructor(
        private val stringProvider: StringProvider,
        private val colorProvider: ColorProvider
) : EpoxyController() {

    var listener: Listener? = null

    private var viewState: VerificationChooseMethodViewState? = null

    init {
        // We are requesting a model build directly as the first build of epoxy is on the main thread.
        // It avoids to build the whole list on the main thread.
        requestModelBuild()
    }

    fun update(viewState: VerificationChooseMethodViewState) {
        this.viewState = viewState
        requestModelBuild()
    }

    override fun buildModels() {
        val state = viewState ?: return

        if (state.QRModeAvailable) {
            bottomSheetVerificationNoticeItem {
                id("notice")
                notice(stringProvider.getString(R.string.verification_scan_notice))
            }

            // TODO Generate the QR code
            bottomSheetVerificationBigImageItem {
                id("qr")
                imageRes(R.drawable.riotx_logo)
            }

            bottomSheetSeparatorItem {
                id("sep0")
            }

            bottomSheetVerificationActionItem {
                id("openCamera")
                title(stringProvider.getString(R.string.verification_scan_their_code))
                titleColor(colorProvider.getColor(R.color.riotx_accent))
                iconRes(R.drawable.ic_camera)
                iconColor(colorProvider.getColor(R.color.riotx_accent))
                listener { listener?.openCamera() }
            }

            bottomSheetSeparatorItem {
                id("sep1")
            }

            bottomSheetVerificationActionItem {
                id("openEmoji")
                title(stringProvider.getString(R.string.verification_scan_emoji_title))
                titleColor(colorProvider.getColorFromAttribute(R.attr.riotx_text_primary))
                subTitle(stringProvider.getString(R.string.verification_scan_emoji_subtitle))
                iconRes(R.drawable.ic_arrow_right)
                iconColor(colorProvider.getColorFromAttribute(R.attr.riotx_text_primary))
                listener { listener?.doVerifyBySas() }
            }
        } else if (state.SASModeAvailable) {
            bottomSheetVerificationActionItem {
                id("openEmoji")
                title(stringProvider.getString(R.string.verification_no_scan_emoji_title))
                titleColor(colorProvider.getColor(R.color.riotx_accent))
                iconRes(R.drawable.ic_arrow_right)
                iconColor(colorProvider.getColorFromAttribute(R.attr.riotx_text_primary))
                listener { listener?.doVerifyBySas() }
            }
        }
    }

    interface Listener {
        fun openCamera()
        fun doVerifyBySas()
    }
}
