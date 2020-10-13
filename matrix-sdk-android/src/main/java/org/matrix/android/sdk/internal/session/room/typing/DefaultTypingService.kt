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

import android.os.SystemClock
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import org.matrix.android.sdk.api.MatrixCallback
import org.matrix.android.sdk.api.session.room.typing.TypingService
import org.matrix.android.sdk.api.util.Cancelable
import org.matrix.android.sdk.internal.task.TaskExecutor
import org.matrix.android.sdk.internal.task.configureWith
import timber.log.Timber

/**
 * Rules:
 * - user is typing: notify the homeserver (true), at least once every 10s
 * - user stop typing: after 10s delay: notify the homeserver (false)
 * - user empty the text composer or quit the timeline screen: notify the homeserver (false)
 */
internal class DefaultTypingService @AssistedInject constructor(
        @Assisted private val roomId: String,
        private val taskExecutor: TaskExecutor,
        private val sendTypingTask: SendTypingTask
) : TypingService {

    @AssistedInject.Factory
    interface Factory {
        fun create(roomId: String): TypingService
    }

    private var currentTask: Cancelable? = null
    private var currentAutoStopTask: Cancelable? = null

    // What the homeserver knows
    private var userIsTyping = false

    // Last time the user is typing event has been sent
    private var lastRequestTimestamp: Long = 0

    override fun userIsTyping() {
        scheduleAutoStop()

        val now = SystemClock.elapsedRealtime()

        if (userIsTyping && now < lastRequestTimestamp + MIN_DELAY_BETWEEN_TWO_USER_IS_TYPING_REQUESTS_MILLIS) {
            Timber.d("Typing: Skip start request")
            return
        }

        Timber.d("Typing: Send start request")
        userIsTyping = true
        lastRequestTimestamp = now

        currentTask?.cancel()

        val params = SendTypingTask.Params(roomId, true)
        currentTask = sendTypingTask
                .configureWith(params)
                .executeBy(taskExecutor)
    }

    override fun userStopsTyping() {
        if (!userIsTyping) {
            Timber.d("Typing: Skip stop request")
            return
        }

        Timber.d("Typing: Send stop request")
        userIsTyping = false
        lastRequestTimestamp = 0

        currentAutoStopTask?.cancel()
        currentTask?.cancel()

        val params = SendTypingTask.Params(roomId, false)
        currentTask = sendTypingTask
                .configureWith(params)
                .executeBy(taskExecutor)
    }

    private fun scheduleAutoStop() {
        Timber.d("Typing: Schedule auto stop")
        currentAutoStopTask?.cancel()

        val params = SendTypingTask.Params(
                roomId,
                false,
                delay = MIN_DELAY_TO_SEND_STOP_TYPING_REQUEST_WHEN_NO_USER_ACTIVITY_MILLIS)
        currentAutoStopTask = sendTypingTask
                .configureWith(params) {
                    callback = object : MatrixCallback<Unit> {
                        override fun onSuccess(data: Unit) {
                            userIsTyping = false
                        }
                    }
                }
                .executeBy(taskExecutor)
    }

    companion object {
        private const val MIN_DELAY_BETWEEN_TWO_USER_IS_TYPING_REQUESTS_MILLIS = 10_000L
        private const val MIN_DELAY_TO_SEND_STOP_TYPING_REQUEST_WHEN_NO_USER_ACTIVITY_MILLIS = 10_000L
    }
}
