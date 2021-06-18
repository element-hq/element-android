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

import android.content.res.ColorStateList
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.ClickListener
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.extensions.setTextOrHide

/**
 * A action for bottom sheet.
 */
@EpoxyModelClass(layout = R.layout.item_verification_action)
abstract class BottomSheetVerificationActionItem : VectorEpoxyModel<BottomSheetVerificationActionItem.Holder>() {

    @EpoxyAttribute
    @DrawableRes
    var iconRes: Int = -1

    @EpoxyAttribute
    var title: CharSequence = ""

    @EpoxyAttribute
    var subTitle: CharSequence? = null

    @EpoxyAttribute
    var titleColor: Int = 0

    @EpoxyAttribute
    var iconColor: Int = -1

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    lateinit var listener: ClickListener

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.view.onClick(listener)
        holder.title.text = title
        holder.title.setTextColor(titleColor)

        holder.subTitle.setTextOrHide(subTitle)

        if (iconRes != -1) {
            holder.icon.isVisible = true
            holder.icon.setImageResource(iconRes)
            if (iconColor != -1) {
                ImageViewCompat.setImageTintList(holder.icon, ColorStateList.valueOf(iconColor))
            }
        } else {
            holder.icon.isVisible = false
        }
    }

    class Holder : VectorEpoxyHolder() {
        val title by bind<TextView>(R.id.itemVerificationActionTitle)
        val subTitle by bind<TextView>(R.id.itemVerificationActionSubTitle)
        val icon by bind<ImageView>(R.id.itemVerificationActionIcon)
    }
}
