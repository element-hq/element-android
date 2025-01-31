/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.session.call

import org.matrix.android.sdk.api.MatrixConfiguration
import org.matrix.android.sdk.api.session.call.CallIdGenerator
import org.matrix.android.sdk.api.session.call.MxCall
import org.matrix.android.sdk.api.session.room.model.call.CallCapabilities
import org.matrix.android.sdk.api.session.room.model.call.CallInviteContent
import org.matrix.android.sdk.api.session.room.model.call.CallSignalingContent
import org.matrix.android.sdk.internal.di.DeviceId
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.session.call.model.MxCallImpl
import org.matrix.android.sdk.internal.session.profile.GetProfileInfoTask
import org.matrix.android.sdk.internal.session.room.send.LocalEchoEventFactory
import org.matrix.android.sdk.internal.session.room.send.queue.EventSenderProcessor
import org.matrix.android.sdk.internal.util.time.Clock
import javax.inject.Inject

internal class MxCallFactory @Inject constructor(
        @DeviceId private val deviceId: String?,
        private val localEchoEventFactory: LocalEchoEventFactory,
        private val eventSenderProcessor: EventSenderProcessor,
        private val matrixConfiguration: MatrixConfiguration,
        private val getProfileInfoTask: GetProfileInfoTask,
        @UserId private val userId: String,
        private val clock: Clock,
) {

    fun createIncomingCall(roomId: String, opponentUserId: String, content: CallInviteContent): MxCall? {
        content.callId ?: return null
        return MxCallImpl(
                callId = content.callId,
                isOutgoing = false,
                roomId = roomId,
                userId = userId,
                ourPartyId = deviceId ?: "",
                isVideoCall = content.isVideo(),
                localEchoEventFactory = localEchoEventFactory,
                eventSenderProcessor = eventSenderProcessor,
                matrixConfiguration = matrixConfiguration,
                getProfileInfoTask = getProfileInfoTask,
                clock = clock,
        ).apply {
            updateOpponentData(opponentUserId, content, content.capabilities)
        }
    }

    fun createOutgoingCall(roomId: String, opponentUserId: String, isVideoCall: Boolean): MxCall {
        return MxCallImpl(
                callId = CallIdGenerator.generate(),
                isOutgoing = true,
                roomId = roomId,
                userId = userId,
                ourPartyId = deviceId ?: "",
                isVideoCall = isVideoCall,
                localEchoEventFactory = localEchoEventFactory,
                eventSenderProcessor = eventSenderProcessor,
                matrixConfiguration = matrixConfiguration,
                getProfileInfoTask = getProfileInfoTask,
                clock = clock,
        ).apply {
            // Setup with this userId, might be updated when processing the Answer event
            this.opponentUserId = opponentUserId
        }
    }

    fun updateOutgoingCallWithOpponentData(
            call: MxCall,
            userId: String,
            content: CallSignalingContent,
            callCapabilities: CallCapabilities?
    ) {
        (call as? MxCallImpl)?.updateOpponentData(userId, content, callCapabilities)
    }
}
