/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.ui.views

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.View
import nl.dionsegijn.konfetti.xml.KonfettiView

/**
 * Konfetti workaround to avoid crashes on API 21/22
 * https://github.com/DanielMartinus/Konfetti/issues/297
 */
class CompatKonfetti @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null
) : KonfettiView(context, attrs) {

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        when (Build.VERSION.SDK_INT) {
            Build.VERSION_CODES.LOLLIPOP, Build.VERSION_CODES.LOLLIPOP_MR1 -> safeOnVisibilityChanged(changedView, visibility)
            else -> super.onVisibilityChanged(changedView, visibility)
        }
    }

    private fun safeOnVisibilityChanged(changedView: View, visibility: Int) {
        runCatching { super.onVisibilityChanged(changedView, visibility) }
    }
}
