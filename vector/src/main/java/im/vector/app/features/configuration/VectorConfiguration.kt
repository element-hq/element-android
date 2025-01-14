/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.configuration

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.annotation.RequiresApi
import im.vector.app.features.settings.FontScalePreferences
import im.vector.app.features.settings.VectorLocaleProvider
import im.vector.app.features.themes.ThemeUtils
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

/**
 * Handle locale configuration change, such as theme, font size and locale chosen by the user.
 */
class VectorConfiguration @Inject constructor(
        private val context: Context,
        private val fontScalePreferences: FontScalePreferences,
        private val vectorLocale: VectorLocaleProvider,
) {

    fun onConfigurationChanged() {
        if (Locale.getDefault().toString() != vectorLocale.applicationLocale.toString()) {
            Timber.v("## onConfigurationChanged(): the locale has been updated to ${Locale.getDefault()}")
            Timber.v("## onConfigurationChanged(): restore the expected value ${vectorLocale.applicationLocale}")
            Locale.setDefault(vectorLocale.applicationLocale)
        }
        // Night mode may have changed
        ThemeUtils.init(context)
    }

    fun applyToApplicationContext() {
        val locale = vectorLocale.applicationLocale
        val fontScale = fontScalePreferences.getResolvedFontScaleValue()

        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        @Suppress("DEPRECATION")
        config.locale = locale
        config.fontScale = fontScale.scale
        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    /**
     * Compute a localised context.
     *
     * @param context the context
     * @return the localised context
     */
    fun getLocalisedContext(context: Context): Context {
        try {
            val locale = vectorLocale.applicationLocale

            // create new configuration passing old configuration from original Context
            val configuration = Configuration(context.resources.configuration)

            configuration.fontScale = fontScalePreferences.getResolvedFontScaleValue().scale

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                setLocaleForApi24(configuration, locale)
            } else {
                configuration.setLocale(locale)
            }
            configuration.setLayoutDirection(locale)
            return context.createConfigurationContext(configuration)
        } catch (e: Exception) {
            Timber.e(e, "## getLocalisedContext() failed")
        }

        return context
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun setLocaleForApi24(config: Configuration, locale: Locale) {
        val set: MutableSet<Locale> = LinkedHashSet()
        // bring the user locale to the front of the list
        set.add(locale)
        val all = LocaleList.getDefault()
        for (i in 0 until all.size()) {
            // append other locales supported by the user
            set.add(all[i])
        }
        val locales = set.toTypedArray()
        config.setLocales(LocaleList(*locales))
    }

    /**
     * Compute the locale status value.
     * @return the local status value
     */
    fun getHash(): String {
        return (vectorLocale.applicationLocale.toString() +
                "_" + fontScalePreferences.getResolvedFontScaleValue().preferenceValue +
                "_" + ThemeUtils.getApplicationTheme(context))
    }
}
