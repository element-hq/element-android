/*
 * Copyright (c) 2022 New Vector Ltd
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

package im.vector.app.ui.robot.interaction

import android.view.ViewConfiguration
import androidx.test.espresso.UiController
import androidx.test.espresso.action.MotionEvents
import androidx.test.espresso.action.Tapper

object VeryLongClick : Tapper {
    override fun sendTap(uiController: UiController?,
                         coordinates: FloatArray?,
                         precision: FloatArray?,
                         inputDevice: Int,
                         buttonState: Int): Tapper.Status {
        checkNotNull(uiController)
        checkNotNull(coordinates)
        checkNotNull(precision)

        val downEvent = MotionEvents.sendDown(uiController, coordinates, precision, inputDevice, buttonState).down
        try {
            // Duration before a press turns into a long press.
            // Factor 1.5 is needed, otherwise a long press is not safely detected.
            // See android.test.TouchUtils longClickView
            // Factor 10 is needed to simulate user still pressing after the longClick is detected
            val longPressTimeout = (ViewConfiguration.getLongPressTimeout() * 10f).toLong()
            uiController.loopMainThreadForAtLeast(longPressTimeout)
            if (!MotionEvents.sendUp(uiController, downEvent)) {
                MotionEvents.sendCancel(uiController, downEvent)
                return Tapper.Status.FAILURE
            }
        } finally {
            downEvent!!.recycle()
        }
        return Tapper.Status.SUCCESS
    }

    override fun sendTap(uiController: UiController?, coordinates: FloatArray?, precision: FloatArray?): Tapper.Status {
        return sendTap(uiController, coordinates, precision, 0, 0)
    }
}
