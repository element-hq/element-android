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

import androidx.core.view.isVisible
import com.airbnb.epoxy.EpoxyAttribute
import com.airbnb.epoxy.EpoxyModelClass
import im.vector.app.R
import im.vector.app.core.utils.DimensionConverter
import im.vector.app.features.home.room.detail.timeline.style.TimelineMessageLayout
import im.vector.app.features.location.live.LocationLiveMessageBannerView
import im.vector.app.features.location.live.LocationLiveMessageBannerViewState

@EpoxyModelClass(layout = R.layout.item_timeline_event_base)
abstract class MessageLiveLocationItem : AbsMessageLocationItem<MessageLiveLocationItem.Holder>() {

    // TODO define the needed attributes
    @EpoxyAttribute
    var currentUserId: String? = null

    override fun bind(holder: Holder) {
        super.bind(holder)
        bindLocationLiveBanner(holder)
    }

    private fun bindLocationLiveBanner(holder: Holder) {
        // TODO add check on device id to confirm that is the one that sent the beacon
        val isEmitter = currentUserId != null && currentUserId == userId
        val messageLayout = attributes.informationData.messageLayout
        val viewState = buildViewState(holder, messageLayout, isEmitter)
        holder.locationLiveMessageBanner.isVisible = true
        holder.locationLiveMessageBanner.render(viewState)
        // TODO adjust Copyright map placement if needed
    }

    private fun buildViewState(
            holder: Holder,
            messageLayout: TimelineMessageLayout,
            isEmitter: Boolean
    ): LocationLiveMessageBannerViewState {
        return when {
            messageLayout is TimelineMessageLayout.Bubble && isEmitter ->
                LocationLiveMessageBannerViewState.Emitter(
                        remainingTimeInMillis = 4000 * 1000L,
                        bottomStartCornerRadiusInDp = messageLayout.cornersRadius.bottomStartRadius,
                        bottomEndCornerRadiusInDp = messageLayout.cornersRadius.bottomEndRadius,
                        isStopButtonCenteredVertically = false
                )
            messageLayout is TimelineMessageLayout.Bubble              ->
                LocationLiveMessageBannerViewState.Watcher(
                        bottomStartCornerRadiusInDp = messageLayout.cornersRadius.bottomStartRadius,
                        bottomEndCornerRadiusInDp = messageLayout.cornersRadius.bottomEndRadius,
                        formattedLocalTimeOfEndOfLive = "12:34",
                )
            isEmitter                                                  -> {
                val cornerRadius = getBannerCornerRadiusForDefaultLayout(holder)
                LocationLiveMessageBannerViewState.Emitter(
                        remainingTimeInMillis = 4000 * 1000L,
                        bottomStartCornerRadiusInDp = cornerRadius,
                        bottomEndCornerRadiusInDp = cornerRadius,
                        isStopButtonCenteredVertically = true
                )
            }
            else                                                       -> {
                val cornerRadius = getBannerCornerRadiusForDefaultLayout(holder)
                LocationLiveMessageBannerViewState.Watcher(
                        bottomStartCornerRadiusInDp = cornerRadius,
                        bottomEndCornerRadiusInDp = cornerRadius,
                        formattedLocalTimeOfEndOfLive = "12:34",
                )
            }
        }
    }

    private fun getBannerCornerRadiusForDefaultLayout(holder: Holder): Float {
        val dimensionConverter = DimensionConverter(holder.view.resources)
        return dimensionConverter.dpToPx(8).toFloat()
    }

    class Holder : AbsMessageLocationItem.Holder() {
        val locationLiveMessageBanner by bind<LocationLiveMessageBannerView>(R.id.locationLiveMessageBanner)
    }
}
