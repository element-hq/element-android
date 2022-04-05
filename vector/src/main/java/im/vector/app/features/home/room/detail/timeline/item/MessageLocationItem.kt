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
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
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
import im.vector.app.features.location.live.LocationLiveMessageBannerView
import im.vector.app.features.location.live.LocationLiveMessageBannerViewState

@EpoxyModelClass(layout = R.layout.item_timeline_event_base)
abstract class MessageLocationItem : AbsMessageItem<MessageLocationItem.Holder>() {

    @EpoxyAttribute
    var locationUrl: String? = null

    @EpoxyAttribute
    var userId: String? = null

    @EpoxyAttribute
    var mapWidth: Int = 0

    @EpoxyAttribute
    var mapHeight: Int = 0

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    var locationPinProvider: LocationPinProvider? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        renderSendState(holder.view, null)
        bindMap(holder)
        bindLocationLiveBanner(holder)
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
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(e: GlideException?,
                                              model: Any?,
                                              target: Target<Drawable>?,
                                              isFirstResource: Boolean): Boolean {
                        holder.staticMapPinImageView.setImageResource(R.drawable.ic_location_pin_failed)
                        holder.staticMapErrorTextView.isVisible = true
                        return false
                    }

                    override fun onResourceReady(resource: Drawable?,
                                                 model: Any?,
                                                 target: Target<Drawable>?,
                                                 dataSource: DataSource?,
                                                 isFirstResource: Boolean): Boolean {
                        locationPinProvider?.create(userId) { pinDrawable ->
                            // we are not using Glide since it does not display it correctly when there is no user photo
                            holder.staticMapPinImageView.setImageDrawable(pinDrawable)
                        }
                        holder.staticMapErrorTextView.isVisible = false
                        return false
                    }
                })
                .transform(imageCornerTransformation)
                .into(holder.staticMapImageView)
    }

    private fun bindLocationLiveBanner(holder: Holder) {
        val messageLayout = attributes.informationData.messageLayout
        val viewState = if (messageLayout is TimelineMessageLayout.Bubble) {
            LocationLiveMessageBannerViewState.Emitter(
                    remainingTimeInMillis = 4000 * 1000L,
                    bottomStartCornerRadiusInDp = messageLayout.cornersRadius.bottomStartRadius,
                    bottomEndCornerRadiusInDp = messageLayout.cornersRadius.bottomEndRadius,
                    isStopButtonCenteredVertically = false
            )
        } else {
            val dimensionConverter = DimensionConverter(holder.view.resources)
            val cornerRadius = dimensionConverter.dpToPx(8).toFloat()
//            LocationLiveMessageBannerViewState.Watcher(
//                    bottomStartCornerRadiusInDp = cornerRadius,
//                    bottomEndCornerRadiusInDp = cornerRadius,
//                    formattedLocalTimeOfEndOfLive = "12:34",
//            )
            LocationLiveMessageBannerViewState.Emitter(
                    remainingTimeInMillis = 4000 * 1000L,
                    bottomStartCornerRadiusInDp = cornerRadius,
                    bottomEndCornerRadiusInDp = cornerRadius,
                    isStopButtonCenteredVertically = true
            )
        }
        holder.locationLiveMessageBanner.isVisible = true
        holder.locationLiveMessageBanner.render(viewState)

        // TODO create a dedicated message Item per state: Start, Location, End? Check if inheritance is possible in Epoxy model
        // TODO adjust Copyright map placement
    }

    override fun getViewStubId() = STUB_ID

    class Holder : AbsMessageItem.Holder(STUB_ID) {
        val staticMapImageView by bind<ImageView>(R.id.staticMapImageView)
        val staticMapPinImageView by bind<ImageView>(R.id.staticMapPinImageView)
        val staticMapErrorTextView by bind<TextView>(R.id.staticMapErrorTextView)
        val locationLiveMessageBanner by bind<LocationLiveMessageBannerView>(R.id.locationLiveMessageBanner)
    }

    companion object {
        private const val STUB_ID = R.id.messageContentLocationStub
    }
}
