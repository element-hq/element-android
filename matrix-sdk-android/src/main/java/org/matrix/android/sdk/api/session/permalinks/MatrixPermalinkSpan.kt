/*
 * Copyright 2025 New Vector Ltd.
 * Copyright 2020 The Matrix.org Foundation C.I.C.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package org.matrix.android.sdk.api.session.permalinks

import android.text.style.ClickableSpan
import android.view.View
import org.matrix.android.sdk.api.session.permalinks.MatrixPermalinkSpan.Callback

/**
 * This MatrixPermalinkSpan is a clickable span which use a [Callback] to communicate back.
 * @property url the permalink url tied to the span
 * @property callback the callback to use.
 */
class MatrixPermalinkSpan(
        private val url: String,
        private val callback: Callback? = null
) : ClickableSpan() {

    interface Callback {
        fun onUrlClicked(url: String)
    }

    override fun onClick(widget: View) {
        callback?.onUrlClicked(url)
    }
}
