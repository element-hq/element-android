/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.home.room.detail.composer.voice

import android.content.res.Resources
import android.view.MotionEvent
import im.vector.app.core.utils.DimensionConverter
import im.vector.app.features.home.room.detail.composer.voice.VoiceMessageRecorderView.DraggingState
import kotlin.math.absoluteValue

class DraggableStateProcessor(
        resources: Resources,
        dimensionConverter: DimensionConverter,
) {

    private val distanceToLock = dimensionConverter.dpToPx(48).toFloat()
    private val distanceToCancel = dimensionConverter.dpToPx(120).toFloat()
    private val rtlXMultiplier = resources.getInteger(im.vector.lib.ui.styles.R.integer.rtl_x_multiplier)

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
        val distanceX = (firstX - currentX).absoluteValue
        val distanceY = firstY - currentY
        return draggingState.nextDragState(currentX, currentY, distanceX, distanceY).also {
            lastDistanceX = distanceX
            lastDistanceY = distanceY
        }
    }

    private fun DraggingState.nextDragState(currentX: Float, currentY: Float, distanceX: Float, distanceY: Float): DraggingState {
        return when (this) {
            DraggingState.Ready -> {
                when {
                    isDraggingToCancel(currentX, distanceX, distanceY) -> DraggingState.Cancelling(distanceX)
                    isDraggingToLock(currentY, distanceX, distanceY) -> DraggingState.Locking(distanceY)
                    else -> DraggingState.Ready
                }
            }
            is DraggingState.Cancelling -> {
                when {
                    isDraggingToLock(currentY, distanceX, distanceY) -> DraggingState.Locking(distanceY)
                    shouldCancelRecording(distanceX) -> DraggingState.Cancel
                    else -> DraggingState.Cancelling(distanceX)
                }
            }
            is DraggingState.Locking -> {
                when {
                    isDraggingToCancel(currentX, distanceX, distanceY) -> DraggingState.Cancelling(distanceX)
                    shouldLockRecording(distanceY) -> DraggingState.Lock
                    else -> DraggingState.Locking(distanceY)
                }
            }
            else -> {
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
