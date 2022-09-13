/*
 * Copyright 2021 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.api.session.threads

import org.matrix.android.sdk.api.session.events.model.Event
import org.matrix.android.sdk.api.session.room.sender.SenderInfo

/**
 * This class contains all the details needed for threads.
 * Is is mainly used from within an Event.
 */
data class ThreadDetails(
        val isRootThread: Boolean = false,
        val numberOfThreads: Int = 0,
        val threadSummarySenderInfo: SenderInfo? = null,
        val threadSummaryLatestEvent: Event? = null,
        val lastMessageTimestamp: Long? = null,
        val threadNotificationState: ThreadNotificationState = ThreadNotificationState.NO_NEW_MESSAGE,
        val isThread: Boolean = false,
        val lastRootThreadEdition: String? = null
)
