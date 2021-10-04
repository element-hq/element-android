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

package im.vector.app.features.themes

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.drawable.DrawableCompat
import im.vector.app.R
import im.vector.app.core.di.DefaultSharedPreferences
import timber.log.Timber
import java.util.concurrent.atomic.AtomicReference

/**
 * Util class for managing themes.
 */
object ThemeUtils {
    // preference key
    const val APPLICATION_THEME_KEY = "APPLICATION_THEME_KEY"

    // the theme possible values
    private const val SYSTEM_THEME_VALUE = "system"
    private const val THEME_DARK_VALUE = "dark"
    private const val THEME_LIGHT_VALUE = "light"
    private const val THEME_BLACK_VALUE = "black"

    // The default theme
    private const val DEFAULT_THEME = SYSTEM_THEME_VALUE

    private var currentTheme = AtomicReference<String>(null)

    private val mColorByAttr = HashMap<Int, Int>()

    // init the theme
    fun init(context: Context) {
        val theme = getApplicationTheme(context)
        setApplicationTheme(context, theme)
    }

    /**
     * @return true if current theme is System
     */
    fun isSystemTheme(context: Context): Boolean {
        val theme = getApplicationTheme(context)
        return theme == SYSTEM_THEME_VALUE
    }

    /**
     * @return true if current theme is Light or current theme is System and system theme is light
     */
    fun isLightTheme(context: Context): Boolean {
        val theme = getApplicationTheme(context)
        return theme == THEME_LIGHT_VALUE
                || (theme == SYSTEM_THEME_VALUE && !isSystemDarkTheme(context.resources))
    }

    /**
     * Provides the selected application theme
     *
     * @param context the context
     * @return the selected application theme
     */
    fun getApplicationTheme(context: Context): String {
        val currentTheme = this.currentTheme.get()
        return if (currentTheme == null) {
            val prefs = DefaultSharedPreferences.getInstance(context)
            var themeFromPref = prefs.getString(APPLICATION_THEME_KEY, DEFAULT_THEME) ?: DEFAULT_THEME
            if (themeFromPref == "status") {
                // Migrate to the default theme
                themeFromPref = DEFAULT_THEME
                prefs.edit { putString(APPLICATION_THEME_KEY, DEFAULT_THEME) }
            }
            this.currentTheme.set(themeFromPref)
            themeFromPref
        } else {
            currentTheme
        }
    }

    /**
     * @return true if system theme is dark
     */
    private fun isSystemDarkTheme(resources: Resources): Boolean {
        return resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    /**
     * Update the application theme
     *
     * @param aTheme the new theme
     */
    fun setApplicationTheme(context: Context, aTheme: String) {
        currentTheme.set(aTheme)
        context.setTheme(
                when (aTheme) {
                    SYSTEM_THEME_VALUE -> if (isSystemDarkTheme(context.resources)) R.style.Theme_Vector_Dark else R.style.Theme_Vector_Light
                    THEME_DARK_VALUE   -> R.style.Theme_Vector_Dark
                    THEME_BLACK_VALUE  -> R.style.Theme_Vector_Black
                    else               -> R.style.Theme_Vector_Light
                }
        )

        // Clear the cache
        mColorByAttr.clear()
    }

    /**
     * Set the activity theme according to the selected one.
     *
     * @param activity the activity
     */
    fun setActivityTheme(activity: Activity, otherThemes: ActivityOtherThemes) {
        when (getApplicationTheme(activity)) {
            SYSTEM_THEME_VALUE -> if (isSystemDarkTheme(activity.resources)) activity.setTheme(otherThemes.dark)
            THEME_DARK_VALUE   -> activity.setTheme(otherThemes.dark)
            THEME_BLACK_VALUE  -> activity.setTheme(otherThemes.black)
        }

        mColorByAttr.clear()
    }

    /**
     * Translates color attributes to colors
     *
     * @param c              Context
     * @param colorAttribute Color Attribute
     * @return Requested Color
     */
    @ColorInt
    fun getColor(c: Context, @AttrRes colorAttribute: Int): Int {
        return mColorByAttr.getOrPut(colorAttribute) {
            try {
                val color = TypedValue()
                c.theme.resolveAttribute(colorAttribute, color, true)
                color.data
            } catch (e: Exception) {
                Timber.e(e, "Unable to get color")
                ContextCompat.getColor(c, android.R.color.holo_red_dark)
            }
        }
    }

    fun getAttribute(c: Context, @AttrRes attribute: Int): TypedValue? {
        try {
            val typedValue = TypedValue()
            c.theme.resolveAttribute(attribute, typedValue, true)
            return typedValue
        } catch (e: Exception) {
            Timber.e(e, "Unable to get color")
        }
        return null
    }

    /**
     * Tint the drawable with a theme attribute
     *
     * @param context   the context
     * @param drawable  the drawable to tint
     * @param attribute the theme color
     * @return the tinted drawable
     */
    fun tintDrawable(context: Context, drawable: Drawable, @AttrRes attribute: Int): Drawable {
        return tintDrawableWithColor(drawable, getColor(context, attribute))
    }

    /**
     * Tint the drawable with a color integer
     *
     * @param drawable the drawable to tint
     * @param color    the color
     * @return the tinted drawable
     */
    fun tintDrawableWithColor(drawable: Drawable, @ColorInt color: Int): Drawable {
        val tinted = DrawableCompat.wrap(drawable)
        drawable.mutate()
        DrawableCompat.setTint(tinted, color)
        return tinted
    }
}
