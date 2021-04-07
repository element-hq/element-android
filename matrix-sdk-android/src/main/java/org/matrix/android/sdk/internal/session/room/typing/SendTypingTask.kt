/*
 * Copyright 2020 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room.typing

import org.matrix.android.sdk.internal.di.UserId
import org.matrix.android.sdk.internal.network.executeRequest
import org.matrix.android.sdk.internal.session.room.RoomAPI
import org.matrix.android.sdk.internal.task.Task
import kotlinx.coroutines.delay
import org.matrix.android.sdk.internal.network.GlobalErrorReceiver
import javax.inject.Inject

internal interface SendTypingTask : Task<SendTypingTask.Params, Unit> {

    data class Params(
            val roomId: String,
            val isTyping: Boolean,
            val typingTimeoutMillis: Int? = 30_000,
            // Optional delay before sending the request to the homeserver
            val delay: Long? = null
    )
}

internal class DefaultSendTypingTask @Inject constructor(
        private val roomAPI: RoomAPI,
        @UserId private val userId: String,
        private val globalErrorReceiver: GlobalErrorReceiver
) : SendTypingTask {

    override suspend fun execute(params: SendTypingTask.Params) {
        delay(params.delay ?: -1)

        executeRequest(globalErrorReceiver) {
            roomAPI.sendTypingState(
                    params.roomId,
                    userId,
                    TypingBody(params.isTyping, params.typingTimeoutMillis?.takeIf { params.isTyping })
            )
        }
    }
}
