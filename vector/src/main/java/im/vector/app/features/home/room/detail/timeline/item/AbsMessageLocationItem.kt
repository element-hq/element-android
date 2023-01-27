/*
 * Copyright (c) 2021 New Vector Ltd
 * Copyright (c) 2022 BWI GmbH
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

import android.widget.ImageView
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.airbnb.epoxy.EpoxyAttribute
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.mapbox.mapboxsdk.maps.MapView
import im.vector.app.R
import im.vector.app.core.utils.DimensionConverter
import im.vector.app.features.home.room.detail.timeline.helper.LocationPinProvider
import im.vector.app.features.home.room.detail.timeline.style.TimelineMessageLayout
import im.vector.app.features.home.room.detail.timeline.style.granularRoundedCorners
import im.vector.app.features.location.INITIAL_MAP_ZOOM_IN_TIMELINE
import im.vector.app.features.location.LocationData
import im.vector.app.features.location.MapLoadingErrorView
import im.vector.app.features.location.MapLoadingErrorViewState
import im.vector.app.features.location.zoomToLocation

abstract class AbsMessageLocationItem<H : AbsMessageLocationItem.Holder>(
        @LayoutRes layoutId: Int = R.layout.item_timeline_event_base
) : AbsMessageItem<H>(layoutId) {

    @EpoxyAttribute
    var locationData: LocationData? = null

    @EpoxyAttribute
    var mapStyleUrl: String? = null

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

    override fun onViewAttachedToWindow(holder: H) {
        super.onViewAttachedToWindow(holder)
        holder.staticMapView.onStart()
    }

    override fun onViewDetachedFromWindow(holder: H) {
        super.onViewDetachedFromWindow(holder)
        holder.staticMapView.onStop()
    }

    private fun bindMap(holder: Holder) {
        val messageLayout = attributes.informationData.messageLayout
        val imageCornerTransformation = if (messageLayout is TimelineMessageLayout.Bubble) {
            messageLayout.cornersRadius.granularRoundedCorners()
        } else {
            val dimensionConverter = DimensionConverter(holder.view.resources)
            RoundedCorners(dimensionConverter.dpToPx(8))
        }

        holder.staticMapView.apply {
            updateLayoutParams {
                width = mapWidth
                height = mapHeight
            }
            addOnDidFailLoadingMapListener {
                holder.staticMapLoadingErrorView.isVisible = true
                val mapErrorViewState = MapLoadingErrorViewState(imageCornerTransformation)
                holder.staticMapLoadingErrorView.render(mapErrorViewState)
            }

            addOnDidFinishLoadingMapListener {
                locationPinProvider?.create(locationUserId) { pinDrawable ->
                    holder.staticMapPinImageView.setImageDrawable(pinDrawable)
                }
                holder.staticMapLoadingErrorView.isVisible = false
            }

            clipToOutline = true
            getMapAsync { mapbox ->
                mapbox.setStyle(mapStyleUrl)
                locationData?.let {
                    mapbox.zoomToLocation(it, false, INITIAL_MAP_ZOOM_IN_TIMELINE)
                }
                mapbox.uiSettings.setAllGesturesEnabled(false)
                mapbox.addOnMapClickListener {
                    attributes.itemClickListener?.invoke(holder.staticMapView)
                    true
                }
                mapbox.addOnMapLongClickListener {
                    attributes.itemLongClickListener?.onLongClick(holder.staticMapView)
                    true
                }
            }
        }
    }

    abstract class Holder(@IdRes stubId: Int) : AbsMessageItem.Holder(stubId) {
        val staticMapView by bind<MapView>(R.id.staticMapView)
        val staticMapPinImageView by bind<ImageView>(R.id.staticMapPinImageView)
        val staticMapLoadingErrorView by bind<MapLoadingErrorView>(R.id.staticMapLoadingError)
    }
}
