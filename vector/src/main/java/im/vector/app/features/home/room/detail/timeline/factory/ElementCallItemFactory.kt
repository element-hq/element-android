/*
 * Copyright 2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */
package im.vector.app.features.home.room.detail.timeline.factory

import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.resources.UserPreferencesProvider
import im.vector.app.features.home.room.detail.timeline.MessageColorProvider
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import im.vector.app.features.home.room.detail.timeline.helper.AvatarSizeProvider
import im.vector.app.features.home.room.detail.timeline.helper.MessageInformationDataFactory
import im.vector.app.features.home.room.detail.timeline.helper.MessageItemAttributesFactory
import im.vector.app.features.home.room.detail.timeline.item.ElementCallTileTimelineItem
import im.vector.app.features.home.room.detail.timeline.item.ElementCallTileTimelineItem_
import im.vector.app.features.home.room.detail.timeline.item.MessageInformationData
import im.vector.app.features.home.room.detail.timeline.item.ReactionsSummaryEvents
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.room.model.message.ElementCallNotifyContent
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

class ElementCallItemFactory @Inject constructor(
        private val session: Session,
        private val userPreferencesProvider: UserPreferencesProvider,
        private val messageColorProvider: MessageColorProvider,
        private val messageInformationDataFactory: MessageInformationDataFactory,
        private val messageItemAttributesFactory: MessageItemAttributesFactory,
        private val avatarSizeProvider: AvatarSizeProvider,
        private val noticeItemFactory: NoticeItemFactory
) {

    fun create(params: TimelineItemFactoryParams): VectorEpoxyModel<*>? {
        val event = params.event
        if (event.root.eventId == null) return null
        val showHiddenEvents = userPreferencesProvider.shouldShowHiddenEvents()
        val roomSummary = params.partialState.roomSummary ?: return null
        val informationData = messageInformationDataFactory.create(params)
        val callItem = when (event.root.getClearType()) {
            in EventType.ELEMENT_CALL_NOTIFY.values -> {
                val notifyContent: ElementCallNotifyContent = event.root.content.toModel() ?: return null
                createElementCallTileTimelineItem(
                        roomSummary = roomSummary,
                        callId = notifyContent.callId.orEmpty(),
                        callStatus = ElementCallTileTimelineItem.CallStatus.INVITED,
                        callKind = ElementCallTileTimelineItem.CallKind.VIDEO,
                        callback = params.callback,
                        highlight = params.isHighlighted,
                        informationData = informationData,
                        reactionsSummaryEvents = params.reactionsSummaryEvents
                )
            }
            else -> null
        }
        return if (callItem == null && showHiddenEvents) {
            // Fallback to notice item for showing hidden events
            noticeItemFactory.create(params)
        } else {
            callItem
        }
    }

    private fun createElementCallTileTimelineItem(
            roomSummary: RoomSummary,
            callId: String,
            callKind: ElementCallTileTimelineItem.CallKind,
            callStatus: ElementCallTileTimelineItem.CallStatus,
            informationData: MessageInformationData,
            highlight: Boolean,
            callback: TimelineEventController.Callback?,
            reactionsSummaryEvents: ReactionsSummaryEvents?
    ): ElementCallTileTimelineItem? {
        val userOfInterest = roomSummary.toMatrixItem()
        val attributes = messageItemAttributesFactory.create(null, informationData, callback, reactionsSummaryEvents).let {
            ElementCallTileTimelineItem.Attributes(
                    callId = callId,
                    callKind = callKind,
                    callStatus = callStatus,
                    informationData = informationData,
                    avatarRenderer = it.avatarRenderer,
                    messageColorProvider = messageColorProvider,
                    itemClickListener = it.itemClickListener,
                    itemLongClickListener = it.itemLongClickListener,
                    reactionPillCallback = it.reactionPillCallback,
                    readReceiptsCallback = it.readReceiptsCallback,
                    userOfInterest = userOfInterest,
                    callback = callback,
                    reactionsSummaryEvents = reactionsSummaryEvents
            )
        }
        return ElementCallTileTimelineItem_()
                .attributes(attributes)
                .highlighted(highlight)
                .leftGuideline(avatarSizeProvider.leftGuideline)
    }
}
