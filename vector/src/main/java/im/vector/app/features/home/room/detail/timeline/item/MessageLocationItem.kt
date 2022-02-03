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
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
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
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                        holder.staticMapPinImageView.setImageResource(R.drawable.ic_location_pin_failed)
                        holder.staticMapErrorTextView.isVisible = true
                        return false
                    }

                    override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                        locationPinProvider?.create(locationOwnerId) { pinDrawable ->
                            GlideApp.with(holder.staticMapPinImageView)
                                    .load(pinDrawable)
                                    .into(holder.staticMapPinImageView)
                        }
                        holder.staticMapErrorTextView.isVisible = false
                        return false
                    }
                })
                .into(holder.staticMapImageView)
    }

    override fun getViewType() = STUB_ID

    class Holder : AbsMessageItem.Holder(STUB_ID) {
        val staticMapImageView by bind<ImageView>(R.id.staticMapImageView)
        val staticMapPinImageView by bind<ImageView>(R.id.staticMapPinImageView)
        val staticMapErrorTextView by bind<TextView>(R.id.staticMapErrorTextView)
    }

    companion object {
        private const val STUB_ID = R.id.messageContentLocationStub
    }
}
