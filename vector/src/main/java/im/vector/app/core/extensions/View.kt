/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.extensions

import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.getSystemService

fun View.hideKeyboard() {
    val imm = context?.getSystemService<InputMethodManager>()
    imm?.hideSoftInputFromWindow(windowToken, 0)
}

fun View.showKeyboard(andRequestFocus: Boolean = false) {
    if (andRequestFocus) {
        requestFocus()
    }
    val imm = context?.getSystemService<InputMethodManager>()
    imm?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

fun View.setHorizontalPadding(padding: Int) {
    setPadding(
            padding,
            paddingTop,
            padding,
            paddingBottom
    )
}
