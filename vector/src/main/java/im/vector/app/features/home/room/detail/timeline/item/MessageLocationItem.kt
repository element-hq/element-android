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
import im.vector.app.core.glide.GlideApp
import im.vector.app.features.home.room.detail.timeline.helper.LocationPinProvider

@EpoxyModelClass(layout = R.layout.item_timeline_event_base)
abstract class MessageLocationItem : AbsMessageItem<MessageLocationItem.Holder>() {

    @EpoxyAttribute
    var locationUrl: String? = null

    @EpoxyAttribute
    var userId: String? = null

    @EpoxyAttribute
    var locationPinProvider: LocationPinProvider? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        renderSendState(holder.view, null)

        val location = locationUrl ?: return
        val locationOwnerId = userId ?: return

        GlideApp.with(holder.staticMapImageView)
                .load(location)
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
