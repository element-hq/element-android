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

package im.vector.riotx.features.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.preference.Preference
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import im.vector.matrix.android.api.Matrix
import im.vector.riotx.R
import im.vector.riotx.core.di.ScreenComponent
import im.vector.riotx.core.preference.VectorPreference
import im.vector.riotx.core.utils.copyToClipboard
import im.vector.riotx.core.utils.displayInWebView
import im.vector.riotx.features.version.VersionProvider
import javax.inject.Inject

class VectorSettingsHelpAboutFragment : VectorSettingsBaseFragment() {

    override var titleRes = R.string.preference_root_help_about
    override val preferenceXmlRes = R.xml.vector_settings_help_about

    @Inject lateinit var versionProvider: VersionProvider

    override fun injectWith(injector: ScreenComponent) {
        injector.inject(this)
    }

    override fun bindPref() {
        // preference to start the App info screen, to facilitate App permissions access
        findPreference<VectorPreference>(APP_INFO_LINK_PREFERENCE_KEY)!!
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
        findPreference<VectorPreference>(VectorPreferences.SETTINGS_VERSION_PREFERENCE_KEY)!!.let {
            it.summary = versionProvider.getVersion(longFormat = false, useBuildNumber = true)

            it.setOnPreferenceClickListener { pref ->
                copyToClipboard(requireContext(), pref.summary)
                true
            }
        }

        // SDK version
        findPreference<VectorPreference>(VectorPreferences.SETTINGS_SDK_VERSION_PREFERENCE_KEY)!!.let {
            it.summary = Matrix.getSdkVersion()

            it.setOnPreferenceClickListener { pref ->
                copyToClipboard(requireContext(), pref.summary)
                true
            }
        }

        // olm version
        findPreference<VectorPreference>(VectorPreferences.SETTINGS_OLM_VERSION_PREFERENCE_KEY)!!
                .summary = session.getCryptoVersion(requireContext(), false)

        // copyright
        findPreference<VectorPreference>(VectorPreferences.SETTINGS_COPYRIGHT_PREFERENCE_KEY)!!
                .onPreferenceClickListener = Preference.OnPreferenceClickListener {
            activity?.displayInWebView(VectorSettingsUrls.COPYRIGHT)
            false
        }

        // terms & conditions
        findPreference<VectorPreference>(VectorPreferences.SETTINGS_APP_TERM_CONDITIONS_PREFERENCE_KEY)!!
                .onPreferenceClickListener = Preference.OnPreferenceClickListener {
            activity?.displayInWebView(VectorSettingsUrls.TAC)
            false
        }

        // privacy policy
        findPreference<VectorPreference>(VectorPreferences.SETTINGS_PRIVACY_POLICY_PREFERENCE_KEY)!!
                .onPreferenceClickListener = Preference.OnPreferenceClickListener {
            activity?.displayInWebView(VectorSettingsUrls.PRIVACY_POLICY)
            false
        }

        // third party notice
        findPreference<VectorPreference>(VectorPreferences.SETTINGS_THIRD_PARTY_NOTICES_PREFERENCE_KEY)!!
                .onPreferenceClickListener = Preference.OnPreferenceClickListener {
            activity?.displayInWebView(VectorSettingsUrls.THIRD_PARTY_LICENSES)
            false
        }

        findPreference<VectorPreference>(VectorPreferences.SETTINGS_OTHER_THIRD_PARTY_NOTICES_PREFERENCE_KEY)!!
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