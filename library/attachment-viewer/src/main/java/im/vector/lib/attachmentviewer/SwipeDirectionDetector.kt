/*
 * Copyright (c) 2020 New Vector Ltd
 * Copyright (C) 2018 stfalcon.com
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

package im.vector.lib.attachmentviewer

import android.content.Context
import android.view.MotionEvent
import kotlin.math.sqrt

class SwipeDirectionDetector(
        context: Context,
        private val onDirectionDetected: (SwipeDirection) -> Unit
) {

    private val touchSlop: Int = android.view.ViewConfiguration.get(context).scaledTouchSlop
    private var startX: Float = 0f
    private var startY: Float = 0f
    private var isDetected: Boolean = false

    fun handleTouchEvent(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN                          -> {
                startX = event.x
                startY = event.y
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                if (!isDetected) {
                    onDirectionDetected(SwipeDirection.NotDetected)
                }
                startY = 0.0f
                startX = startY
                isDetected = false
            }
            MotionEvent.ACTION_MOVE                          -> if (!isDetected && getEventDistance(event) > touchSlop) {
                isDetected = true
                onDirectionDetected(getDirection(startX, startY, event.x, event.y))
            }
        }
    }

    /**
     * Given two points in the plane p1=(x1, x2) and p2=(y1, y1), this method
     * returns the direction that an arrow pointing from p1 to p2 would have.
     *
     * @param x1 the x position of the first point
     * @param y1 the y position of the first point
     * @param x2 the x position of the second point
     * @param y2 the y position of the second point
     * @return the direction
     */
    private fun getDirection(x1: Float, y1: Float, x2: Float, y2: Float): SwipeDirection {
        val angle = getAngle(x1, y1, x2, y2)
        return SwipeDirection.fromAngle(angle)
    }

    /**
     * Finds the angle between two points in the plane (x1,y1) and (x2, y2)
     * The angle is measured with 0/360 being the X-axis to the right, angles
     * increase counter clockwise.
     *
     * @param x1 the x position of the first point
     * @param y1 the y position of the first point
     * @param x2 the x position of the second point
     * @param y2 the y position of the second point
     * @return the angle between two points
     */
    private fun getAngle(x1: Float, y1: Float, x2: Float, y2: Float): Double {
        val rad = Math.atan2((y1 - y2).toDouble(), (x2 - x1).toDouble()) + Math.PI
        return (rad * 180 / Math.PI + 180) % 360
    }

    private fun getEventDistance(ev: MotionEvent): Float {
        val dx = ev.getX(0) - startX
        val dy = ev.getY(0) - startY
        return sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }
}
