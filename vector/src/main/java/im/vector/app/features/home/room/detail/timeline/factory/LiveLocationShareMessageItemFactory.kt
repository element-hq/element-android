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

package im.vector.app.features.home.room.detail.timeline.factory

import im.vector.app.core.date.VectorDateFormatter
import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.resources.DateProvider
import im.vector.app.core.utils.DimensionConverter
import im.vector.app.features.home.room.detail.timeline.helper.AvatarSizeProvider
import im.vector.app.features.home.room.detail.timeline.helper.LocationPinProvider
import im.vector.app.features.home.room.detail.timeline.helper.TimelineMediaSizeProvider
import im.vector.app.features.home.room.detail.timeline.item.AbsMessageItem
import im.vector.app.features.home.room.detail.timeline.item.LiveLocationShareSummaryData
import im.vector.app.features.home.room.detail.timeline.item.MessageLiveLocationItem
import im.vector.app.features.home.room.detail.timeline.item.MessageLiveLocationItem_
import im.vector.app.features.home.room.detail.timeline.item.MessageLiveLocationStartItem
import im.vector.app.features.home.room.detail.timeline.item.MessageLiveLocationStartItem_
import im.vector.app.features.location.INITIAL_MAP_ZOOM_IN_TIMELINE
import im.vector.app.features.location.UrlMapProvider
import im.vector.app.features.location.toLocationData
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.Session
import org.threeten.bp.LocalDateTime
import timber.log.Timber
import javax.inject.Inject

class LiveLocationShareMessageItemFactory @Inject constructor(
        private val session: Session,
        private val dimensionConverter: DimensionConverter,
        private val timelineMediaSizeProvider: TimelineMediaSizeProvider,
        private val avatarSizeProvider: AvatarSizeProvider,
        private val urlMapProvider: UrlMapProvider,
        private val locationPinProvider: LocationPinProvider,
        private val vectorDateFormatter: VectorDateFormatter,
) {

    fun create(
            liveLocationShareSummaryData: LiveLocationShareSummaryData?,
            highlight: Boolean,
            attributes: AbsMessageItem.Attributes,
    ): VectorEpoxyModel<*>? {
        return when (val currentState = getViewState(liveLocationShareSummaryData)) {
            LiveLocationShareViewState.Loading    -> buildLoadingItem(highlight, attributes)
            LiveLocationShareViewState.Inactive   -> buildInactiveItem()
            is LiveLocationShareViewState.Running -> buildRunningItem(highlight, attributes, currentState)
            LiveLocationShareViewState.Unkwown    -> null
        }
    }

    private fun buildLoadingItem(
            highlight: Boolean,
            attributes: AbsMessageItem.Attributes,
    ): MessageLiveLocationStartItem {
        val width = timelineMediaSizeProvider.getMaxSize().first
        val height = dimensionConverter.dpToPx(MessageItemFactory.MESSAGE_LOCATION_ITEM_HEIGHT_IN_DP)

        return MessageLiveLocationStartItem_()
                .attributes(attributes)
                .mapWidth(width)
                .mapHeight(height)
                .highlighted(highlight)
                .leftGuideline(avatarSizeProvider.leftGuideline)
    }

    private fun buildRunningItem(
            highlight: Boolean,
            attributes: AbsMessageItem.Attributes,
            runningState: LiveLocationShareViewState.Running,
    ): MessageLiveLocationItem {
        // TODO only render location if enabled in preferences: to be handled in a next PR
        val width = timelineMediaSizeProvider.getMaxSize().first
        val height = dimensionConverter.dpToPx(MessageItemFactory.MESSAGE_LOCATION_ITEM_HEIGHT_IN_DP)

        val locationUrl = runningState.lastGeoUri.toLocationData()?.let {
            urlMapProvider.buildStaticMapUrl(it, INITIAL_MAP_ZOOM_IN_TIMELINE, width, height)
        }

        return MessageLiveLocationItem_()
                .attributes(attributes)
                .locationUrl(locationUrl)
                .mapWidth(width)
                .mapHeight(height)
                .locationUserId(attributes.informationData.senderId)
                .locationPinProvider(locationPinProvider)
                .highlighted(highlight)
                .leftGuideline(avatarSizeProvider.leftGuideline)
                .currentUserId(session.myUserId)
                .endOfLiveDateTime(runningState.endOfLiveDateTime)
                .vectorDateFormatter(vectorDateFormatter)
    }

    private fun buildInactiveItem() = null

    private fun getViewState(liveLocationShareSummaryData: LiveLocationShareSummaryData?): LiveLocationShareViewState {
        return when {
            liveLocationShareSummaryData?.isActive == null                                                   -> LiveLocationShareViewState.Unkwown
            liveLocationShareSummaryData.isActive && liveLocationShareSummaryData.lastGeoUri.isNullOrEmpty() -> LiveLocationShareViewState.Loading
            liveLocationShareSummaryData.isActive.not() || isLiveTimedOut(liveLocationShareSummaryData)      -> LiveLocationShareViewState.Inactive
            else                                                                                             ->
                LiveLocationShareViewState.Running(
                        liveLocationShareSummaryData.lastGeoUri.orEmpty(),
                        getEndOfLiveDateTime(liveLocationShareSummaryData)
                )
        }.also { viewState -> Timber.d("computed viewState: $viewState") }
    }

    private fun isLiveTimedOut(liveLocationShareSummaryData: LiveLocationShareSummaryData): Boolean {
        return getEndOfLiveDateTime(liveLocationShareSummaryData)
                ?.let { endOfLive ->
                    // this will only cover users with different timezones but not users with manually time set
                    val now = LocalDateTime.now()
                    now.isAfter(endOfLive)
                }
                .orFalse()
    }

    private fun getEndOfLiveDateTime(liveLocationShareSummaryData: LiveLocationShareSummaryData): LocalDateTime? {
        return liveLocationShareSummaryData.endOfLiveTimestampAsMilliseconds?.let { DateProvider.toLocalDateTime(timestamp = it) }
    }

    private sealed class LiveLocationShareViewState {
        object Loading : LiveLocationShareViewState()
        data class Running(val lastGeoUri: String, val endOfLiveDateTime: LocalDateTime?) : LiveLocationShareViewState()
        object Inactive : LiveLocationShareViewState()
        object Unkwown : LiveLocationShareViewState()
    }
}
