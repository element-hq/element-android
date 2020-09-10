/*
 * Copyright 2018 New Vector Ltd
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

package im.vector.app.features.settings

import android.content.Context
import androidx.annotation.StringRes
import androidx.core.content.edit
import im.vector.app.R
import im.vector.app.core.di.DefaultSharedPreferences

/**
 * Object to manage the Font Scale choice of the user
 */
object FontScale {
    // Key for the SharedPrefs
    private const val APPLICATION_FONT_SCALE_KEY = "APPLICATION_FONT_SCALE_KEY"

    data class FontScaleValue(
            val index: Int,
            // Possible values for the SharedPrefs
            val preferenceValue: String,
            val scale: Float,
            @StringRes
            val nameResId: Int
    )

    private val fontScaleValues = listOf(
            FontScaleValue(0, "FONT_SCALE_TINY", 0.70f, R.string.tiny),
            FontScaleValue(1, "FONT_SCALE_SMALL", 0.85f, R.string.small),
            FontScaleValue(2, "FONT_SCALE_NORMAL", 1.00f, R.string.normal),
            FontScaleValue(3, "FONT_SCALE_LARGE", 1.15f, R.string.large),
            FontScaleValue(4, "FONT_SCALE_LARGER", 1.30f, R.string.larger),
            FontScaleValue(5, "FONT_SCALE_LARGEST", 1.45f, R.string.largest),
            FontScaleValue(6, "FONT_SCALE_HUGE", 1.60f, R.string.huge)
    )

    private val normalFontScaleValue = fontScaleValues[2]

    /**
     * Get the font scale value from SharedPrefs. Init the SharedPrefs if necessary
     *
     * @return the font scale value
     */
    fun getFontScaleValue(context: Context): FontScaleValue {
        val preferences = DefaultSharedPreferences.getInstance(context)

        return if (APPLICATION_FONT_SCALE_KEY !in preferences) {
            val fontScale = context.resources.configuration.fontScale

            (fontScaleValues.firstOrNull { it.scale == fontScale } ?: normalFontScaleValue)
                    .also { preferences.edit { putString(APPLICATION_FONT_SCALE_KEY, it.preferenceValue) } }
        } else {
            val pref = preferences.getString(APPLICATION_FONT_SCALE_KEY, null)
            fontScaleValues.firstOrNull { it.preferenceValue == pref } ?: normalFontScaleValue
        }
    }

    fun updateFontScale(context: Context, index: Int) {
        fontScaleValues.getOrNull(index)?.let {
            saveFontScaleValue(context, it)
        }
    }

    /**
     * Store the font scale vale
     *
     * @param fontScaleValue the font scale value to store
     */
    private fun saveFontScaleValue(context: Context, fontScaleValue: FontScaleValue) {
        DefaultSharedPreferences.getInstance(context)
                .edit { putString(APPLICATION_FONT_SCALE_KEY, fontScaleValue.preferenceValue) }
    }
}
