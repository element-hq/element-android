/*
 * Copyright 2022-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings

import android.content.SharedPreferences
import im.vector.app.core.di.DefaultPreferences
import java.util.Locale
import javax.inject.Inject

/**
 * Class to provide the Locale choice of the user.
 */
class VectorLocaleProvider @Inject constructor(
        @DefaultPreferences
        private val preferences: SharedPreferences,
) {
    /**
     * Get the current local.
     * SharedPref values has been initialized in [VectorLocale.init]
     */
    val applicationLocale: Locale
        get() = Locale(
                preferences.getString(VectorLocale.APPLICATION_LOCALE_LANGUAGE_KEY, "")!!,
                preferences.getString(VectorLocale.APPLICATION_LOCALE_COUNTRY_KEY, "")!!,
                preferences.getString(VectorLocale.APPLICATION_LOCALE_VARIANT_KEY, "")!!
        )
}
