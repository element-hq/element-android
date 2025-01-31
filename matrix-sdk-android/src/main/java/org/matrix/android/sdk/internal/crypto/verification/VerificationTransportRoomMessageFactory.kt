/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2021 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.internal.crypto.verification

import kotlinx.coroutines.CoroutineScope
import org.matrix.android.sdk.internal.crypto.tasks.SendVerificationMessageTask
import org.matrix.android.sdk.internal.di.DeviceId
import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.session.room.send.LocalEchoEventFactory
import org.matrix.android.sdk.internal.util.time.Clock
import javax.inject.Inject

internal class VerificationTransportRoomMessageFactory @Inject constructor(
        private val sendVerificationMessageTask: SendVerificationMessageTask,
        @UserId
        private val userId: String,
        @DeviceId
        private val deviceId: String?,
        private val localEchoEventFactory: LocalEchoEventFactory,
        private val cryptoCoroutineScope: CoroutineScope,
        private val clock: Clock,
) {

    fun createTransport(roomId: String, tx: DefaultVerificationTransaction?): VerificationTransportRoomMessage {
        return VerificationTransportRoomMessage(
                sendVerificationMessageTask = sendVerificationMessageTask,
                userId = userId,
                userDeviceId = deviceId,
                roomId = roomId,
                localEchoEventFactory = localEchoEventFactory,
                tx = tx,
                cryptoCoroutineScope = cryptoCoroutineScope,
                clock = clock,
        )
    }
}
