/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.onboarding.ftueauth

import android.widget.ImageView
import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel

@EpoxyModelClass
abstract class SplashCarouselItem : VectorEpoxyModel<SplashCarouselItem.Holder>(R.layout.item_splash_carousel) {

    @EpoxyAttribute
    lateinit var item: SplashCarouselState.Item

    override fun bind(holder: Holder) {
        super.bind(holder)

        holder.view.setBackgroundResource(item.pageBackground)
        holder.image.setImageResource(item.image)
        holder.title.text = item.title.charSequence
        holder.body.setText(item.body)
    }

    class Holder : VectorEpoxyHolder() {
        val image by bind<ImageView>(R.id.carousel_item_image)
        val title by bind<TextView>(R.id.carousel_item_title)
        val body by bind<TextView>(R.id.carousel_item_body)
    }
}
