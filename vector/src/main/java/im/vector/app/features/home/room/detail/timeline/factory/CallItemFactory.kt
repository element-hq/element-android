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
import im.vector.app.features.call.webrtc.WebRtcCallManager
import im.vector.app.features.home.room.detail.timeline.MessageColorProvider
import im.vector.app.features.home.room.detail.timeline.TimelineEventController
import im.vector.app.features.home.room.detail.timeline.helper.AvatarSizeProvider
import im.vector.app.features.home.room.detail.timeline.helper.MessageInformationDataFactory
import im.vector.app.features.home.room.detail.timeline.helper.MessageItemAttributesFactory
import im.vector.app.features.home.room.detail.timeline.helper.RoomSummaryHolder
import im.vector.app.features.home.room.detail.timeline.item.CallTileTimelineItem
import im.vector.app.features.home.room.detail.timeline.item.CallTileTimelineItem_
import im.vector.app.features.home.room.detail.timeline.item.MessageInformationData
import org.matrix.android.sdk.api.extensions.orFalse
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.call.CallAnswerContent
import org.matrix.android.sdk.api.session.room.model.call.CallHangupContent
import org.matrix.android.sdk.api.session.room.model.call.CallInviteContent
import org.matrix.android.sdk.api.session.room.model.call.CallRejectContent
import org.matrix.android.sdk.api.session.room.model.call.CallSignallingContent
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.util.toMatrixItem
import javax.inject.Inject

class CallItemFactory @Inject constructor(
        private val messageColorProvider: MessageColorProvider,
        private val messageInformationDataFactory: MessageInformationDataFactory,
        private val messageItemAttributesFactory: MessageItemAttributesFactory,
        private val avatarSizeProvider: AvatarSizeProvider,
        private val roomSummaryHolder: RoomSummaryHolder,
        private val callManager: WebRtcCallManager
) {

    fun create(event: TimelineEvent,
               highlight: Boolean,
               callback: TimelineEventController.Callback?
    ): VectorEpoxyModel<*>? {
        if (event.root.eventId == null) return null
        val informationData = messageInformationDataFactory.create(event, null)
        val callSignalingContent = event.getCallSignallingContent() ?: return null
        val callId = callSignalingContent.callId ?: return null
        val call = callManager.getCallById(callId)
        val callKind = if (call?.mxCall?.isVideoCall.orFalse()) {
            CallTileTimelineItem.CallKind.VIDEO
        } else {
            CallTileTimelineItem.CallKind.AUDIO
        }
        return when (event.root.getClearType()) {
            EventType.CALL_ANSWER -> {
                if (call == null) return null
                createCallTileTimelineItem(
                        callId = callId,
                        callStatus = CallTileTimelineItem.CallStatus.IN_CALL,
                        callKind = callKind,
                        callback = callback,
                        highlight = highlight,
                        informationData = informationData
                )
            }
            EventType.CALL_INVITE -> {
                if (call == null) return null
                createCallTileTimelineItem(
                        callId = callId,
                        callStatus = CallTileTimelineItem.CallStatus.INVITED,
                        callKind = callKind,
                        callback = callback,
                        highlight = highlight,
                        informationData = informationData
                )
            }
            EventType.CALL_REJECT -> {
                createCallTileTimelineItem(
                        callId = callId,
                        callStatus = CallTileTimelineItem.CallStatus.REJECTED,
                        callKind = callKind,
                        callback = callback,
                        highlight = highlight,
                        informationData = informationData
                )
            }
            EventType.CALL_HANGUP -> {
                createCallTileTimelineItem(
                        callId = callId,
                        callStatus = CallTileTimelineItem.CallStatus.ENDED,
                        callKind = callKind,
                        callback = callback,
                        highlight = highlight,
                        informationData = informationData
                )
            }
            else                  -> null
        }
    }

    private fun TimelineEvent.getCallSignallingContent(): CallSignallingContent? {
        return when (root.getClearType()) {
            EventType.CALL_INVITE -> root.getClearContent().toModel<CallInviteContent>()
            EventType.CALL_HANGUP -> root.getClearContent().toModel<CallHangupContent>()
            EventType.CALL_REJECT -> root.getClearContent().toModel<CallRejectContent>()
            EventType.CALL_ANSWER -> root.getClearContent().toModel<CallAnswerContent>()
            else                  -> null
        }
    }

    private fun createCallTileTimelineItem(
            callId: String,
            callKind: CallTileTimelineItem.CallKind,
            callStatus: CallTileTimelineItem.CallStatus,
            informationData: MessageInformationData,
            highlight: Boolean,
            callback: TimelineEventController.Callback?
    ): CallTileTimelineItem? {

        val userOfInterest = roomSummaryHolder.roomSummary?.toMatrixItem() ?: return null
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
                    userOfInterest = userOfInterest
            )
        }
        return CallTileTimelineItem_()
                .attributes(attributes)
                .highlighted(highlight)
                .leftGuideline(avatarSizeProvider.leftGuideline)
    }
}
