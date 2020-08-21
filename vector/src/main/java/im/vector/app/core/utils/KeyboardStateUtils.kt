/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.core.utils

import android.app.Activity
import android.graphics.Rect
import android.view.View
import android.view.ViewTreeObserver

class KeyboardStateUtils(activity: Activity) : ViewTreeObserver.OnGlobalLayoutListener {

    private val contentView: View = activity.findViewById<View>(android.R.id.content).also {
        it.viewTreeObserver.addOnGlobalLayoutListener(this)
    }
    var isKeyboardShowing: Boolean = false
        private set

    override fun onGlobalLayout() {
        val rect = Rect()
        contentView.getWindowVisibleDisplayFrame(rect)
        val screenHeight = contentView.rootView.height

        val keypadHeight = screenHeight - rect.bottom
        isKeyboardShowing = keypadHeight > screenHeight * 0.15
    }
}
