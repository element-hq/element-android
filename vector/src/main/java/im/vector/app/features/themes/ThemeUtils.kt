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
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.StyleRes
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
    const val SETTINGS_SC_ACCENT_LIGHT = "SETTINGS_SC_ACCENT_LIGHT"
    const val SETTINGS_SC_ACCENT_DARK = "SETTINGS_SC_ACCENT_DARK"

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
    private var currentLightThemeAccent = AtomicReference<String>(null)
    private var currentDarkThemeAccent = AtomicReference<String>(null)

    // The default theme // SC: from upstream, ignore
    //private const val DEFAULT_THEME = SYSTEM_THEME_VALUE

    //private var currentTheme = AtomicReference<String>(null)

    private val mColorByAttr = HashMap<Int, Int>()

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
            mThemeInitialized = false
            setApplicationTheme(context.applicationContext, getApplicationLightTheme(context), getApplicationDarkTheme(context),
                    getApplicationLightThemeAccent(context), getApplicationDarkThemeAccent(context))
        }
    }

    // init the theme
    fun init(context: Context) {
        val lightTheme = getApplicationLightTheme(context)
        val darkTheme = getApplicationDarkTheme(context)
        val lightAccent = getApplicationLightThemeAccent(context)
        val darkAccent = getApplicationDarkThemeAccent(context)
        setApplicationTheme(context, lightTheme, darkTheme, lightAccent, darkAccent)
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

    fun getApplicationLightThemeAccent(context: Context): String {
        val currentAccent = this.currentLightThemeAccent.get()
        return if (currentAccent == null) {
            val accentFromPref = DefaultSharedPreferences.getInstance(context).getString(SETTINGS_SC_ACCENT_LIGHT, "green") ?: "green"
            this.currentLightThemeAccent.set(accentFromPref)
            accentFromPref
        } else {
            currentAccent
        }
    }

    fun getApplicationDarkThemeAccent(context: Context): String {
        val currentAccent = this.currentDarkThemeAccent.get()
        return if (currentAccent == null) {
            val accentFromPref = DefaultSharedPreferences.getInstance(context).getString(SETTINGS_SC_ACCENT_DARK, "green") ?: "green"
            this.currentDarkThemeAccent.set(accentFromPref)
            accentFromPref
        } else {
            currentAccent
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
    fun setApplicationTheme(context: Context, aLightTheme: String, aDarkTheme: String, aLightAccent: String, aDarkAccent: String) {
        currentLightTheme.set(aLightTheme)
        currentDarkTheme.set(aDarkTheme)
        currentLightThemeAccent.set(aLightAccent)
        currentDarkThemeAccent.set(aDarkAccent)
        val aTheme = if (useDarkTheme(context)) aDarkTheme else aLightTheme
        context.setTheme(
                when (aTheme) {
                    //SYSTEM_THEME_VALUE -> if (isSystemDarkTheme(context.resources)) R.style.Theme_Vector_Dark else R.style.Theme_Vector_Light
                    THEME_LIGHT_VALUE  -> R.style.Theme_Vector_Light
                    THEME_DARK_VALUE   -> R.style.Theme_Vector_Dark
                    THEME_BLACK_VALUE  -> R.style.Theme_Vector_Black
                    THEME_SC_LIGHT_VALUE -> getAccentedThemeRes(R.style.AppTheme_SC_Light, aLightAccent)
                    THEME_SC_VALUE     -> getAccentedThemeRes(R.style.AppTheme_SC, aDarkAccent)
                    THEME_SC_DARK_VALUE -> getAccentedThemeRes(R.style.AppTheme_SC_Dark, aDarkAccent)
                    THEME_SC_COLORED_VALUE -> getAccentedThemeRes(R.style.AppTheme_SC_Colored, aDarkAccent)
                    THEME_SC_DARK_COLORED_VALUE -> getAccentedThemeRes(R.style.AppTheme_SC_Dark_Colored, aDarkAccent)
                    else               -> getAccentedThemeRes(R.style.AppTheme_SC_Light, aLightAccent)
                }
        )

        // Clear the cache
        mColorByAttr.clear()
    }

    fun setApplicationLightTheme(context: Context, theme: String) {
        setApplicationTheme(context, theme, getApplicationDarkTheme(context),
                getApplicationLightThemeAccent(context), getApplicationDarkThemeAccent(context))
    }

    fun setApplicationDarkTheme(context: Context, theme: String) {
        setApplicationTheme(context, getApplicationLightTheme(context), theme,
                getApplicationLightThemeAccent(context), getApplicationDarkThemeAccent(context))
    }

    fun setApplicationLightThemeAccent(context: Context, themeAccent: String) {
        setApplicationTheme(context, getApplicationLightTheme(context), getApplicationDarkTheme(context),
                themeAccent, getApplicationDarkThemeAccent(context))
    }

    fun setApplicationDarkThemeAccent(context: Context, themeAccent: String) {
        setApplicationTheme(context, getApplicationLightTheme(context), getApplicationDarkTheme(context),
                getApplicationLightThemeAccent(context), themeAccent)
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
            THEME_SC_LIGHT_VALUE     -> activity.setTheme(getAccentedThemeRes(otherThemes.sc_light, getApplicationLightThemeAccent(activity)))
            THEME_SC_VALUE     -> activity.setTheme(getAccentedThemeRes(otherThemes.sc, getApplicationDarkThemeAccent(activity)))
            THEME_SC_DARK_VALUE     -> activity.setTheme(getAccentedThemeRes(otherThemes.sc_dark, getApplicationDarkThemeAccent(activity)))
            THEME_SC_COLORED_VALUE     -> activity.setTheme(getAccentedThemeRes(otherThemes.sc_colored, getApplicationDarkThemeAccent(activity)))
            THEME_SC_DARK_COLORED_VALUE     -> activity.setTheme(getAccentedThemeRes(otherThemes.sc_dark_colored, getApplicationDarkThemeAccent(activity)))
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

    @StyleRes
    private fun getAccentedThemeRes(@StyleRes resId: Int, themeAccent: String): Int {
        return when (resId) {
            R.style.AppTheme_SC_Light -> {
                when (themeAccent) {
                    "green" -> resId
                    "bluelight" -> R.style.AppTheme_SC_Light_BlueLight
                    "amber" -> R.style.AppTheme_SC_Light_Amber
                    "cyan" -> R.style.AppTheme_SC_Light_Cyan
                    "gold" -> R.style.AppTheme_SC_Light_Gold
                    "lime" -> R.style.AppTheme_SC_Light_Lime
                    "orange" -> R.style.AppTheme_SC_Light_Orange
                    "pink" -> R.style.AppTheme_SC_Light_Pink
                    "purple" -> R.style.AppTheme_SC_Light_Purple
                    "red" -> R.style.AppTheme_SC_Light_Red
                    "teal" -> R.style.AppTheme_SC_Light_Teal
                    "turquoise" -> R.style.AppTheme_SC_Light_Turquoise
                    "yellow" -> R.style.AppTheme_SC_Light_Yellow
                    "carnation" -> R.style.AppTheme_SC_Light_Carnation
                    "denim" -> R.style.AppTheme_SC_Light_Denim
                    "indigo" -> R.style.AppTheme_SC_Light_Indigo
                    "lava" -> R.style.AppTheme_SC_Light_Lava
                    "blue" -> R.style.AppTheme_SC_Light_Blue
                    "greendark" -> R.style.AppTheme_SC_Light_GreenDark
                    // Do not change this comment for automatic light theme insertion
                    else -> resId
                }
            }
            R.style.AppTheme_SC -> {
                when (themeAccent) {
                    "green" -> resId
                    "bluelight" -> R.style.AppTheme_SC_BlueLight
                    "amber" -> R.style.AppTheme_SC_Amber
                    "cyan" -> R.style.AppTheme_SC_Cyan
                    "gold" -> R.style.AppTheme_SC_Gold
                    "lime" -> R.style.AppTheme_SC_Lime
                    "orange" -> R.style.AppTheme_SC_Orange
                    "pink" -> R.style.AppTheme_SC_Pink
                    "purple" -> R.style.AppTheme_SC_Purple
                    "red" -> R.style.AppTheme_SC_Red
                    "teal" -> R.style.AppTheme_SC_Teal
                    "turquoise" -> R.style.AppTheme_SC_Turquoise
                    "yellow" -> R.style.AppTheme_SC_Yellow
                    "carnation" -> R.style.AppTheme_SC_Carnation
                    "denim" -> R.style.AppTheme_SC_Denim
                    "indigo" -> R.style.AppTheme_SC_Indigo
                    "lava" -> R.style.AppTheme_SC_Lava
                    "blue" -> R.style.AppTheme_SC_Blue
                    "greendark" -> R.style.AppTheme_SC_GreenDark
                    // Do not change this comment for automatic black theme insertion
                    else -> resId
                }
            }
            R.style.AppTheme_SC_Dark -> {
                when (themeAccent) {
                    "green" -> resId
                    "bluelight" -> R.style.AppTheme_SC_Dark_BlueLight
                    "amber" -> R.style.AppTheme_SC_Dark_Amber
                    "cyan" -> R.style.AppTheme_SC_Dark_Cyan
                    "gold" -> R.style.AppTheme_SC_Dark_Gold
                    "lime" -> R.style.AppTheme_SC_Dark_Lime
                    "orange" -> R.style.AppTheme_SC_Dark_Orange
                    "pink" -> R.style.AppTheme_SC_Dark_Pink
                    "purple" -> R.style.AppTheme_SC_Dark_Purple
                    "red" -> R.style.AppTheme_SC_Dark_Red
                    "teal" -> R.style.AppTheme_SC_Dark_Teal
                    "turquoise" -> R.style.AppTheme_SC_Dark_Turquoise
                    "yellow" -> R.style.AppTheme_SC_Dark_Yellow
                    "carnation" -> R.style.AppTheme_SC_Dark_Carnation
                    "denim" -> R.style.AppTheme_SC_Dark_Denim
                    "indigo" -> R.style.AppTheme_SC_Dark_Indigo
                    "lava" -> R.style.AppTheme_SC_Dark_Lava
                    "blue" -> R.style.AppTheme_SC_Dark_Blue
                    "greendark" -> R.style.AppTheme_SC_Dark_GreenDark
                    // Do not change this comment for automatic dark theme insertion
                    else -> resId
                }
            }
            R.style.AppTheme_SC_Colored -> {
                when (themeAccent) {
                    "green" -> resId
                    "bluelight" -> R.style.AppTheme_SC_Colored_BlueLight
                    "amber" -> R.style.AppTheme_SC_Colored_Amber
                    "cyan" -> R.style.AppTheme_SC_Colored_Cyan
                    "gold" -> R.style.AppTheme_SC_Colored_Gold
                    "lime" -> R.style.AppTheme_SC_Colored_Lime
                    "orange" -> R.style.AppTheme_SC_Colored_Orange
                    "pink" -> R.style.AppTheme_SC_Colored_Pink
                    "purple" -> R.style.AppTheme_SC_Colored_Purple
                    "red" -> R.style.AppTheme_SC_Colored_Red
                    "teal" -> R.style.AppTheme_SC_Colored_Teal
                    "turquoise" -> R.style.AppTheme_SC_Colored_Turquoise
                    "yellow" -> R.style.AppTheme_SC_Colored_Yellow
                    "carnation" -> R.style.AppTheme_SC_Colored_Carnation
                    "denim" -> R.style.AppTheme_SC_Colored_Denim
                    "indigo" -> R.style.AppTheme_SC_Colored_Indigo
                    "lava" -> R.style.AppTheme_SC_Colored_Lava
                    "blue" -> R.style.AppTheme_SC_Colored_Blue
                    "greendark" -> R.style.AppTheme_SC_Colored_GreenDark
                    // Do not change this comment for automatic black colored theme insertion
                    else -> resId
                }
            }
            R.style.AppTheme_SC_Dark_Colored -> {
                when (themeAccent) {
                    "green" -> resId
                    "bluelight" -> R.style.AppTheme_SC_Dark_Colored_BlueLight
                    "amber" -> R.style.AppTheme_SC_Dark_Colored_Amber
                    "cyan" -> R.style.AppTheme_SC_Dark_Colored_Cyan
                    "gold" -> R.style.AppTheme_SC_Dark_Colored_Gold
                    "lime" -> R.style.AppTheme_SC_Dark_Colored_Lime
                    "orange" -> R.style.AppTheme_SC_Dark_Colored_Orange
                    "pink" -> R.style.AppTheme_SC_Dark_Colored_Pink
                    "purple" -> R.style.AppTheme_SC_Dark_Colored_Purple
                    "red" -> R.style.AppTheme_SC_Dark_Colored_Red
                    "teal" -> R.style.AppTheme_SC_Dark_Colored_Teal
                    "turquoise" -> R.style.AppTheme_SC_Dark_Colored_Turquoise
                    "yellow" -> R.style.AppTheme_SC_Dark_Colored_Yellow
                    "carnation" -> R.style.AppTheme_SC_Dark_Colored_Carnation
                    "denim" -> R.style.AppTheme_SC_Dark_Colored_Denim
                    "indigo" -> R.style.AppTheme_SC_Dark_Colored_Indigo
                    "lava" -> R.style.AppTheme_SC_Dark_Colored_Lava
                    "blue" -> R.style.AppTheme_SC_Dark_Colored_Blue
                    "greendark" -> R.style.AppTheme_SC_Dark_Colored_GreenDark
                    // Do not change this comment for automatic dark colored theme insertion
                    else -> resId
                }
            }
            else -> resId
        }
    }

}
