/*
 * Copyright 2020-2024 New Vector Ltd.
 * Copyright 2019 New Vector Ltd
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package im.vector.app.features.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withResumed
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreference
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.mvrx.fragmentViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.di.ActiveSessionHolder
import im.vector.app.core.dialogs.ExportKeysDialog
import im.vector.app.core.extensions.queryExportKeys
import im.vector.app.core.extensions.registerStartForActivityResult
import im.vector.app.core.intent.ExternalIntentData
import im.vector.app.core.intent.analyseIntent
import im.vector.app.core.intent.getFilenameFromUri
import im.vector.app.core.platform.SimpleTextWatcher
import im.vector.app.core.preference.VectorPreference
import im.vector.app.core.preference.VectorPreferenceCategory
import im.vector.app.core.preference.VectorSwitchPreference
import im.vector.app.core.resources.BuildMeta
import im.vector.app.core.utils.copyToClipboard
import im.vector.app.core.utils.openFileSelection
import im.vector.app.core.utils.toast
import im.vector.app.databinding.DialogImportE2eKeysBinding
import im.vector.app.features.analytics.AnalyticsConfig
import im.vector.app.features.analytics.plan.MobileScreen
import im.vector.app.features.analytics.ui.consent.AnalyticsConsentViewActions
import im.vector.app.features.analytics.ui.consent.AnalyticsConsentViewModel
import im.vector.app.features.analytics.ui.consent.AnalyticsConsentViewState
import im.vector.app.features.crypto.keys.KeysExporter
import im.vector.app.features.crypto.keys.KeysImporter
import im.vector.app.features.crypto.keysbackup.settings.KeysBackupManageActivity
import im.vector.app.features.crypto.recover.BootstrapBottomSheet
import im.vector.app.features.crypto.recover.SetupMode
import im.vector.app.features.navigation.Navigator
import im.vector.app.features.pin.PinCodeStore
import im.vector.app.features.pin.PinMode
import im.vector.app.features.raw.wellknown.getElementWellknown
import im.vector.app.features.raw.wellknown.isE2EByDefault
import im.vector.app.features.themes.ThemeUtils
import im.vector.lib.strings.CommonPlurals
import im.vector.lib.strings.CommonStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.gujun.android.span.span
import org.matrix.android.sdk.api.extensions.getFingerprintHumanReadable
import org.matrix.android.sdk.api.extensions.tryOrNull
import org.matrix.android.sdk.api.raw.RawService
import org.matrix.android.sdk.api.session.crypto.crosssigning.isVerified
import org.matrix.android.sdk.api.session.crypto.model.DeviceInfo
import javax.inject.Inject

@AndroidEntryPoint
class VectorSettingsSecurityPrivacyFragment :
        VectorSettingsBaseFragment() {

    @Inject lateinit var activeSessionHolder: ActiveSessionHolder
    @Inject lateinit var pinCodeStore: PinCodeStore
    @Inject lateinit var keysExporter: KeysExporter
    @Inject lateinit var keysImporter: KeysImporter
    @Inject lateinit var rawService: RawService
    @Inject lateinit var navigator: Navigator
    @Inject lateinit var analyticsConfig: AnalyticsConfig
    @Inject lateinit var vectorPreferences: VectorPreferences
    @Inject lateinit var buildMeta: BuildMeta

    override var titleRes = CommonStrings.settings_security_and_privacy
    override val preferenceXmlRes = R.xml.vector_settings_security_privacy

    private val analyticsConsentViewModel: AnalyticsConsentViewModel by fragmentViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        analyticsScreenName = MobileScreen.ScreenName.SettingsSecurity
    }

    // cryptography
    private val mCryptographyCategory by lazy {
        findPreference<PreferenceCategory>(VectorPreferences.SETTINGS_CRYPTOGRAPHY_PREFERENCE_KEY)!!
    }

    private val cryptoInfoDeviceNamePreference by lazy {
        findPreference<VectorPreference>("SETTINGS_ENCRYPTION_INFORMATION_DEVICE_NAME_PREFERENCE_KEY")!!
    }

    private val cryptoInfoDeviceIdPreference by lazy {
        findPreference<VectorPreference>("SETTINGS_ENCRYPTION_INFORMATION_DEVICE_ID_PREFERENCE_KEY")!!
    }

    private val cryptoInfoDeviceKeyPreference by lazy {
        findPreference<VectorPreference>("SETTINGS_ENCRYPTION_INFORMATION_DEVICE_KEY_PREFERENCE_KEY")!!
    }

    private val mCrossSigningStatePreference by lazy {
        findPreference<VectorPreference>(VectorPreferences.SETTINGS_ENCRYPTION_CROSS_SIGNING_PREFERENCE_KEY)!!
    }

    private val manageBackupPref by lazy {
        findPreference<VectorPreference>(VectorPreferences.SETTINGS_SECURE_MESSAGE_RECOVERY_PREFERENCE_KEY)!!
    }

    private val exportPref by lazy {
        findPreference<VectorPreference>(VectorPreferences.SETTINGS_ENCRYPTION_EXPORT_E2E_ROOM_KEYS_PREFERENCE_KEY)!!
    }

    private val importPref by lazy {
        findPreference<VectorPreference>(VectorPreferences.SETTINGS_ENCRYPTION_IMPORT_E2E_ROOM_KEYS_PREFERENCE_KEY)!!
    }

    private val showDeviceListPref by lazy {
        findPreference<VectorPreference>(VectorPreferences.SETTINGS_SHOW_DEVICES_LIST_PREFERENCE_KEY)!!
    }

    private val showDevicesListV2Pref by lazy {
        findPreference<VectorPreference>(VectorPreferences.SETTINGS_SHOW_DEVICES_LIST_V2_PREFERENCE_KEY)!!
    }

    // encrypt to unverified devices
    private val sendToUnverifiedDevicesPref by lazy {
        findPreference<SwitchPreference>(VectorPreferences.SETTINGS_ENCRYPTION_NEVER_SENT_TO_PREFERENCE_KEY)!!
    }

    private val openPinCodeSettingsPref by lazy {
        findPreference<VectorPreference>("SETTINGS_SECURITY_PIN")!!
    }

    private val analyticsCategory by lazy {
        findPreference<VectorPreferenceCategory>("SETTINGS_ANALYTICS_PREFERENCE_KEY")!!
    }

    private val analyticsConsent by lazy {
        findPreference<VectorSwitchPreference>("SETTINGS_USER_ANALYTICS_CONSENT_KEY")!!
    }

    private val incognitoKeyboardPref by lazy {
        findPreference<VectorSwitchPreference>(VectorPreferences.SETTINGS_SECURITY_INCOGNITO_KEYBOARD_PREFERENCE_KEY)!!
    }

    override fun onCreateRecyclerView(inflater: LayoutInflater, parent: ViewGroup, savedInstanceState: Bundle?): RecyclerView {
        return super.onCreateRecyclerView(inflater, parent, savedInstanceState).also {
            // Insert animation are really annoying the first time the list is shown
            // due to the way preference fragment is done, it's not trivial to disable it for first appearance only..
            // And it's not that an issue that this list is not animated, it's pretty static
            it.itemAnimator = null
        }
    }

    override fun onResume() {
        super.onResume()
        session.liveSecretSynchronisationInfo()
                .onEach {
                    refresh4SSection(it)
                    refreshXSigningStatus()
                }
                .launchIn(viewLifecycleOwner.lifecycleScope)

        viewLifecycleOwner.lifecycleScope.launch {
            findPreference<VectorPreference>(VectorPreferences.SETTINGS_CRYPTOGRAPHY_HS_ADMIN_DISABLED_E2E_DEFAULT)?.isVisible =
                    rawService
                            .getElementWellknown(session.sessionParams)
                            ?.isE2EByDefault() == false

            refreshXSigningStatus()
            // My device name may have been updated
            refreshMyDevice()
        }
    }

    private val secureBackupCategory by lazy {
        findPreference<VectorPreferenceCategory>("SETTINGS_CRYPTOGRAPHY_MANAGE_4S_CATEGORY_KEY")!!
    }
    private val secureBackupPreference by lazy {
        findPreference<VectorPreference>("SETTINGS_SECURE_BACKUP_RECOVERY_PREFERENCE_KEY")!!
    }

    private val ignoredUsersPreference by lazy {
        findPreference<VectorPreference>("SETTINGS_IGNORED_USERS_PREFERENCE_KEY")!!
    }
//    private val secureBackupResetPreference by lazy {
//        findPreference<VectorPreference>(VectorPreferences.SETTINGS_SECURE_BACKUP_RESET_PREFERENCE_KEY)
//    }

    private fun refresh4SSection(info: SecretsSynchronisationInfo) {
        // it's a lot of if / else if / else
        // But it's not yet clear how to manage all cases
        if (!info.isCrossSigningEnabled) {
            // There is not cross signing, so we can remove the section
            secureBackupCategory.isVisible = false
        } else {
            if (!info.isBackupSetup) {
                if (info.isCrossSigningEnabled && info.allPrivateKeysKnown) {
                    // You can setup recovery!
                    secureBackupCategory.isVisible = true
                    secureBackupPreference.title = getString(CommonStrings.settings_secure_backup_setup)
                    secureBackupPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                        BootstrapBottomSheet.show(parentFragmentManager, SetupMode.NORMAL)
                        true
                    }
                } else {
                    // just hide all, you can't setup from here
                    // you should synchronize to get gossips
                    secureBackupCategory.isVisible = false
                }
            } else {
                // so here we know that 4S is setup
                if (info.isCrossSigningTrusted && info.allPrivateKeysKnown) {
                    // Looks like we have all cross signing secrets and session is trusted
                    // Let's see if there is a megolm backup
                    if (!info.megolmBackupAvailable || info.megolmSecretKnown) {
                        // Only option here is to create a new backup if you want?
                        // aka reset
                        secureBackupCategory.isVisible = true
                        secureBackupPreference.title = getString(CommonStrings.settings_secure_backup_reset)
                        secureBackupPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                            BootstrapBottomSheet.show(parentFragmentManager, SetupMode.PASSPHRASE_RESET)
                            true
                        }
                    } else if (!info.megolmSecretKnown) {
                        // megolm backup is available but we don't have key
                        // you could try to synchronize to get missing megolm key ?
                        secureBackupCategory.isVisible = true
                        secureBackupPreference.title = getString(CommonStrings.settings_secure_backup_enter_to_setup)
                        secureBackupPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                            vectorActivity.let {
                                it.navigator.requestSelfSessionVerification(it)
                            }
                            true
                        }
                    } else {
                        secureBackupCategory.isVisible = false
                    }
                } else {
                    // there is a backup, but this session is not trusted, or is missing some secrets
                    // you should enter passphrase to get them or verify against another session
                    secureBackupCategory.isVisible = true
                    secureBackupPreference.title = getString(CommonStrings.settings_secure_backup_enter_to_setup)
                    secureBackupPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                        vectorActivity.let {
                            it.navigator.requestSelfSessionVerification(it)
                        }
                        true
                    }
                }
            }
        }
    }

    override fun bindPref() {
        // Refresh Key Management section
        refreshKeysManagementSection()

        // Analytics
        setUpAnalytics()

        // Incognito Keyboard
        setUpIncognitoKeyboard()

        // Pin code
        openPinCodeSettingsPref.setOnPreferenceClickListener {
            openPinCodePreferenceScreen()
            true
        }

        secureBackupPreference.icon = activity?.let {
            ThemeUtils.tintDrawable(
                    it,
                    ContextCompat.getDrawable(it, R.drawable.ic_secure_backup)!!, im.vector.lib.ui.styles.R.attr.vctr_content_primary
            )
        }

        ignoredUsersPreference.icon = activity?.let {
            ThemeUtils.tintDrawable(
                    it,
                    ContextCompat.getDrawable(it, R.drawable.ic_settings_root_ignored_users)!!, im.vector.lib.ui.styles.R.attr.vctr_content_primary
            )
        }

        findPreference<VectorPreference>(VectorPreferences.SETTINGS_CRYPTOGRAPHY_HS_ADMIN_DISABLED_E2E_DEFAULT)?.let {
            it.icon = ThemeUtils.tintDrawableWithColor(
                    ContextCompat.getDrawable(requireContext(), R.drawable.ic_notification_privacy_warning)!!,
                    ThemeUtils.getColor(requireContext(), com.google.android.material.R.attr.colorError)
            )
            it.summary = span {
                text = getString(CommonStrings.settings_hs_admin_e2e_disabled)
                textColor = ThemeUtils.getColor(requireContext(), com.google.android.material.R.attr.colorError)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeAnalyticsState()
    }

    private fun observeAnalyticsState() {
        analyticsConsentViewModel.onEach(AnalyticsConsentViewState::userConsent) {
            analyticsConsent.isChecked = it
        }
    }

    private fun setUpAnalytics() {
        analyticsCategory.isVisible = analyticsConfig.isEnabled

        analyticsConsent.setOnPreferenceChangeListener { _, newValue ->
            val newValueBool = newValue as? Boolean ?: false
            if (newValueBool) {
                // User wants to enable analytics, display the opt in screen
                navigator.openAnalyticsOptIn(requireContext())
            } else {
                // Just disable analytics
                analyticsConsentViewModel.handle(AnalyticsConsentViewActions.SetUserConsent(false))
            }
            true
        }
    }

    private fun setUpIncognitoKeyboard() {
        incognitoKeyboardPref.isVisible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    }

    // Todo this should be refactored and use same state as 4S section
    private suspend fun refreshXSigningStatus() {
        val crossSigningKeys = session.cryptoService().crossSigningService().getMyCrossSigningKeys()
        val xSigningIsEnableInAccount = crossSigningKeys != null
        val xSigningKeysAreTrusted = session.cryptoService().crossSigningService().checkUserTrust(session.myUserId).isVerified()
        val xSigningKeyCanSign = session.cryptoService().crossSigningService().canCrossSign()

        withContext(Dispatchers.Main) {
            when {
                xSigningKeyCanSign -> {
                    mCrossSigningStatePreference.setIcon(R.drawable.ic_shield_trusted)
                    mCrossSigningStatePreference.summary = getString(CommonStrings.encryption_information_dg_xsigning_complete)
                }
                xSigningKeysAreTrusted -> {
                    mCrossSigningStatePreference.setIcon(R.drawable.ic_shield_custom)
                    mCrossSigningStatePreference.summary = getString(CommonStrings.encryption_information_dg_xsigning_trusted)
                }
                xSigningIsEnableInAccount -> {
                    mCrossSigningStatePreference.setIcon(R.drawable.ic_shield_black)
                    mCrossSigningStatePreference.summary = getString(CommonStrings.encryption_information_dg_xsigning_not_trusted)
                }
                else -> {
                    mCrossSigningStatePreference.setIcon(android.R.color.transparent)
                    mCrossSigningStatePreference.summary = getString(CommonStrings.encryption_information_dg_xsigning_disabled)
                }
            }
            mCrossSigningStatePreference.isVisible = true
        }
    }

    private val saveMegolmStartForActivityResult = registerStartForActivityResult {
        val uri = it.data?.data ?: return@registerStartForActivityResult
        if (it.resultCode == Activity.RESULT_OK) {
            ExportKeysDialog().show(requireActivity(), object : ExportKeysDialog.ExportKeyDialogListener {
                override fun onPassphrase(passphrase: String) {
                    displayLoadingView()

                    export(passphrase, uri)
                }
            })
        }
    }

    private fun export(passphrase: String, uri: Uri) {
        lifecycleScope.launch {
            try {
                keysExporter.export(passphrase, uri)
                requireActivity().toast(getString(CommonStrings.encryption_exported_successfully))
            } catch (failure: Throwable) {
                requireActivity().toast(errorFormatter.toHumanReadable(failure))
            }
            hideLoadingView()
        }
    }

    private val pinActivityResultLauncher = registerStartForActivityResult {
        if (it.resultCode == Activity.RESULT_OK) {
            doOpenPinCodePreferenceScreen()
        }
    }

    private val importKeysActivityResultLauncher = registerStartForActivityResult {
        val data = it.data ?: return@registerStartForActivityResult
        if (it.resultCode == Activity.RESULT_OK) {
            importKeys(data)
        }
    }

    private fun openPinCodePreferenceScreen() {
        viewLifecycleOwner.lifecycleScope.launch {
            withResumed {
                viewLifecycleOwner.lifecycleScope.launch {
                    val hasPinCode = pinCodeStore.hasEncodedPin()
                    if (hasPinCode) {
                        navigator.openPinCode(
                                requireContext(),
                                pinActivityResultLauncher,
                                PinMode.AUTH
                        )
                    } else {
                        doOpenPinCodePreferenceScreen()
                    }
                }
            }
        }
    }

    private fun doOpenPinCodePreferenceScreen() {
        (vectorActivity as? VectorSettingsActivity)?.navigateTo(VectorSettingsPinFragment::class.java)
    }

    private fun refreshKeysManagementSection() {
        // If crypto is not enabled parent section will be removed
        // TODO notice that this will not work when no network
        manageBackupPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            context?.let {
                startActivity(KeysBackupManageActivity.intent(it))
            }
            false
        }

        exportPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            queryExportKeys(
                    userId = activeSessionHolder.getSafeActiveSession()?.myUserId ?: "",
                    applicationName = buildMeta.applicationName,
                    activityResultLauncher = saveMegolmStartForActivityResult,
            )
            true
        }

        importPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            importKeys()
            true
        }
    }

    /**
     * Manage the e2e keys import.
     */
    private fun importKeys() {
        openFileSelection(
                requireActivity(),
                importKeysActivityResultLauncher,
                false,
                0
        )
    }

    /**
     * Manage the e2e keys import.
     *
     * @param intent the intent result
     */
    private fun importKeys(intent: Intent) {
        val sharedDataItems = analyseIntent(intent)
        val thisActivity = activity

        if (sharedDataItems.isNotEmpty() && thisActivity != null) {
            val sharedDataItem = sharedDataItems[0]

            val uri = when (sharedDataItem) {
                is ExternalIntentData.IntentDataUri -> sharedDataItem.uri
                is ExternalIntentData.IntentDataClipData -> sharedDataItem.clipDataItem.uri
                else -> null
            }

            val mimetype = when (sharedDataItem) {
                is ExternalIntentData.IntentDataClipData -> sharedDataItem.mimeType
                else -> null
            }

            if (uri == null) {
                return
            }

            val appContext = thisActivity.applicationContext

            val filename = getFilenameFromUri(appContext, uri)

            val dialogLayout = thisActivity.layoutInflater.inflate(R.layout.dialog_import_e2e_keys, null)
            val views = DialogImportE2eKeysBinding.bind(dialogLayout)

            if (filename.isNullOrBlank()) {
                views.dialogE2eKeysPassphraseFilename.isVisible = false
            } else {
                views.dialogE2eKeysPassphraseFilename.isVisible = true
                views.dialogE2eKeysPassphraseFilename.text = getString(CommonStrings.import_e2e_keys_from_file, filename)
            }

            val builder = MaterialAlertDialogBuilder(thisActivity)
                    .setTitle(CommonStrings.encryption_import_room_keys)
                    .setView(dialogLayout)

            views.dialogE2eKeysPassphraseEditText.addTextChangedListener(object : SimpleTextWatcher() {
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    views.dialogE2eKeysImportButton.isEnabled = !views.dialogE2eKeysPassphraseEditText.text.isNullOrEmpty()
                }
            })

            val importDialog = builder.show()

            views.dialogE2eKeysImportButton.debouncedClicks {
                val password = views.dialogE2eKeysPassphraseEditText.text.toString()

                displayLoadingView()

                lifecycleScope.launch {
                    val data = try {
                        keysImporter.import(uri, mimetype, password)
                    } catch (failure: Throwable) {
                        appContext.toast(errorFormatter.toHumanReadable(failure))
                        null
                    }
                    hideLoadingView()

                    if (data != null) {
                        MaterialAlertDialogBuilder(thisActivity)
                                .setMessage(
                                        resources.getQuantityString(
                                                CommonPlurals.encryption_import_room_keys_success,
                                                data.successfullyNumberOfImportedKeys,
                                                data.successfullyNumberOfImportedKeys,
                                                data.totalNumberOfKeys
                                        )
                                )
                                .setPositiveButton(CommonStrings.ok) { dialog, _ -> dialog.dismiss() }
                                .show()
                    }
                }
                importDialog.dismiss()
            }
        }
    }

    // ==============================================================================================================
    // Cryptography
    // ==============================================================================================================

    /**
     * Build the cryptography preference section.
     */
    private suspend fun refreshCryptographyPreference(devices: List<DeviceInfo>) {
        showDeviceListPref.isVisible = !vectorPreferences.isNewSessionManagerEnabled()
        showDeviceListPref.isEnabled = devices.isNotEmpty()
        showDeviceListPref.summary = resources.getQuantityString(CommonPlurals.settings_active_sessions_count, devices.size, devices.size)

        showDevicesListV2Pref.isVisible = vectorPreferences.isNewSessionManagerEnabled()
        showDevicesListV2Pref.isEnabled = devices.isNotEmpty()
        showDevicesListV2Pref.summary = resources.getQuantityString(CommonPlurals.settings_active_sessions_count, devices.size, devices.size)

        val userId = session.myUserId
        val deviceId = session.sessionParams.deviceId

        val aMyDeviceInfo = devices.find { it.deviceId == deviceId }

        // crypto section: device name
        if (aMyDeviceInfo != null) {
            cryptoInfoDeviceNamePreference.summary = aMyDeviceInfo.displayName

            cryptoInfoDeviceNamePreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                copyToClipboard(requireActivity(), aMyDeviceInfo.displayName ?: "")
                true
            }
        }

        // crypto section: device ID
        if (!deviceId.isNullOrEmpty()) {
            cryptoInfoDeviceIdPreference.summary = deviceId

            cryptoInfoDeviceIdPreference.setOnPreferenceClickListener {
                copyToClipboard(requireActivity(), deviceId)
                true
            }
        }

        // crypto section: device key (fingerprint)
        val deviceInfo = session.cryptoService().getCryptoDeviceInfo(userId, deviceId)

        val fingerprint = deviceInfo?.fingerprint()
        if (fingerprint?.isNotEmpty() == true) {
            cryptoInfoDeviceKeyPreference.summary = deviceInfo.getFingerprintHumanReadable()

            cryptoInfoDeviceKeyPreference.setOnPreferenceClickListener {
                copyToClipboard(requireActivity(), fingerprint)
                true
            }
        }

        sendToUnverifiedDevicesPref.isChecked = session.cryptoService().getGlobalBlacklistUnverifiedDevices()

        sendToUnverifiedDevicesPref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            session.cryptoService().setGlobalBlacklistUnverifiedDevices(sendToUnverifiedDevicesPref.isChecked)

            true
        }
    }

    // ==============================================================================================================
    // devices list
    // ==============================================================================================================

    private suspend fun refreshMyDevice() {
        session.cryptoService().getUserDevices(session.myUserId).map {
            DeviceInfo(
                    userId = session.myUserId,
                    deviceId = it.deviceId,
                    displayName = it.displayName()
            )
        }.let {
            withContext(Dispatchers.Main) {
                refreshCryptographyPreference(it)
            }
        }
        // TODO Move to a ViewModel...
        val devicesList = tryOrNull { session.cryptoService().fetchDevicesList() }
        devicesList?.let {
            withContext(Dispatchers.Main) {
                refreshCryptographyPreference(it)
            }
        }
    }
}
