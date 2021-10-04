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
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.room.typing.TypingService
import timber.log.Timber

/**
 * Rules:
 * - user is typing: notify the homeserver (true), at least once every 10s
 * - user stop typing: after 10s delay: notify the homeserver (false)
 * - user empty the text composer or quit the timeline screen: notify the homeserver (false)
 */
internal class DefaultTypingService @AssistedInject constructor(
        @Assisted private val roomId: String,
        private val sendTypingTask: SendTypingTask
) : TypingService {

    @AssistedFactory
    interface Factory {
        fun create(roomId: String): DefaultTypingService
    }

    private val coroutineScope = CoroutineScope(Job())
    private var currentTask: Job? = null

    // What the homeserver knows
    private var userIsTyping = false

    // Last time the user is typing event has been sent
    private var lastRequestTimestamp: Long = 0

    /**
     * Notify to the server that the user is typing and schedule the auto typing off
     */
    override fun userIsTyping() {
        val now = SystemClock.elapsedRealtime()
        currentTask?.cancel()
        currentTask = coroutineScope.launch {
            if (userIsTyping && now < lastRequestTimestamp + MIN_DELAY_BETWEEN_TWO_USER_IS_TYPING_REQUESTS_MILLIS) {
                Timber.d("Typing: Skip start request")
            } else {
                Timber.d("Typing: Send start request")
                lastRequestTimestamp = now
                sendRequest(true)
            }
            delay(MIN_DELAY_TO_SEND_STOP_TYPING_REQUEST_WHEN_NO_USER_ACTIVITY_MILLIS)
            Timber.d("Typing: auto stop")
            sendRequest(false)
        }
    }

    override fun userStopsTyping() {
        if (!userIsTyping) {
            Timber.d("Typing: Skip stop request")
            return
        }

        Timber.d("Typing: Send stop request")
        lastRequestTimestamp = 0

        currentTask?.cancel()
        currentTask = coroutineScope.launch {
            sendRequest(false)
        }
    }

    private suspend fun sendRequest(isTyping: Boolean) {
        try {
            sendTypingTask.execute(SendTypingTask.Params(roomId, isTyping))
            userIsTyping = isTyping
        } catch (failure: Throwable) {
            // Ignore network error, etc...
            Timber.w(failure, "Unable to send typing request")
        }
    }

    companion object {
        private const val MIN_DELAY_BETWEEN_TWO_USER_IS_TYPING_REQUESTS_MILLIS = 10_000L
        private const val MIN_DELAY_TO_SEND_STOP_TYPING_REQUEST_WHEN_NO_USER_ACTIVITY_MILLIS = 10_000L
    }
}
