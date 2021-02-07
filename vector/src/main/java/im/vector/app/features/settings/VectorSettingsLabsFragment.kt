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
import im.vector.app.R
import im.vector.app.core.extensions.restart
import im.vector.app.core.preference.VectorSwitchPreference
import im.vector.app.features.themes.ThemeUtils
import javax.inject.Inject

class VectorSettingsLabsFragment @Inject constructor(
        private val vectorPreferences: VectorPreferences
) : VectorSettingsBaseFragment() {

    override var titleRes = R.string.room_settings_labs_pref_title
    override val preferenceXmlRes = R.xml.vector_settings_labs

    override fun bindPref() {
        // Lab

        val systemDarkThemePreTenPref = findPreference<VectorSwitchPreference>(ThemeUtils.SYSTEM_DARK_THEME_PRE_TEN)
        systemDarkThemePreTenPref?.let {
            if (ThemeUtils.darkThemeDefinitivelyPossible()) {
                it.parent?.removePreference(it)
            }
        }
        /*
        systemDarkThemePreTenPref?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            if (newValue is Boolean) {
                if (ThemeUtils.shouldUseDarkTheme(requireContext())) {
                    // Restart the Activity | TODO: we need to do this AFTER the value is persisted...
                    activity?.restart()
                }
                true
            } else {
                false
            }
        }
         */

        findPreference<VectorSwitchPreference>(VectorPreferences.SETTINGS_ALLOW_URL_PREVIEW_IN_ENCRYPTED_ROOM_KEY)?.isEnabled = vectorPreferences.showUrlPreviews()
    }
}
