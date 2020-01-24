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
package im.vector.riotx.features.crypto.verification.epoxy

import androidx.core.view.ViewCompat
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.riotx.R
import im.vector.riotx.core.epoxy.VectorEpoxyHolder
import im.vector.riotx.core.epoxy.VectorEpoxyModel
import im.vector.riotx.core.ui.views.QrCodeImageView

/**
 * A action for bottom sheet.
 */
@EpoxyModelClass(layout = R.layout.item_verification_qr_code)
abstract class BottomSheetVerificationQrCodeItem : VectorEpoxyModel<BottomSheetVerificationQrCodeItem.Holder>() {

    @EpoxyAttribute
    lateinit var data: String

    @EpoxyAttribute
    var animate = false

    @EpoxyAttribute
    var contentDescription: String? = null

    override fun bind(holder: Holder) {
        holder.qsrCodeImage.setData(data, animate)

        if (contentDescription == null) {
            ViewCompat.setImportantForAccessibility(holder.qsrCodeImage, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO)
        } else {
            ViewCompat.setImportantForAccessibility(holder.qsrCodeImage, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES)
            holder.qsrCodeImage.contentDescription = contentDescription
        }
    }

    class Holder : VectorEpoxyHolder() {
        val qsrCodeImage by bind<QrCodeImageView>(R.id.itemVerificationBigImage)
    }
}
