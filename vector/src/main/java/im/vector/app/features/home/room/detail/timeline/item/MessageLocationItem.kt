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

import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.epoxy.onClick
import im.vector.app.features.home.room.detail.timeline.helper.LocationPinProvider
import im.vector.app.features.location.LocationData
import im.vector.app.features.location.MapTilerMapView

@EpoxyModelClass(layout = R.layout.item_timeline_event_base)
abstract class MessageLocationItem : AbsMessageItem<MessageLocationItem.Holder>() {

    interface Callback {
        fun onMapClicked()
    }

    @EpoxyAttribute
    var callback: Callback? = null

    @EpoxyAttribute
    var locationData: LocationData? = null

    @EpoxyAttribute
    var userId: String? = null

    @EpoxyAttribute
    var locationPinProvider: LocationPinProvider? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        renderSendState(holder.mapViewContainer, null)

        val location = locationData ?: return
        val locationOwnerId = userId ?: return

        holder.clickableMapArea.onClick {
            callback?.onMapClicked()
        }

        holder.mapView.apply {
            initialize {
                zoomToLocation(location.latitude, location.longitude, INITIAL_ZOOM)

                locationPinProvider?.create(locationOwnerId) { pinDrawable ->
                    addPinToMap(locationOwnerId, pinDrawable)
                    updatePinLocation(locationOwnerId, location.latitude, location.longitude)
                }
            }
        }
    }

    override fun getViewType() = STUB_ID

    class Holder : AbsMessageItem.Holder(STUB_ID) {
        val mapViewContainer by bind<ConstraintLayout>(R.id.mapViewContainer)
        val mapView by bind<MapTilerMapView>(R.id.mapView)
        val clickableMapArea by bind<FrameLayout>(R.id.clickableMapArea)
    }

    companion object {
        private const val STUB_ID = R.id.messageContentLocationStub
        private const val INITIAL_ZOOM = 15.0
    }
}
