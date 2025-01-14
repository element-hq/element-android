/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.epoxy

import android.view.View
import android.widget.TextView
import im.vector.app.core.utils.DebouncedClickListener

/**
 * View.OnClickListener lambda.
 */
typealias ClickListener = (View) -> Unit

fun View.onClick(listener: ClickListener?) {
    if (listener == null) {
        setOnClickListener(null)
    } else {
        setOnClickListener(DebouncedClickListener(listener))
    }
}

fun TextView.onLongClickIgnoringLinks(listener: View.OnLongClickListener?) {
    if (listener == null) {
        setOnLongClickListener(null)
    } else {
        setOnLongClickListener(object : View.OnLongClickListener {
            override fun onLongClick(v: View): Boolean {
                if (hasLongPressedLink()) {
                    return false
                }
                return listener.onLongClick(v)
            }

            /**
             * Infer that a Clickable span has been click by the presence of a selection.
             */
            private fun hasLongPressedLink() = selectionStart != -1 || selectionEnd != -1
        })
    }
}

/**
 * Simple Text listener lambda.
 */
typealias TextListener = (String) -> Unit
