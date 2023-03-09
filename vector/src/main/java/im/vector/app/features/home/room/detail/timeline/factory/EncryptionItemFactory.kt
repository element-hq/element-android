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
import org.matrix.android.sdk.api.crypto.MXCRYPTO_ALGORITHM_MEGOLM
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.content.EncryptionEventContent
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.getRoomSummary
import org.matrix.android.sdk.api.session.room.model.localecho.RoomLocalEcho
import javax.inject.Inject

class EncryptionItemFactory @Inject constructor(
        private val messageItemAttributesFactory: MessageItemAttributesFactory,
        private val messageColorProvider: MessageColorProvider,
        private val stringProvider: StringProvider,
        private val informationDataFactory: MessageInformationDataFactory,
        private val avatarSizeProvider: AvatarSizeProvider,
        private val session: Session
) {

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
            val roomSummary = session.getRoomSummary(event.root.roomId.orEmpty())
            val isDirect = roomSummary?.isDirect.orFalse()
            val (resTitle, resDescription, resShield) = when {
                isDirect -> {
                    val isWaitingUser = roomSummary?.isEncrypted.orFalse() && roomSummary?.joinedMembersCount == 1 && roomSummary.invitedMembersCount == 0
                    when {
                        RoomLocalEcho.isLocalEchoId(event.root.roomId.orEmpty()) -> Triple(
                                R.string.encryption_enabled,
                                R.string.direct_room_encryption_enabled_tile_description_future,
                                StatusTileTimelineItem.ShieldUIState.BLACK
                        )
                        isWaitingUser -> Triple(
                                R.string.direct_room_encryption_enabled_waiting_users,
                                R.string.direct_room_encryption_enabled_waiting_users_tile_description,
                                StatusTileTimelineItem.ShieldUIState.WAITING
                        )
                        else -> Triple(
                                R.string.encryption_enabled,
                                R.string.direct_room_encryption_enabled_tile_description,
                                StatusTileTimelineItem.ShieldUIState.BLACK
                        )
                    }
                }
                else -> {
                    Triple(R.string.encryption_enabled, R.string.encryption_enabled_tile_description, StatusTileTimelineItem.ShieldUIState.BLACK)
                }
            }

            title = stringProvider.getString(resTitle)
            description = stringProvider.getString(resDescription)
            shield = resShield
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
