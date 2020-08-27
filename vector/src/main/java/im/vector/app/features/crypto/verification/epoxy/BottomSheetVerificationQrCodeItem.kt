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
package im.vector.app.features.crypto.verification.epoxy

import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.ui.views.QrCodeImageView

/**
 * An Epoxy item displaying a QR code
 */
@EpoxyModelClass(layout = R.layout.item_verification_qr_code)
abstract class BottomSheetVerificationQrCodeItem : VectorEpoxyModel<BottomSheetVerificationQrCodeItem.Holder>() {

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
