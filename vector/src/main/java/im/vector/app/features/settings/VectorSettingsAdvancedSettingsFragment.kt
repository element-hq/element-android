/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.SeekBarPreference
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.platform.VectorBaseActivity
import im.vector.app.core.preference.VectorPreference
import im.vector.app.core.preference.VectorPreferenceCategory
import im.vector.app.core.preference.VectorSwitchPreference
import im.vector.app.core.utils.copyToClipboard
import im.vector.app.features.analytics.plan.MobileScreen
import im.vector.app.features.home.NightlyProxy
import im.vector.app.features.rageshake.RageShake
import im.vector.lib.strings.CommonStrings
import javax.inject.Inject

@AndroidEntryPoint
class VectorSettingsAdvancedSettingsFragment :
        VectorSettingsBaseFragment() {

    override var titleRes = CommonStrings.settings_advanced_settings
    override val preferenceXmlRes = R.xml.vector_settings_advanced_settings

    @Inject lateinit var nightlyProxy: NightlyProxy

    private var rageshake: RageShake? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        analyticsScreenName = MobileScreen.ScreenName.SettingsAdvanced
    }

    override fun onResume() {
        super.onResume()

        rageshake = (activity as? VectorBaseActivity<*>)?.rageShake
        rageshake?.interceptor = {
            (activity as? VectorBaseActivity<*>)?.showSnackbar(getString(CommonStrings.rageshake_detected))
        }
    }

    override fun onPause() {
        super.onPause()
        rageshake?.interceptor = null
        rageshake = null
    }

    override fun bindPref() {
        setupRageShakeSection()
        setupNightlySection()
        setupDevToolsSection()
    }

    private fun setupDevToolsSection() {
        findPreference<VectorPreference>("SETTINGS_ACCESS_TOKEN")?.setOnPreferenceClickListener {
            copyToClipboard(requireActivity(), session.sessionParams.credentials.accessToken)
            true
        }

        findPreference<VectorPreference>(VectorPreferences.SETTINGS_DEVELOPER_MODE_KEY_REQUEST_AUDIT_KEY)?.apply {
            isVisible = session.cryptoService().supportKeyRequestInspection()
        }
    }

    private fun setupRageShakeSection() {
        val isRageShakeAvailable = RageShake.isAvailable(requireContext())

        if (isRageShakeAvailable) {
            findPreference<VectorSwitchPreference>(VectorPreferences.SETTINGS_USE_RAGE_SHAKE_KEY)!!
                    .onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->

                if (newValue as? Boolean == true) {
                    rageshake?.start()
                } else {
                    rageshake?.stop()
                }

                true
            }

            findPreference<SeekBarPreference>(VectorPreferences.SETTINGS_RAGE_SHAKE_DETECTION_THRESHOLD_KEY)!!
                    .onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                (activity as? VectorBaseActivity<*>)?.let {
                    val newValueAsInt = newValue as? Int ?: return@OnPreferenceChangeListener true

                    rageshake?.setSensitivity(newValueAsInt)
                }

                true
            }
        } else {
            findPreference<VectorPreferenceCategory>("SETTINGS_RAGE_SHAKE_CATEGORY_KEY")!!.isVisible = false
        }
    }

    private fun setupNightlySection() {
        findPreference<VectorPreferenceCategory>("SETTINGS_NIGHTLY_BUILD_PREFERENCE_KEY")?.isVisible = nightlyProxy.isNightlyBuild()
        findPreference<VectorPreference>("SETTINGS_NIGHTLY_BUILD_UPDATE_PREFERENCE_KEY")?.setOnPreferenceClickListener {
            nightlyProxy.updateApplication()
            true
        }
    }
}
