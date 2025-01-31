/*
 * Copyright 2020-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.crypto.verification.qrconfirmation

import com.airbnb.epoxy.EpoxyController
import im.vector.app.R
import im.vector.app.core.resources.ColorProvider
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.crypto.verification.epoxy.bottomSheetVerificationBigImageItem
import im.vector.app.features.crypto.verification.epoxy.bottomSheetVerificationNoticeItem
import im.vector.app.features.crypto.verification.epoxy.bottomSheetVerificationWaitingItem
import im.vector.lib.core.utils.epoxy.charsequence.toEpoxyCharSequence
import org.matrix.android.sdk.api.session.crypto.model.RoomEncryptionTrustLevel
import javax.inject.Inject

class VerificationQRWaitingController @Inject constructor(
        private val stringProvider: StringProvider,
        private val colorProvider: ColorProvider
) : EpoxyController() {

    private var args: VerificationQRWaitingFragment.Args? = null

    fun update(args: VerificationQRWaitingFragment.Args) {
        this.args = args
        requestModelBuild()
    }

    override fun buildModels() {
        val params = args ?: return
        val host = this

        bottomSheetVerificationNoticeItem {
            id("notice")
            apply {
                notice(host.stringProvider.getString(R.string.qr_code_scanned_verif_waiting_notice).toEpoxyCharSequence())
            }
        }

        bottomSheetVerificationBigImageItem {
            id("image")
            roomEncryptionTrustLevel(RoomEncryptionTrustLevel.Trusted)
        }

        bottomSheetVerificationWaitingItem {
            id("waiting")
            title(host.stringProvider.getString(R.string.qr_code_scanned_verif_waiting, params.otherUserName))
        }
    }
}
