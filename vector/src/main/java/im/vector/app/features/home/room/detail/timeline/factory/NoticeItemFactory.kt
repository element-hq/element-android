/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.factory

import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.timeline.format.NoticeEventFormatter
import im.vector.app.features.home.room.detail.timeline.helper.AvatarSizeProvider
import im.vector.app.features.home.room.detail.timeline.helper.MessageInformationDataFactory
import im.vector.app.features.home.room.detail.timeline.item.NoticeItem
import im.vector.app.features.home.room.detail.timeline.item.NoticeItem_
import im.vector.lib.core.utils.epoxy.charsequence.EpoxyCharSequence
import org.matrix.android.sdk.api.extensions.orFalse
import javax.inject.Inject

class NoticeItemFactory @Inject constructor(
        private val eventFormatter: NoticeEventFormatter,
        private val avatarRenderer: AvatarRenderer,
        private val informationDataFactory: MessageInformationDataFactory,
        private val avatarSizeProvider: AvatarSizeProvider
) {

    fun create(params: TimelineItemFactoryParams): NoticeItem? {
        val event = params.event
        val formattedText = eventFormatter.format(event, isDm = params.partialState.roomSummary?.isDirect.orFalse()) ?: return null
        val informationData = informationDataFactory.create(params)
        val attributes = NoticeItem.Attributes(
                avatarRenderer = avatarRenderer,
                informationData = informationData,
                noticeText = EpoxyCharSequence(formattedText),
                itemLongClickListener = { view ->
                    params.callback?.onEventLongClicked(informationData, null, view) ?: false
                },
                readReceiptsCallback = params.callback,
                avatarClickListener = { params.callback?.onAvatarClicked(informationData) }
        )
        return NoticeItem_()
                .leftGuideline(avatarSizeProvider.leftGuideline)
                .highlighted(params.isHighlighted)
                .attributes(attributes)
    }
}
