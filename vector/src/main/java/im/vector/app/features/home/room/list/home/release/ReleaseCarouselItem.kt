/*
 * Copyright (c) 2022 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.home.room.list.home.release

import android.widget.ImageView
import android.widget.TextView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder
import im.vector.app.core.epoxy.VectorEpoxyModel

@EpoxyModelClass
abstract class ReleaseCarouselItem : VectorEpoxyModel<ReleaseCarouselItem.Holder>(R.layout.item_release_carousel) {

    @EpoxyAttribute
    lateinit var item: ReleaseCarouselData.Item

    override fun bind(holder: Holder) {
        super.bind(holder)

        holder.image.setImageResource(item.image)
        holder.title.setText(item.title)
        holder.body.setText(item.body)
    }

    class Holder : VectorEpoxyHolder() {
        val image by bind<ImageView>(R.id.carousel_item_image)
        val title by bind<TextView>(R.id.carousel_item_title)
        val body by bind<TextView>(R.id.carousel_item_body)
    }
}
