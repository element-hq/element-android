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

package im.vector.riotx.features.crypto.verification.qrconfirmation

import com.airbnb.epoxy.EpoxyController
import im.vector.riotx.R
import im.vector.riotx.core.epoxy.dividerItem
import im.vector.riotx.core.resources.ColorProvider
import im.vector.riotx.core.resources.StringProvider
import im.vector.riotx.features.crypto.verification.epoxy.bottomSheetVerificationActionItem
import im.vector.riotx.features.crypto.verification.epoxy.bottomSheetVerificationNoticeItem
import javax.inject.Inject

class VerificationQrScannedByOtherController @Inject constructor(
        private val stringProvider: StringProvider,
        private val colorProvider: ColorProvider
) : EpoxyController() {

    var listener: Listener? = null

    init {
        requestModelBuild()
    }

    override fun buildModels() {
        bottomSheetVerificationNoticeItem {
            id("notice")
            notice(stringProvider.getString(R.string.qr_code_scanned_by_other_notice))
        }

        dividerItem {
            id("sep0")
        }

        bottomSheetVerificationActionItem {
            id("confirm")
            title(stringProvider.getString(R.string.qr_code_scanned_by_other_yes))
            titleColor(colorProvider.getColor(R.color.riotx_accent))
            iconRes(R.drawable.ic_check_on)
            iconColor(colorProvider.getColor(R.color.riotx_accent))
            listener { listener?.onUserConfirmsQrCodeScanned() }
        }

        dividerItem {
            id("sep1")
        }

        bottomSheetVerificationActionItem {
            id("deny")
            title(stringProvider.getString(R.string.qr_code_scanned_by_other_no))
            titleColor(colorProvider.getColor(R.color.vector_error_color))
            iconRes(R.drawable.ic_check_off)
            iconColor(colorProvider.getColor(R.color.vector_error_color))
            listener { listener?.onUserDeniesQrCodeScanned() }
        }
    }

    interface Listener {
        fun onUserConfirmsQrCodeScanned()
        fun onUserDeniesQrCodeScanned()
    }
}
