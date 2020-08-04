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

import android.widget.ImageView
import androidx.core.view.ViewCompat
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel

/**
 * A action for bottom sheet.
 */
@EpoxyModelClass(layout = R.layout.item_verification_big_image)
abstract class BottomSheetVerificationBigImageItem : VectorEpoxyModel<BottomSheetVerificationBigImageItem.Holder>() {

    @EpoxyAttribute
    var imageRes: Int = 0

    @EpoxyAttribute
    var contentDescription: String? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.image.setImageResource(imageRes)

        if (contentDescription == null) {
            ViewCompat.setImportantForAccessibility(holder.image, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO)
        } else {
            ViewCompat.setImportantForAccessibility(holder.image, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES)
            holder.image.contentDescription = contentDescription
        }
    }

    class Holder : VectorEpoxyHolder() {
        val image by bind<ImageView>(R.id.itemVerificationBigImage)
    }
}
