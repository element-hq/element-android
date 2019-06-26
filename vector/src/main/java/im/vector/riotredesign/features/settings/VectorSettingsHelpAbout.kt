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

package im.vector.riotredesign.features.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.preference.Preference
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import im.vector.matrix.android.api.Matrix
import im.vector.riotredesign.R
import im.vector.riotredesign.core.utils.copyToClipboard
import im.vector.riotredesign.core.utils.displayInWebView
import im.vector.riotredesign.features.version.getVersion

class VectorSettingsHelpAbout : VectorSettingsBaseFragment() {

    override var titleRes = R.string.preference_root_help_about
    override val preferenceXmlRes = R.xml.vector_settings_help_about

    override fun bindPref() {
        // preference to start the App info screen, to facilitate App permissions access
        findPreference(APP_INFO_LINK_PREFERENCE_KEY)
                .onPreferenceClickListener = Preference.OnPreferenceClickListener {

            activity?.let {
                val intent = Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                    val uri = Uri.fromParts("package", requireContext().packageName, null)

                    data = uri
                }
                it.applicationContext.startActivity(intent)
            }

            true
        }

        // application version
        (findPreference(PreferencesManager.SETTINGS_VERSION_PREFERENCE_KEY)).let {
            it.summary = getVersion(longFormat = false, useBuildNumber = true)

            it.setOnPreferenceClickListener { pref ->
                copyToClipboard(requireContext(), pref.summary)
                true
            }
        }

        // SDK version
        (findPreference(PreferencesManager.SETTINGS_SDK_VERSION_PREFERENCE_KEY)).let {
            it.summary = Matrix.getSdkVersion()

            it.setOnPreferenceClickListener { pref ->
                copyToClipboard(requireContext(), pref.summary)
                true
            }
        }

        // olm version
        findPreference(PreferencesManager.SETTINGS_OLM_VERSION_PREFERENCE_KEY)
                .summary = mSession.getCryptoVersion(requireContext(), false)

        // copyright
        findPreference(PreferencesManager.SETTINGS_COPYRIGHT_PREFERENCE_KEY)
                .onPreferenceClickListener = Preference.OnPreferenceClickListener {
            activity?.displayInWebView(VectorSettingsUrls.COPYRIGHT)
            false
        }

        // terms & conditions
        findPreference(PreferencesManager.SETTINGS_APP_TERM_CONDITIONS_PREFERENCE_KEY)
                .onPreferenceClickListener = Preference.OnPreferenceClickListener {
            activity?.displayInWebView(VectorSettingsUrls.TAC)
            false
        }

        // privacy policy
        findPreference(PreferencesManager.SETTINGS_PRIVACY_POLICY_PREFERENCE_KEY)
                .onPreferenceClickListener = Preference.OnPreferenceClickListener {
            activity?.displayInWebView(VectorSettingsUrls.PRIVACY_POLICY)
            false
        }

        // third party notice
        findPreference(PreferencesManager.SETTINGS_THIRD_PARTY_NOTICES_PREFERENCE_KEY)
                .onPreferenceClickListener = Preference.OnPreferenceClickListener {
            activity?.displayInWebView(VectorSettingsUrls.THIRD_PARTY_LICENSES)
            false
        }

        findPreference(PreferencesManager.SETTINGS_OTHER_THIRD_PARTY_NOTICES_PREFERENCE_KEY)
                .onPreferenceClickListener = Preference.OnPreferenceClickListener {
            // See https://developers.google.com/android/guides/opensource
            startActivity(Intent(requireActivity(), OssLicensesMenuActivity::class.java))
            false
        }
    }

    companion object {
        private const val APP_INFO_LINK_PREFERENCE_KEY = "APP_INFO_LINK_PREFERENCE_KEY"
    }
}