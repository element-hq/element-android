/*
 * Copyright (c) 2021 New Vector Ltd
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

package im.vector.app.features.home.room.detail.composer.voice

import android.content.res.Resources
import android.view.MotionEvent
import im.vector.app.R
import im.vector.app.core.utils.DimensionConverter
import im.vector.app.features.home.room.detail.composer.voice.VoiceMessageRecorderView.DraggingState
import im.vector.app.features.home.room.detail.composer.voice.VoiceMessageRecorderView.RecordingState
import kotlin.math.abs

class DraggableStateProcessor(
        resources: Resources,
        dimensionConverter: DimensionConverter,
) {

    private val minimumMove = dimensionConverter.dpToPx(16)
    private val distanceToLock = dimensionConverter.dpToPx(48).toFloat()
    private val distanceToCancel = dimensionConverter.dpToPx(120).toFloat()
    private val rtlXMultiplier = resources.getInteger(R.integer.rtl_x_multiplier)

    private var firstX: Float = 0f
    private var firstY: Float = 0f
    private var lastX: Float = 0f
    private var lastY: Float = 0f
    private var lastDistanceX: Float = 0f
    private var lastDistanceY: Float = 0f

    fun reset(event: MotionEvent) {
        firstX = event.rawX
        firstY = event.rawY
        lastX = firstX
        lastY = firstY
        lastDistanceX = 0F
        lastDistanceY = 0F
    }

    fun process(event: MotionEvent, recordingState: RecordingState): RecordingState {
        val currentX = event.rawX
        val currentY = event.rawY
        val distanceX = abs(firstX - currentX)
        val distanceY = abs(firstY - currentY)
        return nextRecordingState(recordingState, currentX, currentY, distanceX, distanceY).also {
            lastX = currentX
            lastY = currentY
            lastDistanceX = distanceX
            lastDistanceY = distanceY
        }
    }

    private fun nextRecordingState(recordingState: RecordingState, currentX: Float, currentY: Float, distanceX: Float, distanceY: Float): RecordingState {
        return when (recordingState) {
            RecordingState.Started      -> {
                // Determine if cancelling or locking for the first move action.
                if (((currentX < firstX && rtlXMultiplier == 1) || (currentX > firstX && rtlXMultiplier == -1)) && distanceX > distanceY && distanceX > lastDistanceX) {
                    DraggingState.Cancelling(distanceX)
                } else if (currentY < firstY && distanceY > distanceX && distanceY > lastDistanceY) {
                    DraggingState.Locking(distanceY)
                } else {
                    recordingState
                }
            }
            is DraggingState.Cancelling -> {
                // Check if cancelling conditions met, also check if it should be initial state
                if (distanceX < minimumMove && distanceX < lastDistanceX) {
                    RecordingState.Started
                } else if (shouldCancelRecording(distanceX)) {
                    RecordingState.Cancelled
                } else {
                    DraggingState.Cancelling(distanceX)
                }
            }
            is DraggingState.Locking    -> {
                // Check if locking conditions met, also check if it should be initial state
                if (distanceY < minimumMove && distanceY < lastDistanceY) {
                    RecordingState.Started
                } else if (shouldLockRecording(distanceY)) {
                    RecordingState.Locked
                } else {
                    DraggingState.Locking(distanceY)
                }
            }
            else                        -> {
                recordingState
            }
        }
    }

    private fun shouldCancelRecording(distanceX: Float): Boolean {
        return distanceX >= distanceToCancel
    }

    private fun shouldLockRecording(distanceY: Float): Boolean {
        return distanceY >= distanceToLock
    }
}

