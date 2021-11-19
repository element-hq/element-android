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

class DraggableStateProcessor(
        resources: Resources,
        dimensionConverter: DimensionConverter,
) {

    private val distanceToLock = dimensionConverter.dpToPx(48).toFloat()
    private val distanceToCancel = dimensionConverter.dpToPx(120).toFloat()
    private val rtlXMultiplier = resources.getInteger(R.integer.rtl_x_multiplier)

    private var firstX: Float = 0f
    private var firstY: Float = 0f
    private var lastDistanceX: Float = 0f
    private var lastDistanceY: Float = 0f

    fun initialize(event: MotionEvent) {
        firstX = event.rawX
        firstY = event.rawY
        lastDistanceX = 0F
        lastDistanceY = 0F
    }

    fun process(event: MotionEvent, draggingState: DraggingState): DraggingState {
        val currentX = event.rawX
        val currentY = event.rawY
        val distanceX = firstX - currentX
        val distanceY = firstY - currentY
        return draggingState.nextDragState(currentX, currentY, distanceX, distanceY).also {
            lastDistanceX = distanceX
            lastDistanceY = distanceY
        }
    }

    private fun DraggingState.nextDragState(currentX: Float, currentY: Float, distanceX: Float, distanceY: Float): DraggingState {
        return when (this) {
            DraggingState.Ready         -> {
                when {
                    isDraggingToCancel(currentX, distanceX, distanceY) -> DraggingState.Cancelling(distanceX)
                    isDraggingToLock(currentY, distanceX, distanceY)   -> DraggingState.Locking(distanceY)
                    else                                               -> DraggingState.Ready
                }
            }
            is DraggingState.Cancelling -> {
                when {
                    isDraggingToLock(currentY, distanceX, distanceY) -> DraggingState.Locking(distanceY)
                    shouldCancelRecording(distanceX)                 -> DraggingState.Cancel
                    else                                             -> DraggingState.Cancelling(distanceX)
                }
            }
            is DraggingState.Locking    -> {
                when {
                    isDraggingToCancel(currentX, distanceX, distanceY) -> DraggingState.Cancelling(distanceX)
                    shouldLockRecording(distanceY)                     -> DraggingState.Lock
                    else                                               -> DraggingState.Locking(distanceY)
                }
            }
            else                        -> {
                this
            }
        }
    }

    private fun isDraggingToLock(currentY: Float, distanceX: Float, distanceY: Float) = (currentY < firstY) &&
            distanceY > distanceX && distanceY > lastDistanceY

    private fun isDraggingToCancel(currentX: Float, distanceX: Float, distanceY: Float) = isDraggingHorizontal(currentX) &&
            distanceX > distanceY && distanceX > lastDistanceX

    private fun isDraggingHorizontal(currentX: Float) = (currentX < firstX && rtlXMultiplier == 1) || (currentX > firstX && rtlXMultiplier == -1)

    private fun shouldCancelRecording(distanceX: Float): Boolean {
        return distanceX >= distanceToCancel
    }

    private fun shouldLockRecording(distanceY: Float): Boolean {
        return distanceY >= distanceToLock
    }
}
