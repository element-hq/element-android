/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.crypto.verification.epoxy

import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.ui.views.QrCodeImageView

/**
 * An Epoxy item displaying a QR code.
 */
@EpoxyModelClass
abstract class BottomSheetVerificationQrCodeItem : VectorEpoxyModel<BottomSheetVerificationQrCodeItem.Holder>(R.layout.item_verification_qr_code) {

    @EpoxyAttribute
    lateinit var data: String

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.qsrCodeImage.setData(data)
    }

    class Holder : VectorEpoxyHolder() {
        val qsrCodeImage by bind<QrCodeImageView>(R.id.itemVerificationQrCodeImage)
    }
}
