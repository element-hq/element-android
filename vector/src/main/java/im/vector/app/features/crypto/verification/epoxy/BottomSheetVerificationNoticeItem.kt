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

import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.lib.core.utils.epoxy.charsequence.EpoxyCharSequence

/**
 * A action for bottom sheet.
 */
@EpoxyModelClass(layout = R.layout.item_verification_notice)
abstract class BottomSheetVerificationNoticeItem : VectorEpoxyModel<BottomSheetVerificationNoticeItem.Holder>() {

    @EpoxyAttribute
    lateinit var notice: EpoxyCharSequence

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.notice.text = notice.charSequence
    }

    class Holder : VectorEpoxyHolder() {
        val notice by bind<TextView>(R.id.itemVerificationNoticeText)
    }
}
