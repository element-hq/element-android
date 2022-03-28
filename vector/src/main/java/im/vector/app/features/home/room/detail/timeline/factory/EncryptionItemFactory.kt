/*
 * Copyright (c) 2020 New Vector Ltd
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

import im.vector.app.R
import im.vector.app.core.resources.StringProvider
import im.vector.app.features.home.room.detail.timeline.MessageColorProvider
import im.vector.app.features.home.room.detail.timeline.helper.AvatarSizeProvider
import im.vector.app.features.home.room.detail.timeline.helper.MessageInformationDataFactory
import im.vector.app.features.home.room.detail.timeline.helper.MessageItemAttributesFactory
import im.vector.app.features.home.room.detail.timeline.item.StatusTileTimelineItem
import im.vector.app.features.home.room.detail.timeline.item.StatusTileTimelineItem_
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.internal.crypto.MXCRYPTO_ALGORITHM_MEGOLM
import org.matrix.android.sdk.internal.crypto.model.event.EncryptionEventContent
import javax.inject.Inject

class EncryptionItemFactory @Inject constructor(
        private val messageItemAttributesFactory: MessageItemAttributesFactory,
        private val messageColorProvider: MessageColorProvider,
        private val stringProvider: StringProvider,
        private val informationDataFactory: MessageInformationDataFactory,
        private val avatarSizeProvider: AvatarSizeProvider,
        private val session: Session) {

    fun create(params: TimelineItemFactoryParams): StatusTileTimelineItem? {
        val event = params.event
        if (!event.root.isStateEvent()) {
            return null
        }
        val algorithm = event.root.content.toModel<EncryptionEventContent>()?.algorithm
        val informationData = informationDataFactory.create(params)
        val attributes = messageItemAttributesFactory.create(null, informationData, params.callback, params.reactionsSummaryEvents)

        val isSafeAlgorithm = algorithm == MXCRYPTO_ALGORITHM_MEGOLM
        val title: String
        val description: String
        val shield: StatusTileTimelineItem.ShieldUIState
        if (isSafeAlgorithm) {
            title = stringProvider.getString(R.string.encryption_enabled)
            description = stringProvider.getString(
                    if (session.getRoomSummary(event.root.roomId ?: "")?.isDirect.orFalse()) {
                        R.string.direct_room_encryption_enabled_tile_description
                    } else {
                        R.string.encryption_enabled_tile_description
                    }
            )
            shield = StatusTileTimelineItem.ShieldUIState.BLACK
        } else {
            title = stringProvider.getString(R.string.encryption_misconfigured)
            description = stringProvider.getString(R.string.encryption_unknown_algorithm_tile_description)
            shield = StatusTileTimelineItem.ShieldUIState.ERROR
        }
        return StatusTileTimelineItem_()
                .attributes(
                        StatusTileTimelineItem.Attributes(
                                title = title,
                                description = description,
                                shieldUIState = shield,
                                informationData = informationData,
                                avatarRenderer = attributes.avatarRenderer,
                                messageColorProvider = messageColorProvider,
                                emojiTypeFace = attributes.emojiTypeFace,
                                itemClickListener = attributes.itemClickListener,
                                itemLongClickListener = attributes.itemLongClickListener,
                                reactionPillCallback = attributes.reactionPillCallback,
                                readReceiptsCallback = attributes.readReceiptsCallback,
                                reactionsSummaryEvents = attributes.reactionsSummaryEvents
                        )
                )
                .highlighted(params.isHighlighted)
                .leftGuideline(avatarSizeProvider.leftGuideline)
    }
}
