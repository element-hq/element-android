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
import android.os.Build
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
    const val APPLICATION_DARK_THEME_KEY = "APPLICATION_DARK_THEME_KEY"
    const val SYSTEM_DARK_THEME_PRE_TEN = "SYSTEM_DARK_THEME_PRE_TEN"

    // the theme possible values
    //private const val SYSTEM_THEME_VALUE = "system" // SC does not use this
    private const val THEME_DARK_VALUE = "dark"
    private const val THEME_LIGHT_VALUE = "light"
    private const val THEME_BLACK_VALUE = "black"
    private const val THEME_SC_LIGHT_VALUE = "sc_light"
    private const val THEME_SC_VALUE = "sc"
    private const val THEME_SC_DARK_VALUE = "sc_dark"
    private const val THEME_SC_COLORED_VALUE = "sc_colored"
    private const val THEME_SC_DARK_COLORED_VALUE = "sc_dark_colored"


    private var currentLightTheme = AtomicReference<String>(null)
    private var currentDarkTheme = AtomicReference<String>(null)

    // The default theme // SC: from upstream, ignore
    //private const val DEFAULT_THEME = SYSTEM_THEME_VALUE

    private var currentTheme = AtomicReference<String>(null)

    private val mColorByAttr = HashMap<Int, Int>()

    private var mIsScTheme = false
    private var mUseDarkTheme = false
    private var mThemeInitialized = false

    /**
     * @return Whether a system-wide dark mode is available on this device
     */
    fun darkThemePossible(context: Context): Boolean {
        // On Lineage, available since 15.1: https://review.lineageos.org/c/LineageOS/android_frameworks_base/+/209022
        return darkThemeDefinitivelyPossible() || DefaultSharedPreferences.getInstance(context).getBoolean(SYSTEM_DARK_THEME_PRE_TEN, false)
    }

    fun darkThemeDefinitivelyPossible(): Boolean {
        // Available since Android 10: https://developer.android.com/guide/topics/ui/look-and-feel/darktheme
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }

    fun shouldUseDarkTheme(context: Context): Boolean {
        if (!darkThemePossible(context)) {
            return false
        }
        val currentNightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES
    }

    fun useDarkTheme(context: Context): Boolean {
        if (!mThemeInitialized) {
            mThemeInitialized = true
            mUseDarkTheme = shouldUseDarkTheme(context)
        }
        return mUseDarkTheme
    }

    fun invalidateNightMode(context: Context) {
        val lightTheme = getApplicationLightTheme(context)
        val darkTheme = getApplicationDarkTheme(context)
        if (lightTheme != darkTheme && darkThemePossible(context)) {
            mThemeInitialized = false;
            setApplicationTheme(context.applicationContext, getApplicationLightTheme(context), getApplicationDarkTheme(context))
        }
    }

    // init the theme
    fun init(context: Context) {
        val lightTheme = getApplicationLightTheme(context)
        val darkTheme = getApplicationDarkTheme(context)
        setApplicationTheme(context, lightTheme, darkTheme)
    }

    /**
     * @return true if current theme is System
     */
    fun isSystemTheme(@Suppress("UNUSED_PARAMETER") context: Context): Boolean {
        // SC does not use Element's system theme
        return false
        /*
        val theme = getApplicationTheme(context)
        return theme == SYSTEM_THEME_VALUE
         */
    }

    /**
     * @return true if current theme is Light or current theme is System and system theme is light
     */
    fun isLightTheme(context: Context): Boolean {
        return when (getApplicationTheme(context)) {
            THEME_LIGHT_VALUE,
            THEME_SC_LIGHT_VALUE -> true
            else               -> false
        }
    }

    /**
     * @return true if current theme is black (darker than dark)
     */
    fun isBlackTheme(context: Context): Boolean {
        return when (getApplicationTheme(context)) {
            THEME_BLACK_VALUE,
            THEME_SC_VALUE,
            THEME_SC_COLORED_VALUE -> true
            else                   -> false
        }
    }

    /**
     * Provides the selected application theme
     *
     * @param context the context
     * @return the selected application theme
     */
    fun getApplicationTheme(context: Context): String {
        return if (useDarkTheme(context)) getApplicationDarkTheme(context) else getApplicationLightTheme(context)
    }


    /**
     * Provides the selected application theme for light system design
     *
     * @param context the context
     * @return the selected application theme for light system design
     */
    fun getApplicationLightTheme(context: Context): String {
        val currentTheme = this.currentLightTheme.get()
        return if (currentTheme == null) {
            val prefs = DefaultSharedPreferences.getInstance(context)
            var themeFromPref = prefs.getString(APPLICATION_THEME_KEY, THEME_SC_LIGHT_VALUE) ?: THEME_SC_LIGHT_VALUE
            if (themeFromPref == "status") {
                // Migrate to the default theme
                themeFromPref = THEME_LIGHT_VALUE
                prefs.edit { putString(APPLICATION_THEME_KEY, THEME_LIGHT_VALUE) }
            }
            this.currentLightTheme.set(themeFromPref)
            themeFromPref
        } else {
            currentTheme
        }
    }


    /**
     * Provides the selected application theme for night system design
     *
     * @param context the context
     * @return the selected application theme for night system design
     */
    fun getApplicationDarkTheme(context: Context): String {
        val currentTheme = this.currentDarkTheme.get()
        return if (currentTheme == null) {
            val prefs = DefaultSharedPreferences.getInstance(context)
            var themeFromPref = prefs.getString(APPLICATION_DARK_THEME_KEY, THEME_SC_DARK_VALUE) ?: THEME_SC_DARK_VALUE
            if (themeFromPref == "status") {
                // Migrate to light theme, which is the closest theme
                themeFromPref = THEME_LIGHT_VALUE
                prefs.edit { putString(APPLICATION_DARK_THEME_KEY, THEME_LIGHT_VALUE) }
            }
            this.currentDarkTheme.set(themeFromPref)
            themeFromPref
        } else {
            currentTheme
        }
    }

    /**
     * @return true if system theme is dark
     */
    /* SC: this is from upstream, we do it differently
    private fun isSystemDarkTheme(resources: Resources): Boolean {
        return resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }
    */

    /**
     * Update the application theme
     *
     * @param aTheme the new theme
     */
    fun setApplicationTheme(context: Context, aLightTheme: String, aDarkTheme: String) {
        currentLightTheme.set(aLightTheme)
        currentDarkTheme.set(aDarkTheme)
        val aTheme = if (useDarkTheme(context)) aDarkTheme else aLightTheme
        context.setTheme(
                when (aTheme) {
                    //SYSTEM_THEME_VALUE -> if (isSystemDarkTheme(context.resources)) R.style.Theme_Vector_Dark else R.style.Theme_Vector_Light
                    THEME_LIGHT_VALUE  -> R.style.Theme_Vector_Light
                    THEME_DARK_VALUE   -> R.style.Theme_Vector_Dark
                    THEME_BLACK_VALUE  -> R.style.Theme_Vector_Black
                    THEME_SC_LIGHT_VALUE -> R.style.AppTheme_SC_Light
                    THEME_SC_VALUE     -> R.style.AppTheme_SC
                    THEME_SC_DARK_VALUE -> R.style.AppTheme_SC_Dark
                    THEME_SC_COLORED_VALUE -> R.style.AppTheme_SC_Colored
                    THEME_SC_DARK_COLORED_VALUE -> R.style.AppTheme_SC_Dark_Colored
                    else               -> R.style.AppTheme_SC_Light
                }
        )

        // Clear the cache
        mColorByAttr.clear()
    }

    fun setApplicationLightTheme(context: Context, theme: String) {
        setApplicationTheme(context, theme, getApplicationDarkTheme(context))
    }

    fun setApplicationDarkTheme(context: Context, theme: String) {
        setApplicationTheme(context, getApplicationLightTheme(context), theme)
    }

    /**
     * Set the activity theme according to the selected one.
     *
     * @param activity the activity
     */
    fun setActivityTheme(activity: Activity, otherThemes: ActivityOtherThemes) {
        when (getApplicationTheme(activity)) {
            //SYSTEM_THEME_VALUE -> if (isSystemDarkTheme(activity.resources)) activity.setTheme(otherThemes.dark)
            THEME_LIGHT_VALUE  -> activity.setTheme(otherThemes.light)
            THEME_DARK_VALUE   -> activity.setTheme(otherThemes.dark)
            THEME_BLACK_VALUE  -> activity.setTheme(otherThemes.black)
            THEME_SC_LIGHT_VALUE     -> activity.setTheme(otherThemes.sc_light)
            THEME_SC_VALUE     -> activity.setTheme(otherThemes.sc)
            THEME_SC_DARK_VALUE     -> activity.setTheme(otherThemes.sc_dark)
            THEME_SC_COLORED_VALUE     -> activity.setTheme(otherThemes.sc_colored)
            THEME_SC_DARK_COLORED_VALUE     -> activity.setTheme(otherThemes.sc_dark_colored)
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
                when (colorAttribute) {
                    android.R.attr.colorAccent           -> ContextCompat.getColor(c, R.color.accent_sc)
                    R.attr.colorAccent                   -> ContextCompat.getColor(c, R.color.accent_sc)
                    else                                 -> {
                        Timber.e(e, "Unable to get color")
                        ContextCompat.getColor(c, android.R.color.holo_red_dark)
                    }
                }
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
