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
package im.vector.riotx.features.home.room.detail

import androidx.recyclerview.widget.LinearLayoutManager
import im.vector.riotx.core.di.ScreenScope
import im.vector.riotx.features.home.room.detail.timeline.TimelineEventController
import javax.inject.Inject

@ScreenScope
class ReadMarkerHelper @Inject constructor() {

    lateinit var timelineEventController: TimelineEventController
    lateinit var layoutManager: LinearLayoutManager
    var callback: Callback? = null
    private var jumpToReadMarkerVisible = false
    private var state: RoomDetailViewState? = null

    fun updateWith(newState: RoomDetailViewState) {
        state = newState
        checkJumpToReadMarkerVisibility()
    }

    private fun checkJumpToReadMarkerVisibility() {
        val nonNullState = this.state ?: return
        val lastVisibleItem = layoutManager.findLastVisibleItemPosition()
        val readMarkerId = nonNullState.asyncRoomSummary()?.readMarkerId
        val newJumpToReadMarkerVisible = if (readMarkerId == null) {
            false
        } else {
            val correctedReadMarkerId = nonNullState.timeline?.getFirstDisplayableEventId(readMarkerId)
                    ?: readMarkerId
            val positionOfReadMarker = timelineEventController.searchPositionOfEvent(correctedReadMarkerId)
            if (positionOfReadMarker == null) {
                nonNullState.timeline?.isLive == true && lastVisibleItem > 0
            } else {
                positionOfReadMarker > lastVisibleItem
            }
        }
        if (newJumpToReadMarkerVisible != jumpToReadMarkerVisible) {
            jumpToReadMarkerVisible = newJumpToReadMarkerVisible
            callback?.onJumpToReadMarkerVisibilityUpdate(jumpToReadMarkerVisible, readMarkerId)
        }
    }

    interface Callback {
        fun onJumpToReadMarkerVisibilityUpdate(show: Boolean, readMarkerId: String?)
    }
}
