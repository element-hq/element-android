/*
 * Copyright (c) 2022 New Vector Ltd
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
