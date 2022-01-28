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

import android.widget.ImageView
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.bumptech.glide.request.RequestOptions
import im.vector.app.R
import im.vector.app.core.epoxy.onClick
import im.vector.app.core.glide.GlideApp
import im.vector.app.features.home.room.detail.timeline.helper.LocationPinProvider
import im.vector.app.features.location.INITIAL_MAP_ZOOM_IN_TIMELINE
import im.vector.app.features.location.LocationData
import im.vector.app.features.location.getStaticMapUrl

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

    @EpoxyAttribute
    var mapWidth: Int = 1200

    @EpoxyAttribute
    var mapHeight: Int = 800

    override fun bind(holder: Holder) {
        super.bind(holder)
        renderSendState(holder.view, null)

        val location = locationData ?: return
        val locationOwnerId = userId ?: return

        holder.view.onClick {
            callback?.onMapClicked()
        }

        GlideApp.with(holder.staticMapImageView)
                .load(getStaticMapUrl(location.latitude, location.longitude, INITIAL_MAP_ZOOM_IN_TIMELINE, mapWidth, mapHeight))
                .apply(RequestOptions.centerCropTransform())
                .into(holder.staticMapImageView)

        locationPinProvider?.create(locationOwnerId) { pinDrawable ->
            GlideApp.with(holder.staticMapPinImageView)
                    .load(pinDrawable)
                    .into(holder.staticMapPinImageView)
        }
    }

    override fun getViewType() = STUB_ID

    class Holder : AbsMessageItem.Holder(STUB_ID) {
        val staticMapImageView by bind<ImageView>(R.id.staticMapImageView)
        val staticMapPinImageView by bind<ImageView>(R.id.staticMapPinImageView)
    }

    companion object {
        private const val STUB_ID = R.id.messageContentLocationStub
    }
}
