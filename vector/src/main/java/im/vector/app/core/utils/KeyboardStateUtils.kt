/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
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
