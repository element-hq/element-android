/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
import im.vector.app.features.home.room.detail.timeline.item.MessageLiveLocationInactiveItem
import im.vector.app.features.home.room.detail.timeline.item.MessageLiveLocationInactiveItem_
import im.vector.app.features.home.room.detail.timeline.item.MessageLiveLocationItem
import im.vector.app.features.home.room.detail.timeline.item.MessageLiveLocationItem_
import im.vector.app.features.home.room.detail.timeline.item.MessageLiveLocationStartItem
import im.vector.app.features.home.room.detail.timeline.item.MessageLiveLocationStartItem_
import im.vector.app.features.location.INITIAL_MAP_ZOOM_IN_TIMELINE
import im.vector.app.features.location.UrlMapProvider
import im.vector.app.features.location.toLocationData
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.threeten.bp.LocalDateTime
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
            event: TimelineEvent,
            highlight: Boolean,
            attributes: AbsMessageItem.Attributes,
    ): VectorEpoxyModel<*>? {
        val liveLocationShareSummaryData = getLiveLocationShareSummaryData(event)
        val item = when (val currentState = getViewState(liveLocationShareSummaryData)) {
            LiveLocationShareViewState.Inactive -> buildInactiveItem(highlight, attributes)
            LiveLocationShareViewState.Loading -> buildLoadingItem(highlight, attributes)
            is LiveLocationShareViewState.Running -> buildRunningItem(highlight, attributes, currentState)
            LiveLocationShareViewState.Unkwown -> null
        }
        item?.layout(attributes.informationData.messageLayout.layoutRes)

        return item
    }

    private fun buildInactiveItem(
            highlight: Boolean,
            attributes: AbsMessageItem.Attributes,
    ): MessageLiveLocationInactiveItem {
        val width = timelineMediaSizeProvider.getMaxSize().first
        val height = dimensionConverter.dpToPx(MessageItemFactory.MESSAGE_LOCATION_ITEM_HEIGHT_IN_DP)

        return MessageLiveLocationInactiveItem_()
                // disable the click on this state item
                .attributes(attributes.copy(itemClickListener = null))
                .mapWidth(width)
                .mapHeight(height)
                .highlighted(highlight)
                .leftGuideline(avatarSizeProvider.leftGuideline)
    }

    private fun buildLoadingItem(
            highlight: Boolean,
            attributes: AbsMessageItem.Attributes,
    ): MessageLiveLocationStartItem {
        val width = timelineMediaSizeProvider.getMaxSize().first
        val height = dimensionConverter.dpToPx(MessageItemFactory.MESSAGE_LOCATION_ITEM_HEIGHT_IN_DP)

        return MessageLiveLocationStartItem_()
                // disable the click on this state item
                .attributes(attributes.copy(itemClickListener = null))
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
                .pinMatrixItem(attributes.informationData.matrixItem)
                .locationPinProvider(locationPinProvider)
                .highlighted(highlight)
                .leftGuideline(avatarSizeProvider.leftGuideline)
                .currentUserId(session.myUserId)
                .endOfLiveDateTime(runningState.endOfLiveDateTime)
                .vectorDateFormatter(vectorDateFormatter)
    }

    private fun getViewState(liveLocationShareSummaryData: LiveLocationShareSummaryData?): LiveLocationShareViewState {
        return when {
            liveLocationShareSummaryData?.isActive == null -> LiveLocationShareViewState.Unkwown
            liveLocationShareSummaryData.isActive.not() -> LiveLocationShareViewState.Inactive
            liveLocationShareSummaryData.isActive && liveLocationShareSummaryData.lastGeoUri.isNullOrEmpty() -> LiveLocationShareViewState.Loading
            else ->
                LiveLocationShareViewState.Running(
                        liveLocationShareSummaryData.lastGeoUri.orEmpty(),
                        getEndOfLiveDateTime(liveLocationShareSummaryData)
                )
        }
    }

    private fun getEndOfLiveDateTime(liveLocationShareSummaryData: LiveLocationShareSummaryData): LocalDateTime? {
        return liveLocationShareSummaryData.endOfLiveTimestampMillis?.let { DateProvider.toLocalDateTime(timestamp = it) }
    }

    private fun getLiveLocationShareSummaryData(event: TimelineEvent): LiveLocationShareSummaryData? {
        return event.annotations?.liveLocationShareAggregatedSummary?.let { summary ->
            LiveLocationShareSummaryData(
                    isActive = summary.isActive,
                    endOfLiveTimestampMillis = summary.endOfLiveTimestampMillis,
                    lastGeoUri = summary.lastLocationDataContent?.getBestLocationInfo()?.geoUri
            )
        }
    }

    private data class LiveLocationShareSummaryData(
            val isActive: Boolean?,
            val endOfLiveTimestampMillis: Long?,
            val lastGeoUri: String?,
    )

    private sealed class LiveLocationShareViewState {
        object Loading : LiveLocationShareViewState()
        data class Running(val lastGeoUri: String, val endOfLiveDateTime: LocalDateTime?) : LiveLocationShareViewState()
        object Inactive : LiveLocationShareViewState()
        object Unkwown : LiveLocationShareViewState()
    }
}
