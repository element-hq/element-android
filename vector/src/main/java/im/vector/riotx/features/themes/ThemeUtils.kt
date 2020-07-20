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

package im.vector.riotx.features.themes

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.Menu
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.preference.PreferenceManager
import im.vector.riotx.R
import timber.log.Timber

/**
 * Util class for managing themes.
 */
object ThemeUtils {
    // preference key
    const val APPLICATION_THEME_KEY = "APPLICATION_THEME_KEY"
    const val APPLICATION_DARK_THEME_KEY = "APPLICATION_DARK_THEME_KEY"

    // the theme possible values
    private const val THEME_DARK_VALUE = "dark"
    private const val THEME_LIGHT_VALUE = "light"
    private const val THEME_BLACK_VALUE = "black"
    private const val THEME_STATUS_VALUE = "status"
    private const val THEME_SC_LIGHT_VALUE = "sc_light"
    private const val THEME_SC_VALUE = "sc"
    private const val THEME_SC_DARK_VALUE = "sc_dark"
    private const val THEME_SC_COLORED_VALUE = "sc_colored"
    private const val THEME_SC_DARK_COLORED_VALUE = "sc_dark_colored"

    private val mColorByAttr = HashMap<Int, Int>()

    private var mIsScTheme = false
    private var mUseDarkTheme = false
    private var mThemeInitialized = false

    fun shouldUseDarkTheme(context: Context): Boolean {
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
        mThemeInitialized = false;
        setApplicationTheme(context.applicationContext, getApplicationLightTheme(context), getApplicationDarkTheme(context))
    }

    /**
     * @return true if current theme is Light or Status
     */
    fun isLightTheme(context: Context): Boolean {
        return when (getApplicationTheme(context)) {
            THEME_LIGHT_VALUE,
            THEME_SC_LIGHT_VALUE,
            THEME_STATUS_VALUE -> true
            else               -> false
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
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(APPLICATION_THEME_KEY, THEME_SC_LIGHT_VALUE) ?: THEME_SC_LIGHT_VALUE
    }


    /**
     * Provides the selected application theme for night system design
     *
     * @param context the context
     * @return the selected application theme for night system design
     */
    fun getApplicationDarkTheme(context: Context): String {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(APPLICATION_DARK_THEME_KEY, THEME_SC_DARK_VALUE) ?: THEME_SC_DARK_VALUE
    }

    /**
     * Whether this is SC theme.
     *
     * @param context the context
     * @return true if SC theme is active, false otherwise
     */
    fun isScTheme(context: Context?): Boolean {
        if (context != null) {
            mIsScTheme = when (getApplicationTheme(context)) {
                THEME_SC_LIGHT_VALUE,
                THEME_SC_VALUE,
                THEME_SC_DARK_VALUE,
                THEME_SC_COLORED_VALUE,
                THEME_SC_DARK_COLORED_VALUE -> true
                else -> false
            }
        }
        return mIsScTheme;
    }

    /**
     * Update the application theme
     *
     * @param aTheme the new theme
     */
    fun setApplicationTheme(context: Context, aLightTheme: String, aDarkTheme: String) {
        val aTheme = if (useDarkTheme(context)) aDarkTheme else aLightTheme
        when (aTheme) {
            THEME_LIGHT_VALUE  -> context.setTheme(R.style.AppTheme_Light)
            THEME_DARK_VALUE   -> context.setTheme(R.style.AppTheme_Dark)
            THEME_BLACK_VALUE  -> context.setTheme(R.style.AppTheme_Black)
            THEME_STATUS_VALUE -> context.setTheme(R.style.AppTheme_Status)
            THEME_SC_LIGHT_VALUE -> context.setTheme(R.style.AppTheme_SC_Light)
            THEME_SC_VALUE     -> context.setTheme(R.style.AppTheme_SC)
            THEME_SC_DARK_VALUE     -> context.setTheme(R.style.AppTheme_SC_Dark)
            THEME_SC_COLORED_VALUE     -> context.setTheme(R.style.AppTheme_SC_Colored)
            THEME_SC_DARK_COLORED_VALUE     -> context.setTheme(R.style.AppTheme_SC_Dark_Colored)
            else               -> context.setTheme(R.style.AppTheme_Light)
        }

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
            THEME_LIGHT_VALUE  -> activity.setTheme(otherThemes.light)
            THEME_DARK_VALUE   -> activity.setTheme(otherThemes.dark)
            THEME_BLACK_VALUE  -> activity.setTheme(otherThemes.black)
            THEME_STATUS_VALUE -> activity.setTheme(otherThemes.status)
            THEME_SC_LIGHT_VALUE     -> activity.setTheme(otherThemes.sc_light)
            THEME_SC_VALUE     -> activity.setTheme(otherThemes.sc)
            THEME_SC_DARK_VALUE     -> activity.setTheme(otherThemes.sc_dark)
            THEME_SC_COLORED_VALUE     -> activity.setTheme(otherThemes.sc_colored)
            THEME_SC_DARK_COLORED_VALUE     -> activity.setTheme(otherThemes.sc_dark_colored)
        }

        mColorByAttr.clear()
    }

    /**
     * Set the TabLayout colors.
     * It seems that there is no proper way to manage it with the manifest file.
     *
     * @param activity the activity
     * @param layout   the layout
     */
    /*
    fun setTabLayoutTheme(activity: Activity, layout: TabLayout) {
        if (activity is VectorGroupDetailsActivity) {
            val textColor: Int
            val underlineColor: Int
            val backgroundColor: Int

            if (TextUtils.equals(getApplicationTheme(activity), THEME_LIGHT_VALUE)) {
                textColor = ContextCompat.getColor(activity, android.R.color.white)
                underlineColor = textColor
                backgroundColor = ContextCompat.getColor(activity, R.color.tab_groups)
            } else if (TextUtils.equals(getApplicationTheme(activity), THEME_STATUS_VALUE)) {
                textColor = ContextCompat.getColor(activity, android.R.color.white)
                underlineColor = textColor
                backgroundColor = getColor(activity, R.attr.colorPrimary)
            } else {
                textColor = ContextCompat.getColor(activity, R.color.tab_groups)
                underlineColor = textColor
                backgroundColor = getColor(activity, R.attr.colorPrimary)
            }

            layout.setTabTextColors(textColor, textColor)
            layout.setSelectedTabIndicatorColor(underlineColor)
            layout.setBackgroundColor(backgroundColor)
        }
    }    */

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
                    android.R.attr.colorAccent           -> ContextCompat.getColor(c, R.color.riotx_accent)
                    R.attr.colorAccent                   -> ContextCompat.getColor(c, R.color.riotx_accent)
                    R.attr.riotx_positive_accent         -> ContextCompat.getColor(c, R.color.riotx_positive_accent)
                    R.attr.riotx_positive_accent_alpha12 -> ContextCompat.getColor(c, R.color.riotx_positive_accent_alpha12)
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
     * Get the resource Id applied to the current theme
     *
     * @param c          the context
     * @param resourceId the resource id in the light theme
     * @return the resource Id for the current theme
     */
    // TODO Now that we are API 21, this is not necessary anymore
    fun getResourceId(c: Context, resourceId: Int): Int {
        val theme = getApplicationTheme(c)

        return when (theme) {
            THEME_LIGHT_VALUE -> resourceId
            THEME_DARK_VALUE  -> {
                return when (resourceId) {
                    R.drawable.bg_search_edit_text_light     -> R.drawable.bg_search_edit_text_dark
                    R.drawable.bg_unread_notification_light  -> R.drawable.bg_unread_notification_dark
                    R.drawable.vector_label_background_light -> R.drawable.vector_label_background_dark
                    R.drawable.divider_horizontal_light      -> R.drawable.divider_horizontal_dark
                    else                                     -> {
                        Timber.w("Warning, missing case for wanted drawable in dark theme")
                        resourceId
                    }
                }
            }
            THEME_BLACK_VALUE -> {
                return when (resourceId) {
                    R.drawable.bg_search_edit_text_light     -> R.drawable.bg_search_edit_text_black
                    R.drawable.bg_unread_notification_light  -> R.drawable.bg_unread_notification_black
                    R.drawable.vector_label_background_light -> R.drawable.vector_label_background_black
                    R.drawable.divider_horizontal_light      -> R.drawable.divider_horizontal_black
                    else                                     -> {
                        Timber.w("Warning, missing case for wanted drawable in black theme")
                        resourceId
                    }
                }
            }
            THEME_SC_VALUE, THEME_SC_DARK_VALUE, THEME_SC_COLORED_VALUE, THEME_SC_DARK_COLORED_VALUE -> {
                return when (resourceId) {
                    R.drawable.bg_search_edit_text_light     -> R.drawable.bg_search_edit_text_sc
                    R.drawable.bg_unread_notification_light  -> R.drawable.bg_unread_notification_sc
                    R.drawable.vector_label_background_light -> R.drawable.vector_label_background_sc
                    R.drawable.divider_horizontal_light      -> R.drawable.divider_horizontal_sc
                    else                                     -> {
                        Timber.w("Warning, missing case for wanted drawable in sc theme")
                        resourceId
                    }
                }
            }
            else              -> {
                Timber.w("Warning, missing theme: $theme")
                resourceId
            }
        }
    }

    /**
     * Update the menu icons colors
     *
     * @param menu  the menu
     * @param color the color
     */
    fun tintMenuIcons(menu: Menu, color: Int) {
        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            val drawable = item.icon
            if (drawable != null) {
                val wrapped = DrawableCompat.wrap(drawable)
                drawable.mutate()
                DrawableCompat.setTint(wrapped, color)
                item.icon = drawable
            }
        }
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
