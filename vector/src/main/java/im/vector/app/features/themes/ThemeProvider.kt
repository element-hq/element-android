/*
 * Copyright 2021-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.themes

import android.content.Context
import javax.inject.Inject

/**
 * Injectable class to encapsulate ThemeUtils call...
 */
class ThemeProvider @Inject constructor(
        private val context: Context
) {
    fun isLightTheme() = ThemeUtils.isLightTheme(context)
}
