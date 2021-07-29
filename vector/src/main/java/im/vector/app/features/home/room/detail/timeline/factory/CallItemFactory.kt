/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package im.vector.app.features.home.room.detail.timeline.factory

import im.vector.app.core.epoxy.VectorEpoxyModel
import im.vector.app.core.resources.UserPreferencesProvider
import im.vector.app.features.call.vectorCallService
import im.vector.app.features.home.room.detail.timeline.MessageColorProvider
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import im.vector.app.features.home.room.detail.timeline.helper.AvatarSizeProvider
import im.vector.app.features.home.room.detail.timeline.helper.MessageInformationDataFactory
import im.vector.app.features.home.room.detail.timeline.helper.MessageItemAttributesFactory
import im.vector.app.features.home.room.detail.timeline.helper.RoomSummariesHolder
import im.vector.app.features.home.room.detail.timeline.helper.TimelineEventVisibilityHelper
import im.vector.app.features.home.room.detail.timeline.item.CallTileTimelineItem
import im.vector.app.features.home.room.detail.timeline.item.CallTileTimelineItem_
import im.vector.app.features.home.room.detail.timeline.item.MessageInformationData
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

class CallItemFactory @Inject constructor(
        private val session: Session,
        private val userPreferencesProvider: UserPreferencesProvider,
        private val messageColorProvider: MessageColorProvider,
        private val messageInformationDataFactory: MessageInformationDataFactory,
        private val messageItemAttributesFactory: MessageItemAttributesFactory,
        private val avatarSizeProvider: AvatarSizeProvider,
        private val roomSummariesHolder: RoomSummariesHolder) {

    fun create(params: TimelineItemFactoryParams): VectorEpoxyModel<*>? {
        val event = params.event
        if (event.root.eventId == null) return null
        val showHiddenEvents = userPreferencesProvider.shouldShowHiddenEvents()
        val callEventGrouper = params.callEventGrouper ?: return null
        val roomId = event.roomId
        val informationData = messageInformationDataFactory.create(params)
        val callKind = if (callEventGrouper.isVideo()) CallTileTimelineItem.CallKind.VIDEO else CallTileTimelineItem.CallKind.AUDIO
        return when (event.root.getClearType()) {
            EventType.CALL_ANSWER -> {
                if (callEventGrouper.isInCall() || showHiddenEvents) {
                    createCallTileTimelineItem(
                            roomId = roomId,
                            callId = callEventGrouper.callId,
                            callStatus = CallTileTimelineItem.CallStatus.IN_CALL,
                            callKind = callKind,
                            callback = params.callback,
                            highlight = params.isHighlighted,
                            informationData = informationData,
                            isStillActive = callEventGrouper.isInCall()
                    )
                } else {
                    null
                }
            }
            EventType.CALL_INVITE -> {
                if (callEventGrouper.isRinging() || showHiddenEvents) {
                    createCallTileTimelineItem(
                            roomId = roomId,
                            callId = callEventGrouper.callId,
                            callStatus = CallTileTimelineItem.CallStatus.INVITED,
                            callKind = callKind,
                            callback = params.callback,
                            highlight = params.isHighlighted,
                            informationData = informationData,
                            isStillActive = callEventGrouper.isRinging()
                    )
                } else {
                    null
                }
            }
            EventType.CALL_REJECT -> {
                createCallTileTimelineItem(
                        roomId = roomId,
                        callId = callEventGrouper.callId,
                        callStatus = CallTileTimelineItem.CallStatus.REJECTED,
                        callKind = callKind,
                        callback = params.callback,
                        highlight = params.isHighlighted,
                        informationData = informationData,
                        isStillActive = false
                )
            }
            EventType.CALL_HANGUP -> {
                createCallTileTimelineItem(
                        roomId = roomId,
                        callId = callEventGrouper.callId,
                        callStatus = if (callEventGrouper.callWasMissed()) CallTileTimelineItem.CallStatus.MISSED else CallTileTimelineItem.CallStatus.ENDED,
                        callKind = callKind,
                        callback = params.callback,
                        highlight = params.isHighlighted,
                        informationData = informationData,
                        isStillActive = false
                )
            }
            else                  -> null
        }
    }

    private fun createCallTileTimelineItem(
            roomId: String,
            callId: String,
            callKind: CallTileTimelineItem.CallKind,
            callStatus: CallTileTimelineItem.CallStatus,
            informationData: MessageInformationData,
            highlight: Boolean,
            isStillActive: Boolean,
            callback: TimelineEventController.Callback?
    ): CallTileTimelineItem? {
        val correctedRoomId = session.vectorCallService.userMapper.nativeRoomForVirtualRoom(roomId) ?: roomId
        val userOfInterest = roomSummariesHolder.get(correctedRoomId)?.toMatrixItem() ?: return null
        val attributes = messageItemAttributesFactory.create(null, informationData, callback).let {
            CallTileTimelineItem.Attributes(
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
                    isStillActive = isStillActive
            )
        }
        return CallTileTimelineItem_()
                .attributes(attributes)
                .highlighted(highlight)
                .leftGuideline(avatarSizeProvider.leftGuideline)
    }
}
