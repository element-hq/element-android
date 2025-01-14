/*
 * Copyright 2019-2024 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings

import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.preference.VectorPreference
import im.vector.app.core.utils.toast
import im.vector.app.features.navigation.Navigator
import im.vector.app.features.notifications.NotificationDrawerManager
import im.vector.app.features.pin.PinCodeStore
import im.vector.app.features.pin.PinMode
import im.vector.app.features.pin.lockscreen.biometrics.BiometricHelper
import im.vector.app.features.pin.lockscreen.configuration.LockScreenConfiguration
import im.vector.app.features.pin.lockscreen.configuration.LockScreenMode
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.extensions.orFalse
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class VectorSettingsPinFragment :
        VectorSettingsBaseFragment() {

    @Inject lateinit var pinCodeStore: PinCodeStore
    @Inject lateinit var navigator: Navigator
    @Inject lateinit var notificationDrawerManager: NotificationDrawerManager
    @Inject lateinit var biometricHelperFactory: BiometricHelper.BiometricHelperFactory
    @Inject lateinit var defaultLockScreenConfiguration: LockScreenConfiguration

    override var titleRes = CommonStrings.settings_security_application_protection_screen_title
    override val preferenceXmlRes = R.xml.vector_settings_pin

    private val biometricHelper by lazy {
        biometricHelperFactory.create(defaultLockScreenConfiguration.copy(mode = LockScreenMode.CREATE))
    }

    private val usePinCodePref by lazy {
        findPreference<SwitchPreference>(VectorPreferences.SETTINGS_SECURITY_USE_PIN_CODE_FLAG)!!
    }

    private val changePinCodePref by lazy {
        findPreference<VectorPreference>(VectorPreferences.SETTINGS_SECURITY_CHANGE_PIN_CODE_FLAG)!!
    }

    private val useCompleteNotificationPref by lazy {
        findPreference<SwitchPreference>(VectorPreferences.SETTINGS_SECURITY_USE_COMPLETE_NOTIFICATIONS_FLAG)!!
    }

    private val useBiometricPref by lazy {
        findPreference<SwitchPreference>(VectorPreferences.SETTINGS_SECURITY_USE_BIOMETRICS_FLAG)!!
    }

    private fun updateBiometricPrefState(isPinCodeChecked: Boolean) {
        // Biometric auth depends on PIN auth
        useBiometricPref.isEnabled = isPinCodeChecked && biometricHelper.canUseAnySystemAuth
        useBiometricPref.isChecked = isPinCodeChecked && biometricHelper.isSystemAuthEnabledAndValid
    }

    override fun onResume() {
        super.onResume()
        updateBiometricPrefState(isPinCodeChecked = usePinCodePref.isChecked)
        viewLifecycleOwner.lifecycleScope.launch {
            refreshPinCodeStatus()
        }
    }

    override fun bindPref() {
        usePinCodePref.setOnPreferenceChangeListener { _, value ->
            val isChecked = (value as? Boolean).orFalse()
            updateBiometricPrefState(isPinCodeChecked = isChecked)
            if (!isChecked) {
                disableBiometricAuthentication()
            }
            true
        }

        useCompleteNotificationPref.setOnPreferenceChangeListener { _, _ ->
            // Refresh the drawer for an immediate effect of this change
            notificationDrawerManager.notificationStyleChanged()
            true
        }

        useBiometricPref.setOnPreferenceChangeListener { _, newValue ->
            if (newValue as? Boolean == true) {
                viewLifecycleOwner.lifecycleScope.launch {
                    runCatching {
                        // If previous system key existed, delete it
                        if (biometricHelper.hasSystemKey) {
                            biometricHelper.disableAuthentication()
                        }
                        biometricHelper.enableAuthentication(requireActivity()).collect()
                    }.onFailure {
                        showEnableBiometricErrorMessage()
                    }

                    updateBiometricPrefState(isPinCodeChecked = usePinCodePref.isChecked)
                }
                true
            } else {
                disableBiometricAuthentication()
                true
            }
        }
    }

    private fun disableBiometricAuthentication() {
        runCatching { biometricHelper.disableAuthentication() }
                .onFailure { Timber.e(it) }
    }

    private suspend fun refreshPinCodeStatus() {
        val hasPinCode = pinCodeStore.hasEncodedPin()
        usePinCodePref.isChecked = hasPinCode
        usePinCodePref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            if (hasPinCode) {
                lifecycleScope.launch {
                    pinCodeStore.deletePinCode()
                    refreshPinCodeStatus()
                }
            } else {
                navigator.openPinCode(
                        requireContext(),
                        pinActivityResultLauncher,
                        PinMode.CREATE
                )
            }
            true
        }

        changePinCodePref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            if (hasPinCode) {
                navigator.openPinCode(
                        requireContext(),
                        pinActivityResultLauncher,
                        PinMode.MODIFY
                )
            }
            true
        }
    }

    private fun showEnableBiometricErrorMessage() {
        context?.toast(CommonStrings.settings_security_pin_code_use_biometrics_error)
    }

    private val pinActivityResultLauncher = registerStartForActivityResult {
        // Nothing to do, refreshPinCodeStatus() will be called by `onResume`
    }
}
