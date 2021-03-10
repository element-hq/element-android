/*
 * Copyright (c) 2021 New Vector Ltd
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
package im.vector.app.features.discovery

import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.airbnb.epoxy.EpoxyModelWithHolder
import im.vector.app.R
import im.vector.app.core.epoxy.VectorEpoxyHolder

@EpoxyModelClass(layout = R.layout.item_settings_centered_image)
abstract class SettingsCenteredImageItem : EpoxyModelWithHolder<SettingsCenteredImageItem.Holder>() {

    @EpoxyAttribute
    @DrawableRes
    var drawableRes: Int = 0

    override fun bind(holder: Holder) {
        super.bind(holder)
        holder.image.setImageResource(drawableRes)
    }

    class Holder : VectorEpoxyHolder() {
        val image by bind<ImageView>(R.id.itemSettingsImage)
    }
}
