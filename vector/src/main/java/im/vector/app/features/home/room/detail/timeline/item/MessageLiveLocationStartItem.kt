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

package im.vector.app.features.home.room.detail.timeline.item

import android.graphics.drawable.ColorDrawable
import android.widget.ImageView
import androidx.core.view.updateLayoutParams
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import im.vector.app.R
import im.vector.app.core.glide.GlideApp
import im.vector.app.core.utils.DimensionConverter
import im.vector.app.features.home.room.detail.timeline.style.TimelineMessageLayout
import im.vector.app.features.home.room.detail.timeline.style.granularRoundedCorners
import im.vector.app.features.themes.ThemeUtils

@EpoxyModelClass(layout = R.layout.item_timeline_event_base)
abstract class MessageLiveLocationStartItem : AbsMessageItem<MessageLiveLocationStartItem.Holder>() {

    @EpoxyAttribute
    var mapWidth: Int = 0

    @EpoxyAttribute
    var mapHeight: Int = 0

    override fun bind(holder: Holder) {
        super.bind(holder)
        renderSendState(holder.view, null)
        bindMap(holder)
        bindBottomBanner(holder)
    }

    private fun bindMap(holder: Holder) {
        val messageLayout = attributes.informationData.messageLayout
        val mapCornerTransformation = if (messageLayout is TimelineMessageLayout.Bubble) {
            messageLayout.cornersRadius.granularRoundedCorners()
        } else {
            RoundedCorners(getDefaultLayoutCornerRadiusInDp(holder))
        }
        holder.noLocationMapImageView.updateLayoutParams {
            width = mapWidth
            height = mapHeight
        }
        GlideApp.with(holder.noLocationMapImageView)
                .load(R.drawable.bg_no_location_map)
                .transform(mapCornerTransformation)
                .into(holder.noLocationMapImageView)
    }

    private fun bindBottomBanner(holder: Holder) {
        val messageLayout = attributes.informationData.messageLayout
        val imageCornerTransformation = if (messageLayout is TimelineMessageLayout.Bubble) {
            GranularRoundedCorners(0f, 0f, messageLayout.cornersRadius.bottomEndRadius, messageLayout.cornersRadius.bottomStartRadius)
        } else {
            val bottomCornerRadius = getDefaultLayoutCornerRadiusInDp(holder).toFloat()
            GranularRoundedCorners(0f, 0f, bottomCornerRadius, bottomCornerRadius)
        }
        GlideApp.with(holder.bannerImageView)
                .load(ColorDrawable(ThemeUtils.getColor(holder.bannerImageView.context, R.attr.colorSurface)))
                .transform(imageCornerTransformation)
                .into(holder.bannerImageView)
    }

    private fun getDefaultLayoutCornerRadiusInDp(holder: Holder): Int {
        val dimensionConverter = DimensionConverter(holder.view.resources)
        return dimensionConverter.dpToPx(8)
    }

    override fun getViewStubId() = STUB_ID

    class Holder : AbsMessageItem.Holder(STUB_ID) {
        val bannerImageView by bind<ImageView>(R.id.locationLiveStartBanner)
        val noLocationMapImageView by bind<ImageView>(R.id.locationLiveStartMap)
    }

    companion object {
        private const val STUB_ID = R.id.messageContentLiveLocationStartStub
    }
}
