/*
 * Copyright 2018-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings

import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.core.content.edit
import im.vector.app.core.di.DefaultPreferences
import im.vector.app.core.utils.SystemSettingsProvider
import im.vector.lib.strings.CommonStrings
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

    companion object {
        const val SCALE_TINY = 0.70f
        const val SCALE_SMALL = 0.85f
        const val SCALE_NORMAL = 1.00f
        const val SCALE_LARGE = 1.15f
        const val SCALE_LARGER = 1.30f
        const val SCALE_LARGEST = 1.45f
        const val SCALE_HUGE = 1.60f
    }
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
            FontScaleValue(0, "FONT_SCALE_TINY", FontScalePreferences.SCALE_TINY, CommonStrings.tiny),
            FontScaleValue(1, "FONT_SCALE_SMALL", FontScalePreferences.SCALE_SMALL, CommonStrings.small),
            FontScaleValue(2, "FONT_SCALE_NORMAL", FontScalePreferences.SCALE_NORMAL, CommonStrings.normal),
            FontScaleValue(3, "FONT_SCALE_LARGE", FontScalePreferences.SCALE_LARGE, CommonStrings.large),
            FontScaleValue(4, "FONT_SCALE_LARGER", FontScalePreferences.SCALE_LARGER, CommonStrings.larger),
            FontScaleValue(5, "FONT_SCALE_LARGEST", FontScalePreferences.SCALE_LARGEST, CommonStrings.largest),
            FontScaleValue(6, "FONT_SCALE_HUGE", FontScalePreferences.SCALE_HUGE, CommonStrings.huge)
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
