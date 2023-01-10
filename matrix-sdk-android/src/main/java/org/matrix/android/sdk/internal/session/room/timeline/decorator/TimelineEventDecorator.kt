/*
 * Copyright (c) 2022 The Matrix.org Foundation C.I.C.
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

package org.matrix.android.sdk.internal.session.room.timeline.decorator

import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent

/**
 * This interface can be used to make a copy of a TimelineEvent with new data, before the event is posted to the timeline.
 */
internal fun interface TimelineEventDecorator {
    fun decorate(timelineEvent: TimelineEvent): TimelineEvent
}

/**
 * This is an implementation of [TimelineEventDecorator] which chains calls to decorators.
 */
internal class TimelineEventDecoratorChain(private val decorators: List<TimelineEventDecorator>) : TimelineEventDecorator {

    override fun decorate(timelineEvent: TimelineEvent): TimelineEvent {
        var decorated = timelineEvent
        val iterator = decorators.iterator()
        while (iterator.hasNext()) {
            val decorator = iterator.next()
            decorated = decorator.decorate(decorated)
        }
        return decorated
    }
}
