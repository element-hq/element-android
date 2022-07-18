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

import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.core.content.edit
import im.vector.app.R
import im.vector.app.core.di.DefaultPreferences
import im.vector.app.core.utils.SystemSettingsProvider
import javax.inject.Inject

/**
 * Stores and returns font scale settings using shared preferences.
 */
interface FontScalePreferences {
    /** Defines whether to use system settings for font scale or not.
     * @param useSystem true to use system settings, false to use app settings
     */
    fun setUseSystemScale(useSystem: Boolean)

    /** Returns whether to use system settings for font scale or not.
     * @return useSystem true to use system settings, false to use app settings
     */
    fun getUseSystemScale(): Boolean

    /** Returns font scale taking in account [useSystemScale] setting.
     * @return App setting for font scale if [getUseSystemScale] returns false, system setting otherwise
     */
    fun getResolvedFontScaleValue(): FontScaleValue

    /** Returns persisted app font scale.
     * @return app setting for font scale
     */
    fun getAppFontScaleValue(): FontScaleValue

    /** Sets and stores app font scale setting value.
     * @param fontScaleValue value to be set and saved
     */
    fun setFontScaleValue(fontScaleValue: FontScaleValue)

    /** Returns list of all available font scale values.
     * @return list of values
     */
    fun getAvailableScales(): List<FontScaleValue>
}

/**
 * Object to manage the Font Scale choice of the user.
 */
class FontScalePreferencesImpl @Inject constructor(
        @DefaultPreferences private val preferences: SharedPreferences,
        private val systemSettingsProvider: SystemSettingsProvider,
) : FontScalePreferences {

    companion object {
        private const val APPLICATION_FONT_SCALE_KEY = "APPLICATION_FONT_SCALE_KEY"
        private const val APPLICATION_USE_SYSTEM_FONT_SCALE_KEY = "APPLICATION_USE_SYSTEM_FONT_SCALE_KEY"
    }

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

    override fun getAppFontScaleValue(): FontScaleValue {
        return if (APPLICATION_FONT_SCALE_KEY !in preferences) {
            normalFontScaleValue
        } else {
            val pref = preferences.getString(APPLICATION_FONT_SCALE_KEY, null)
            fontScaleValues.firstOrNull { it.preferenceValue == pref } ?: normalFontScaleValue
        }
    }

    override fun getResolvedFontScaleValue(): FontScaleValue {
        val useSystem = getUseSystemScale()

        return if (useSystem) {
            val fontScale = systemSettingsProvider.getSystemFontScale()
            fontScaleValues.firstOrNull { it.scale == fontScale } ?: normalFontScaleValue
        } else {
            getAppFontScaleValue()
        }
    }

    override fun setFontScaleValue(fontScaleValue: FontScaleValue) {
        preferences
                .edit()
                .putString(APPLICATION_FONT_SCALE_KEY, fontScaleValue.preferenceValue)
                .apply()
    }

    override fun getAvailableScales(): List<FontScaleValue> = fontScaleValues

    override fun getUseSystemScale(): Boolean {
        return preferences.getBoolean(APPLICATION_USE_SYSTEM_FONT_SCALE_KEY, true)
    }

    override fun setUseSystemScale(useSystem: Boolean) {
        preferences
                .edit { putBoolean(APPLICATION_USE_SYSTEM_FONT_SCALE_KEY, useSystem) }
    }
}

data class FontScaleValue(
        val index: Int,
        // Possible values for the SharedPrefs
        val preferenceValue: String,
        val scale: Float,
        @StringRes
        val nameResId: Int
)
