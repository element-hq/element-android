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

import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.utils.DimensionConverter
import im.vector.app.features.home.room.detail.timeline.helper.AvatarSizeProvider
import im.vector.app.features.home.room.detail.timeline.helper.TimelineMediaSizeProvider
import im.vector.app.features.home.room.detail.timeline.item.AbsMessageItem
import im.vector.app.features.home.room.detail.timeline.item.LiveLocationShareSummaryData
import im.vector.app.features.home.room.detail.timeline.item.MessageLiveLocationStartItem
import im.vector.app.features.home.room.detail.timeline.item.MessageLiveLocationStartItem_
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.room.model.message.LocationInfo
import java.time.LocalDateTime
import javax.inject.Inject

class LiveLocationShareMessageItemFactory @Inject constructor(
        private val dimensionConverter: DimensionConverter,
        private val timelineMediaSizeProvider: TimelineMediaSizeProvider,
        private val avatarSizeProvider: AvatarSizeProvider,
) {

    fun create(
            liveLocationShareSummaryData: LiveLocationShareSummaryData?,
            highlight: Boolean,
            attributes: AbsMessageItem.Attributes,
    ): VectorEpoxyModel<*>? {
        // TODO handle location received and stopped states
        // TODO create a dedicated ViewState
        return when {
            liveLocationShareSummaryData == null        -> null
            isLiveRunning(liveLocationShareSummaryData) -> buildStartLiveItem(highlight, attributes)
            else                                        -> null
        }
    }

    private fun isLiveRunning(liveLocationShareSummaryData: LiveLocationShareSummaryData): Boolean {
        // TODO check if the live has timed out as well
        return liveLocationShareSummaryData.isActive.orFalse()
    }

    private fun buildStartLiveItem(
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

    private sealed class LiveLocationShareViewState {
        object Loading : LiveLocationShareViewState()
        data class Running(val locationInfo: LocationInfo, val endOfLiveDateTime: LocalDateTime?) : LiveLocationShareViewState()
        object Inactive : LiveLocationShareViewState()
        object Unkwown : LiveLocationShareViewState()
    }
}
