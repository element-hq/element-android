/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.riotx.features.home.room.detail.timeline.helper

import com.airbnb.epoxy.VisibilityState
import im.vector.matrix.android.api.session.room.timeline.TimelineEvent
import im.vector.riotx.core.epoxy.VectorEpoxyModel
import im.vector.riotx.features.home.room.detail.timeline.TimelineEventController

class ReadMarkerVisibilityStateChangedListener(private val callback: TimelineEventController.Callback?)
    : VectorEpoxyModel.OnVisibilityStateChangedListener {

    private var dispatched: Boolean = false

    override fun onVisibilityStateChanged(visibilityState: Int) {
        if (visibilityState == VisibilityState.VISIBLE && !dispatched) {
            dispatched = true
            callback?.onReadMarkerDisplayed()
        }
    }
}

class TimelineEventVisibilityStateChangedListener(private val callback: TimelineEventController.Callback?,
                                                  private val event: TimelineEvent)
    : VectorEpoxyModel.OnVisibilityStateChangedListener {

    override fun onVisibilityStateChanged(visibilityState: Int) {
        if (visibilityState == VisibilityState.VISIBLE) {
            callback?.onEventVisible(event)
        } else if (visibilityState == VisibilityState.INVISIBLE) {
            callback?.onEventInvisible(event)
        }
    }
}

class MergedTimelineEventVisibilityStateChangedListener(private val callback: TimelineEventController.Callback?,
                                                        private val events: List<TimelineEvent>)
    : VectorEpoxyModel.OnVisibilityStateChangedListener {

    override fun onVisibilityStateChanged(visibilityState: Int) {
        if (visibilityState == VisibilityState.VISIBLE) {
            events.forEach { callback?.onEventVisible(it) }
        } else if (visibilityState == VisibilityState.INVISIBLE) {
            events.forEach { callback?.onEventInvisible(it) }
        }
    }
}
