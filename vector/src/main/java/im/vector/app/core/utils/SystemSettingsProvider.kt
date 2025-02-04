/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.utils

import android.content.Context
import android.provider.Settings
import javax.inject.Inject

/**
 * A helper to get system settings.
 */
interface SystemSettingsProvider {

    /**
     * @return system setting for font scale
     */
    fun getSystemFontScale(): Float
}

class AndroidSystemSettingsProvider @Inject constructor(
        private val context: Context,
) : SystemSettingsProvider {

    override fun getSystemFontScale(): Float {
        return Settings.System.getFloat(context.contentResolver, Settings.System.FONT_SCALE, 1f)
    }
}
