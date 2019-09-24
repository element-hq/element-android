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
import timber.log.Timber
import javax.inject.Inject

@ScreenScope
class ReadMarkerHelper @Inject constructor() {

    lateinit var timelineEventController: TimelineEventController
    lateinit var layoutManager: LinearLayoutManager
    var callback: Callback? = null

    private var state: RoomDetailViewState? = null

    fun updateState(state: RoomDetailViewState) {
        this.state = state
        checkJumpToReadMarkerVisibility()
    }

    fun onTimelineScrolled() {
        checkJumpToReadMarkerVisibility()
    }

    private fun checkJumpToReadMarkerVisibility() {
        val nonNullState = this.state ?: return
        val lastVisibleItem = layoutManager.findLastVisibleItemPosition()
        val readMarkerId = nonNullState.asyncRoomSummary()?.readMarkerId
        if (readMarkerId == null) {
            callback?.onVisibilityUpdated(false, null)
        }
        val positionOfReadMarker = timelineEventController.searchPositionOfEvent(readMarkerId)
        Timber.v("Position of readMarker: $positionOfReadMarker")
        Timber.v("Position of lastVisibleItem: $lastVisibleItem")
        if (positionOfReadMarker == null) {
            if (nonNullState.timeline?.isLive == true && lastVisibleItem > 0) {
                callback?.onVisibilityUpdated(true, readMarkerId)
            } else {
                callback?.onVisibilityUpdated(false, readMarkerId)
            }
        } else {
            if (positionOfReadMarker > lastVisibleItem) {
                callback?.onVisibilityUpdated(true, readMarkerId)
            } else {
                callback?.onVisibilityUpdated(false, readMarkerId)
            }
        }
    }


    interface Callback {
        fun onVisibilityUpdated(show: Boolean, readMarkerId: String?)
    }


}