/*
 * Copyright 2019-2024 New Vector Ltd.
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
import im.vector.app.features.home.room.detail.timeline.helper.CallSignalingEventsGroup
import im.vector.app.features.home.room.detail.timeline.helper.MessageInformationDataFactory
import im.vector.app.features.home.room.detail.timeline.helper.MessageItemAttributesFactory
import im.vector.app.features.home.room.detail.timeline.item.CallTileTimelineItem
import im.vector.app.features.home.room.detail.timeline.item.CallTileTimelineItem_
import im.vector.app.features.home.room.detail.timeline.item.MessageInformationData
import im.vector.app.features.home.room.detail.timeline.item.ReactionsSummaryEvents
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

class CallItemFactory @Inject constructor(
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
        val callEventGrouper = params.eventsGroup?.let { CallSignalingEventsGroup(it) } ?: return null
        val roomSummary = params.partialState.roomSummary ?: return null
        val informationData = messageInformationDataFactory.create(params)
        val callKind = if (callEventGrouper.isVideo()) CallTileTimelineItem.CallKind.VIDEO else CallTileTimelineItem.CallKind.AUDIO
        val callItem = when (event.root.getClearType()) {
            EventType.CALL_ANSWER -> {
                if (callEventGrouper.isInCall()) {
                    createCallTileTimelineItem(
                            roomSummary = roomSummary,
                            callId = callEventGrouper.callId,
                            callStatus = CallTileTimelineItem.CallStatus.IN_CALL,
                            callKind = callKind,
                            callback = params.callback,
                            highlight = params.isHighlighted,
                            informationData = informationData,
                            isStillActive = callEventGrouper.isInCall(),
                            formattedDuration = callEventGrouper.formattedDuration(),
                            reactionsSummaryEvents = params.reactionsSummaryEvents
                    )
                } else {
                    null
                }
            }
            EventType.CALL_INVITE -> {
                if (callEventGrouper.isRinging()) {
                    createCallTileTimelineItem(
                            roomSummary = roomSummary,
                            callId = callEventGrouper.callId,
                            callStatus = CallTileTimelineItem.CallStatus.INVITED,
                            callKind = callKind,
                            callback = params.callback,
                            highlight = params.isHighlighted,
                            informationData = informationData,
                            isStillActive = callEventGrouper.isRinging(),
                            formattedDuration = callEventGrouper.formattedDuration(),
                            reactionsSummaryEvents = params.reactionsSummaryEvents
                    )
                } else {
                    null
                }
            }
            EventType.CALL_REJECT -> {
                createCallTileTimelineItem(
                        roomSummary = roomSummary,
                        callId = callEventGrouper.callId,
                        callStatus = CallTileTimelineItem.CallStatus.REJECTED,
                        callKind = callKind,
                        callback = params.callback,
                        highlight = params.isHighlighted,
                        informationData = informationData,
                        isStillActive = false,
                        formattedDuration = callEventGrouper.formattedDuration(),
                        reactionsSummaryEvents = params.reactionsSummaryEvents
                )
            }
            EventType.CALL_HANGUP -> {
                createCallTileTimelineItem(
                        roomSummary = roomSummary,
                        callId = callEventGrouper.callId,
                        callStatus = if (callEventGrouper.callWasAnswered()) {
                            CallTileTimelineItem.CallStatus.ENDED
                        } else {
                            CallTileTimelineItem.CallStatus.MISSED
                        },
                        callKind = callKind,
                        callback = params.callback,
                        highlight = params.isHighlighted,
                        informationData = informationData,
                        isStillActive = false,
                        formattedDuration = callEventGrouper.formattedDuration(),
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

    private fun createCallTileTimelineItem(
            roomSummary: RoomSummary,
            callId: String,
            callKind: CallTileTimelineItem.CallKind,
            callStatus: CallTileTimelineItem.CallStatus,
            informationData: MessageInformationData,
            highlight: Boolean,
            isStillActive: Boolean,
            formattedDuration: String,
            callback: TimelineEventController.Callback?,
            reactionsSummaryEvents: ReactionsSummaryEvents?
    ): CallTileTimelineItem? {
        val userOfInterest = roomSummary.toMatrixItem()
        val attributes = messageItemAttributesFactory.create(null, informationData, callback, reactionsSummaryEvents).let {
            CallTileTimelineItem.Attributes(
                    callId = callId,
                    callKind = callKind,
                    callStatus = callStatus,
                    informationData = informationData,
                    avatarRenderer = it.avatarRenderer,
                    formattedDuration = formattedDuration,
                    messageColorProvider = messageColorProvider,
                    itemClickListener = it.itemClickListener,
                    itemLongClickListener = it.itemLongClickListener,
                    reactionPillCallback = it.reactionPillCallback,
                    readReceiptsCallback = it.readReceiptsCallback,
                    userOfInterest = userOfInterest,
                    callback = callback,
                    isStillActive = isStillActive,
                    reactionsSummaryEvents = reactionsSummaryEvents
            )
        }
        return CallTileTimelineItem_()
                .attributes(attributes)
                .highlighted(highlight)
                .leftGuideline(avatarSizeProvider.leftGuideline)
    }
}
