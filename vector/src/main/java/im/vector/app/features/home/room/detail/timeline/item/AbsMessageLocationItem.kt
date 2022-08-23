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

package im.vector.app.features.home.room.detail.timeline.item

import android.graphics.drawable.Drawable
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.airbnb.epoxy.EpoxyAttribute
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import im.vector.app.R
import im.vector.app.core.glide.GlideApp
import im.vector.app.core.utils.DimensionConverter
import im.vector.app.features.home.room.detail.timeline.helper.LocationPinProvider
import im.vector.app.features.home.room.detail.timeline.style.TimelineMessageLayout
import im.vector.app.features.home.room.detail.timeline.style.granularRoundedCorners
import im.vector.app.features.location.MapLoadingErrorView
import im.vector.app.features.location.MapLoadingErrorViewState

abstract class AbsMessageLocationItem<H : AbsMessageLocationItem.Holder>(
        @LayoutRes layoutId: Int = R.layout.item_timeline_event_base
) : AbsMessageItem<H>(layoutId) {

    @EpoxyAttribute
    var locationUrl: String? = null

    @EpoxyAttribute
    var locationUserId: String? = null

    @EpoxyAttribute
    var mapWidth: Int = 0

    @EpoxyAttribute
    var mapHeight: Int = 0

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var locationPinProvider: LocationPinProvider? = null

    override fun bind(holder: H) {
        super.bind(holder)
        renderSendState(holder.view, null)
        bindMap(holder)
    }

    private fun bindMap(holder: Holder) {
        val location = locationUrl ?: return
        val messageLayout = attributes.informationData.messageLayout
        val imageCornerTransformation = if (messageLayout is TimelineMessageLayout.Bubble) {
            messageLayout.cornersRadius.granularRoundedCorners()
        } else {
            val dimensionConverter = DimensionConverter(holder.view.resources)
            RoundedCorners(dimensionConverter.dpToPx(8))
        }
        holder.staticMapImageView.updateLayoutParams {
            width = mapWidth
            height = mapHeight
        }
        GlideApp.with(holder.staticMapImageView)
                .load(location)
                .apply(RequestOptions.centerCropTransform())
                .placeholder(holder.staticMapImageView.drawable)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>?,
                            isFirstResource: Boolean
                    ): Boolean {
                        holder.staticMapPinImageView.setImageDrawable(null)
                        holder.staticMapLoadingErrorView.isVisible = true
                        val mapErrorViewState = MapLoadingErrorViewState(imageCornerTransformation)
                        holder.staticMapLoadingErrorView.render(mapErrorViewState)
                        holder.staticMapCopyrightTextView.isVisible = false
                        return false
                    }

                    override fun onResourceReady(
                            resource: Drawable?,
                            model: Any?,
                            target: Target<Drawable>?,
                            dataSource: DataSource?,
                            isFirstResource: Boolean
                    ): Boolean {
                        locationPinProvider?.create(locationUserId) { pinDrawable ->
                            // we are not using Glide since it does not display it correctly when there is no user photo
                            holder.staticMapPinImageView.setImageDrawable(pinDrawable)
                        }
                        holder.staticMapLoadingErrorView.isVisible = false
                        holder.staticMapCopyrightTextView.isVisible = true
                        return false
                    }
                })
                .transform(imageCornerTransformation)
                .into(holder.staticMapImageView)
    }

    abstract class Holder(@IdRes stubId: Int) : AbsMessageItem.Holder(stubId) {
        val staticMapImageView by bind<ImageView>(R.id.staticMapImageView)
        val staticMapPinImageView by bind<ImageView>(R.id.staticMapPinImageView)
        val staticMapLoadingErrorView by bind<MapLoadingErrorView>(R.id.staticMapLoadingError)
        val staticMapCopyrightTextView by bind<TextView>(R.id.staticMapCopyrightTextView)
    }
}
