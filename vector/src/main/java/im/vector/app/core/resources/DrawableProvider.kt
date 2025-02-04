/*
 * Copyright 2020-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.core.resources

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import im.vector.app.features.themes.ThemeUtils
import javax.inject.Inject

class DrawableProvider @Inject constructor(private val context: Context) {

    fun getDrawable(@DrawableRes drawableRes: Int): Drawable? {
        return ContextCompat.getDrawable(context, drawableRes)
    }

    fun getDrawable(@DrawableRes drawableRes: Int, @ColorInt color: Int): Drawable? {
        return ContextCompat.getDrawable(context, drawableRes)?.let {
            ThemeUtils.tintDrawableWithColor(it, color)
        }
    }
}
