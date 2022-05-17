/*
 * Copyright (c) 2021 The Matrix.org Foundation C.I.C.
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
