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
import im.vector.app.core.date.DateFormatKind
import im.vector.app.core.date.VectorDateFormatter
import im.vector.app.core.resources.toTimestamp
import im.vector.app.core.utils.DimensionConverter
import im.vector.app.features.home.room.detail.RoomDetailAction
import im.vector.app.features.home.room.detail.timeline.style.TimelineMessageLayout
import im.vector.app.features.location.live.LocationLiveMessageBannerView
import im.vector.app.features.location.live.LocationLiveMessageBannerViewState
import org.threeten.bp.LocalDateTime

@EpoxyModelClass(layout = R.layout.item_timeline_event_base)
abstract class MessageLiveLocationItem : AbsMessageLocationItem<MessageLiveLocationItem.Holder>() {

    @EpoxyAttribute
    var currentUserId: String? = null

    @EpoxyAttribute
    var endOfLiveDateTime: LocalDateTime? = null

    @EpoxyAttribute(EpoxyAttribute.Option.DoNotHash)
    lateinit var vectorDateFormatter: VectorDateFormatter

    override fun bind(holder: Holder) {
        super.bind(holder)
        bindLocationLiveBanner(holder)
    }

    private fun bindLocationLiveBanner(holder: Holder) {
        // TODO in a future PR add check on device id to confirm that is the one that sent the beacon
        val isEmitter = currentUserId != null && currentUserId == locationUserId
        val messageLayout = attributes.informationData.messageLayout
        val viewState = buildViewState(holder, messageLayout, isEmitter)
        holder.locationLiveMessageBanner.isVisible = true
        holder.locationLiveMessageBanner.render(viewState)
        holder.locationLiveMessageBanner.stopButton.setOnClickListener {
            attributes.callback?.onTimelineItemAction(RoomDetailAction.StopLiveLocationSharing)
        }
    }

    private fun buildViewState(
            holder: Holder,
            messageLayout: TimelineMessageLayout,
            isEmitter: Boolean
    ): LocationLiveMessageBannerViewState {
        return when {
            messageLayout is TimelineMessageLayout.Bubble && isEmitter ->
                LocationLiveMessageBannerViewState.Emitter(
                        remainingTimeInMillis = getRemainingTimeOfLiveInMillis(),
                        bottomStartCornerRadiusInDp = messageLayout.cornersRadius.bottomStartRadius,
                        bottomEndCornerRadiusInDp = messageLayout.cornersRadius.bottomEndRadius,
                        isStopButtonCenteredVertically = false
                )
            messageLayout is TimelineMessageLayout.Bubble ->
                LocationLiveMessageBannerViewState.Watcher(
                        bottomStartCornerRadiusInDp = messageLayout.cornersRadius.bottomStartRadius,
                        bottomEndCornerRadiusInDp = messageLayout.cornersRadius.bottomEndRadius,
                        formattedLocalTimeOfEndOfLive = getFormattedLocalTimeEndOfLive(),
                )
            isEmitter -> {
                val cornerRadius = getBannerCornerRadiusForDefaultLayout(holder)
                LocationLiveMessageBannerViewState.Emitter(
                        remainingTimeInMillis = getRemainingTimeOfLiveInMillis(),
                        bottomStartCornerRadiusInDp = cornerRadius,
                        bottomEndCornerRadiusInDp = cornerRadius,
                        isStopButtonCenteredVertically = true
                )
            }
            else -> {
                val cornerRadius = getBannerCornerRadiusForDefaultLayout(holder)
                LocationLiveMessageBannerViewState.Watcher(
                        bottomStartCornerRadiusInDp = cornerRadius,
                        bottomEndCornerRadiusInDp = cornerRadius,
                        formattedLocalTimeOfEndOfLive = getFormattedLocalTimeEndOfLive(),
                )
            }
        }
    }

    private fun getBannerCornerRadiusForDefaultLayout(holder: Holder): Float {
        val dimensionConverter = DimensionConverter(holder.view.resources)
        return dimensionConverter.dpToPx(8).toFloat()
    }

    private fun getFormattedLocalTimeEndOfLive() =
            endOfLiveDateTime?.toTimestamp()?.let { vectorDateFormatter.format(it, DateFormatKind.MESSAGE_SIMPLE) }.orEmpty()

    private fun getRemainingTimeOfLiveInMillis() =
            (endOfLiveDateTime?.toTimestamp() ?: 0) - LocalDateTime.now().toTimestamp()

    override fun getViewStubId() = STUB_ID

    class Holder : AbsMessageLocationItem.Holder(STUB_ID) {
        val locationLiveMessageBanner by bind<LocationLiveMessageBannerView>(R.id.locationLiveMessageBanner)
    }

    companion object {
        private const val STUB_ID = R.id.messageContentLiveLocationStub
    }
}
