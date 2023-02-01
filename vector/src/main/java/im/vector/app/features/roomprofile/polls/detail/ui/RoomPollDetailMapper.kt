/*
 * Copyright (c) 2023 New Vector Ltd
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

package im.vector.app.features.roomprofile.polls.detail.ui

import im.vector.app.core.extensions.getVectorLastMessageContent
import im.vector.app.features.home.room.detail.timeline.factory.PollItemViewStateFactory
import im.vector.app.features.home.room.detail.timeline.helper.PollResponseDataFactory
import org.matrix.android.sdk.api.session.room.model.message.MessagePollContent
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import timber.log.Timber
import javax.inject.Inject

// TODO add unit tests
class RoomPollDetailMapper @Inject constructor(
        private val pollResponseDataFactory: PollResponseDataFactory,
        private val pollItemViewStateFactory: PollItemViewStateFactory,
) {

    fun map(timelineEvent: TimelineEvent): RoomPollDetail? {
        val eventId = timelineEvent.root.eventId.orEmpty()
        val result = runCatching {
            val content = timelineEvent.getVectorLastMessageContent()
            val pollResponseData = pollResponseDataFactory.create(timelineEvent)
            return if (eventId.isNotEmpty() && content is MessagePollContent) {
                // we assume poll message has been sent here
                val pollItemViewState = pollItemViewStateFactory.create(
                        pollContent = content,
                        pollResponseData = pollResponseData,
                        isSent = true,
                )
                RoomPollDetail(
                        isEnded = pollResponseData?.isClosed == true,
                        pollItemViewState = pollItemViewState,
                )
            } else {
                Timber.w("missing mandatory info about poll event with id=$eventId")
                null
            }
        }

        if (result.isFailure) {
            Timber.w("failed to map event with id $eventId")
        }
        return result.getOrNull()
    }
}
