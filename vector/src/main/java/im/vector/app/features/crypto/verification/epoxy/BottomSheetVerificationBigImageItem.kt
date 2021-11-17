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
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import org.matrix.android.sdk.api.crypto.RoomEncryptionTrustLevel

/**
 * A action for bottom sheet.
 */
@EpoxyModelClass(layout = R.layout.item_verification_big_image)
abstract class BottomSheetVerificationBigImageItem : VectorEpoxyModel<BottomSheetVerificationBigImageItem.Holder>() {

    @EpoxyAttribute
    lateinit var roomEncryptionTrustLevel: RoomEncryptionTrustLevel

    override fun bind(holder: Holder) {
        super.bind(holder)
        when (roomEncryptionTrustLevel) {
            RoomEncryptionTrustLevel.Default -> {
                holder.image.contentDescription = holder.view.context.getString(R.string.a11y_trust_level_default)
                holder.image.setImageResource(R.drawable.ic_shield_black)
            }
            RoomEncryptionTrustLevel.Warning -> {
                holder.image.contentDescription = holder.view.context.getString(R.string.a11y_trust_level_warning)
                holder.image.setImageResource(R.drawable.ic_shield_warning_no_border)
            }
            RoomEncryptionTrustLevel.Trusted -> {
                holder.image.contentDescription = holder.view.context.getString(R.string.a11y_trust_level_trusted)
                holder.image.setImageResource(R.drawable.ic_shield_trusted_no_border)
            }
        }
    }

    class Holder : VectorEpoxyHolder() {
        val image by bind<ImageView>(R.id.itemVerificationBigImage)
    }
}
