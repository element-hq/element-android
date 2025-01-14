/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.timeline.factory

import im.vector.app.core.resources.StringProvider
import im.vector.app.features.home.AvatarRenderer
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import im.vector.app.features.home.room.detail.timeline.helper.AvatarSizeProvider
import im.vector.app.features.home.room.detail.timeline.helper.MessageInformationDataFactory
import im.vector.app.features.home.room.detail.timeline.item.DefaultItem
import im.vector.app.features.home.room.detail.timeline.item.DefaultItem_
import im.vector.app.features.home.room.detail.timeline.item.MessageInformationData
import im.vector.lib.strings.CommonStrings
import javax.inject.Inject

class DefaultItemFactory @Inject constructor(
        private val avatarSizeProvider: AvatarSizeProvider,
        private val avatarRenderer: AvatarRenderer,
        private val stringProvider: StringProvider,
        private val informationDataFactory: MessageInformationDataFactory
) {

    fun create(
            text: String,
            informationData: MessageInformationData,
            highlight: Boolean,
            callback: TimelineEventController.Callback?
    ): DefaultItem {
        val attributes = DefaultItem.Attributes(
                avatarRenderer = avatarRenderer,
                informationData = informationData,
                text = text,
                itemLongClickListener = { view ->
                    callback?.onEventLongClicked(informationData, null, view) ?: false
                }
        )
        return DefaultItem_()
                .leftGuideline(avatarSizeProvider.leftGuideline)
                .highlighted(highlight)
                .attributes(attributes)
    }

    fun create(params: TimelineItemFactoryParams, throwable: Throwable? = null): DefaultItem {
        val event = params.event
        val text = if (throwable == null) {
            stringProvider.getString(CommonStrings.rendering_event_error_type_of_event_not_handled, event.root.getClearType())
        } else {
            stringProvider.getString(CommonStrings.rendering_event_error_exception, event.root.eventId)
        }
        val informationData = informationDataFactory.create(params)
        return create(text, informationData, params.isHighlighted, params.callback)
    }
}
