/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.riotx.features.configuration

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import im.vector.riotx.features.settings.FontScale
import im.vector.riotx.features.settings.VectorLocale
import im.vector.riotx.features.themes.ThemeUtils
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

/**
 * Handle locale configuration change, such as theme, font size and locale chosen by the user
 */
class VectorConfiguration @Inject constructor(private val context: Context) {

    // TODO Import mLanguageReceiver From Riot?
    fun onConfigurationChanged() {
        if (Locale.getDefault().toString() != VectorLocale.applicationLocale.toString()) {
            Timber.v("## onConfigurationChanged(): the locale has been updated to ${Locale.getDefault()}")
            Timber.v("## onConfigurationChanged(): restore the expected value ${VectorLocale.applicationLocale}")
            updateApplicationSettings(VectorLocale.applicationLocale,
                    FontScale.getFontScaleValue(context),
                    ThemeUtils.getApplicationTheme(context))
        }
    }

    private fun updateApplicationSettings(locale: Locale, fontScaleValue: FontScale.FontScaleValue, theme: String) {
        VectorLocale.saveApplicationLocale(context, locale)
        FontScale.saveFontScaleValue(context, fontScaleValue)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        @Suppress("DEPRECATION")
        config.locale = locale
        config.fontScale = FontScale.getFontScaleValue(context).scale
        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(config, context.resources.displayMetrics)

        ThemeUtils.setApplicationTheme(context, theme)
        // TODO PhoneNumberUtils.onLocaleUpdate()
    }

    /**
     * Update the application theme
     *
     * @param theme the new theme
     */
    fun updateApplicationTheme(theme: String) {
        ThemeUtils.setApplicationTheme(context, theme)
        updateApplicationSettings(VectorLocale.applicationLocale,
                FontScale.getFontScaleValue(context),
                theme)
    }

    /**
     * Init the configuration from the saved one
     */
    fun initConfiguration() {
        VectorLocale.init(context)
        val locale = VectorLocale.applicationLocale
        val fontScale = FontScale.getFontScaleValue(context)
        val theme = ThemeUtils.getApplicationTheme(context)

        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        @Suppress("DEPRECATION")
        config.locale = locale
        config.fontScale = fontScale.scale
        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(config, context.resources.displayMetrics)

        // init the theme
        ThemeUtils.setApplicationTheme(context, theme)
    }

    /**
     * Update the application locale
     *
     * @param locale
     */
    fun updateApplicationLocale(locale: Locale) {
        updateApplicationSettings(locale, FontScale.getFontScaleValue(context), ThemeUtils.getApplicationTheme(context))
    }

    /**
     * Compute a localised context
     *
     * @param context the context
     * @return the localised context
     */
    @SuppressLint("NewApi")
    fun getLocalisedContext(context: Context): Context {
        try {
            val resources = context.resources
            val locale = VectorLocale.applicationLocale
            val configuration = resources.configuration
            configuration.fontScale = FontScale.getFontScaleValue(context).scale

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                configuration.setLocale(locale)
                configuration.setLayoutDirection(locale)
                return context.createConfigurationContext(configuration)
            } else {
                @Suppress("DEPRECATION")
                configuration.locale = locale
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    configuration.setLayoutDirection(locale)
                }
                @Suppress("DEPRECATION")
                resources.updateConfiguration(configuration, resources.displayMetrics)
                return context
            }
        } catch (e: Exception) {
            Timber.e(e, "## getLocalisedContext() failed")
        }

        return context
    }

    /**
     * Compute the locale status value
     * @return the local status value
     */
    // TODO Create data class for this
    fun getHash(): String {
        return (VectorLocale.applicationLocale.toString()
                + "_" + FontScale.getFontScaleValue(context).preferenceValue
                + "_" + ThemeUtils.getApplicationTheme(context))
    }
}
