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

package im.vector.app.features.settings

import androidx.preference.Preference
import de.spiritcroc.preference.ColorMatrixListPreference
import im.vector.app.R
import im.vector.app.core.extensions.restart
import im.vector.app.features.themes.ThemeUtils
import javax.inject.Inject

class VectorSettingsAdvancedThemeFragment @Inject constructor(
        //private val vectorPreferences: VectorPreferences
) : VectorSettingsBaseFragment() {

    override var titleRes = R.string.settings_advanced_theme_settings
    override val preferenceXmlRes = R.xml.vector_settings_advanced_theme_settings

    override fun bindPref() {
        val lightAccentPref = findPreference<ColorMatrixListPreference>(ThemeUtils.SETTINGS_SC_ACCENT_LIGHT)!!
        val darkAccentPref = findPreference<ColorMatrixListPreference>(ThemeUtils.SETTINGS_SC_ACCENT_DARK)!!

        lightAccentPref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            if (newValue is String) {
                ThemeUtils.setApplicationLightThemeAccent(requireContext().applicationContext, newValue)
                if (ThemeUtils.isLightTheme(requireContext())) {
                    // Restart the Activity
                    activity?.restart()
                }
                true
            } else {
                false
            }
        }

        darkAccentPref.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            if (newValue is String) {
                ThemeUtils.setApplicationDarkThemeAccent(requireContext().applicationContext, newValue)
                if (!ThemeUtils.isLightTheme(requireContext())) {
                    // Restart the Activity
                    activity?.restart()
                }
                true
            } else {
                false
            }
        }
    }
}
